package com.ascent.climb.data.cloud

import android.content.Context
import com.ascent.climb.R
import com.ascent.climb.data.Account
import com.ascent.climb.data.AuthApi
import com.ascent.climb.data.AuthResult
import org.json.JSONObject

/**
 * 真实登录实现：走 CLOUD-PLAN.md 里约定的 `/auth/…` 那组接口。
 *
 * 与 [com.ascent.climb.data.MockAuthApi] 的关系：那个保留给没配后端的开发环境
 * （[CloudConfig.BASE_URL] 为空时界面用它），两者实现同一个 `AuthApi`，
 * 界面不需要知道自己接的是哪一个。
 *
 * 账号信息在本地留一份镜像（邮箱、昵称、id），只是为了**离线时也能显示已登录**；
 * 权威数据在服务端，每次 `me` / 登录成功都会覆盖镜像。
 */
class CloudAuthApi(private val context: Context) : AuthApi {

    private val prefs = context.getSharedPreferences("ascent_account", Context.MODE_PRIVATE)
    private val http = CloudHttp(CloudConfig.BASE_URL)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NICK = "nickname"
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val PASSWORD_MIN = 8
        const val NICKNAME_MAX = 16
    }

    override fun currentAccount(): Account? {
        // 没有 token 就是没登录：镜像可能是上次退出后残留的
        if (TokenStore.load(context) == null) return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return Account(
            id = prefs.getString(KEY_ID, "cloud") ?: "cloud",
            email = email,
            nickname = prefs.getString(KEY_NICK, email.substringBefore("@")) ?: email
        )
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val e = email.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        return runCatching {
            http.call("POST", "/auth/login", body = JSONObject().put("email", e).put("password", password))
        }.fold(
            onSuccess = { if (it == null) AuthResult.Failure(msg(R.string.cloud_err_server)) else save(it) },
            onFailure = { AuthResult.Failure(message(it)) }
        )
    }

    override suspend fun register(email: String, password: String, nickname: String): AuthResult {
        val e = email.trim()
        val n = nickname.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        if (n.isEmpty() || n.length > NICKNAME_MAX) return AuthResult.Failure(msg(R.string.cloud_err_nickname))
        return runCatching {
            http.call(
                "POST", "/auth/register",
                body = JSONObject().put("email", e).put("password", password).put("nickname", n)
            )
        }.fold(
            onSuccess = { if (it == null) AuthResult.Failure(msg(R.string.cloud_err_server)) else save(it) },
            onFailure = { AuthResult.Failure(message(it)) }
        )
    }

    override suspend fun logout() {
        val token = TokenStore.load(context)
        if (token != null) {
            // 服务端撤销失败也不该把用户卡在登录态：本地照样退出，token 到期自然失效
            runCatching { http.call("POST", "/auth/logout", token) }
        }
        TokenStore.clear(context)
        prefs.edit().clear().apply()
    }

    /**
     * 删除账号。服务端会级联删掉进度、头像与所有 token（CLOUD-PLAN.md §7），
     * 所以这里**必须先成功再清本地**，否则会出现「本地清空、云端还在」的半删除状态。
     */
    override suspend fun deleteAccount(): Boolean {
        val token = TokenStore.load(context) ?: return true
        val ok = runCatching { http.call("DELETE", "/auth/account", token) }.isSuccess
        if (ok) {
            TokenStore.clear(context)
            prefs.edit().clear().apply()
        }
        return ok
    }

    /** 改昵称。成功返回新的账号镜像；失败返回 null（界面保留原值让用户重试）。 */
    suspend fun updateNickname(nickname: String): Account? {
        val token = TokenStore.load(context) ?: return null
        val n = nickname.trim()
        if (n.isEmpty() || n.length > NICKNAME_MAX) return null
        val user = runCatching {
            http.call("PATCH", "/auth/profile", token, JSONObject().put("nickname", n))?.optJSONObject("user")
        }.getOrNull() ?: return null
        return mirror(user)
    }

    /** 把服务端返回的 user 落到本地镜像，并转成界面用的 Account。 */
    private fun mirror(user: JSONObject): Account {
        val account = Account(
            id = user.optString("id", "cloud"),
            email = user.optString("email", ""),
            nickname = user.optString("nickname", "")
        )
        prefs.edit()
            .putString(KEY_ID, account.id)
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_NICK, account.nickname)
            .apply()
        return account
    }

    private fun save(json: JSONObject): AuthResult {
        val token = json.optString("token", "")
        val user = json.optJSONObject("user")
        if (token.isBlank() || user == null) return AuthResult.Failure(msg(R.string.cloud_err_server))
        TokenStore.save(context, token)
        return AuthResult.Success(mirror(user))
    }

    private fun validate(email: String, password: String): String? = when {
        email.isEmpty() -> msg(R.string.cloud_err_email_format)
        !EMAIL_REGEX.matches(email) -> msg(R.string.cloud_err_email_format)
        password.length < PASSWORD_MIN -> msg(R.string.cloud_err_password_short)
        else -> null
    }

    private fun message(t: Throwable): String {
        val e = t as? CloudException ?: return msg(R.string.cloud_err_server)
        return when {
            e.isNetwork -> msg(R.string.cloud_err_network)
            e.code == "invalid_credentials" || e.code == "unauthorized" -> msg(R.string.cloud_err_credentials)
            e.code == "email_taken" -> msg(R.string.cloud_err_email_taken)
            e.code == "too_many_attempts" -> msg(R.string.cloud_err_too_many)
            e.code == "weak_password" -> msg(R.string.cloud_err_password_short)
            e.code == "invalid_email" -> msg(R.string.cloud_err_email_format)
            e.code == "invalid_nickname" -> msg(R.string.cloud_err_nickname)
            else -> msg(R.string.cloud_err_server)
        }
    }

    private fun msg(id: Int): String = context.getString(id)
}
