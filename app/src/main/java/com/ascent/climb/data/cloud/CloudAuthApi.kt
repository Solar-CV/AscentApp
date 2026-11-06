package com.ascent.climb.data.cloud

import android.content.Context
import com.ascent.climb.BuildConfig
import com.ascent.climb.R
import com.ascent.climb.data.Account
import com.ascent.climb.data.AuthApi
import com.ascent.climb.data.AuthResult
import com.ascent.climb.data.CodePurpose
import com.ascent.climb.data.ProgressStore
import org.json.JSONObject

/**
 * 真实登录实现：走 CLOUD-PLAN.md 里约定的 `/auth/…` 那组接口。
 *
 * 与 [com.ascent.climb.data.MockAuthApi] 的关系：那个保留给没配后端的开发环境
 * （[CloudConfig.BASE_URL] 为空时界面用它），两者实现同一个 `AuthApi`，
 * 界面不需要知道自己接的是哪一个。
 *
 * 账号信息在本地留一份镜像（邮箱、昵称、id），只是为了**离线时也能显示已登录**；
 * 权威数据在服务端，每次登录成功或拉取 `me` 都会覆盖镜像。
 *
 * 会话是 access + refresh 两枚（见 [TokenStore]），过期由 [CloudApi] 自动续，
 * 所以界面只会遇到「已登录 / 未登录」两种状态，不会因为一小时没打开就掉线。
 */
class CloudAuthApi(private val context: Context) : AuthApi {

    private val prefs = context.getSharedPreferences("ascent_account", Context.MODE_PRIVATE)
    private val http = CloudHttp(CloudConfig.BASE_URL)
    private val api = CloudApi(context)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NICK = "nickname"
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val PASSWORD_MIN = 8
        const val NICKNAME_MAX = 16
    }

    override fun currentAccount(): Account? {
        // 只要 refresh 还在就算登录着：access 过期是常态，不该让界面闪一下「未登录」
        if (!TokenStore.hasSession(context)) return null
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
        return submit(
            "/auth/login",
            JSONObject().put("email", e).put("password", password).put("device", device())
        )
    }

    /**
     * Google 登录。服务端验票（aud / iss / exp / sub）后建号或直接登录，
     * 昵称与头像由 Google 资料带入，所以这里不传 nickname。
     */
    override suspend fun loginWithGoogle(idToken: String): AuthResult {
        if (idToken.isBlank()) return AuthResult.Failure(msg(R.string.cloud_err_google_failed))
        return submit(
            "/auth/google",
            JSONObject().put("idToken", idToken).put("device", device())
        )
    }

    override suspend fun register(email: String, password: String, nickname: String, code: String): AuthResult {
        val e = email.trim()
        val n = nickname.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        if (n.isEmpty() || n.length > NICKNAME_MAX) return AuthResult.Failure(msg(R.string.cloud_err_nickname))
        val body = JSONObject().put("email", e).put("password", password).put("nickname", n).put("device", device())
        // 服务端 REQUIRE_EMAIL_CODE=false 时不需要验证码，空串直接不发这个字段
        if (code.isNotBlank()) body.put("code", code.trim())
        return submit("/auth/register", body)
    }

    /**
     * 发验证码。返回 null 表示已发出。
     *
     * 客户端**不判断邮箱是否已注册**：服务端对两种情况都回 200，
     * 这样这个接口就不会变成「查某个邮箱有没有账号」的工具。
     */
    override suspend fun sendCode(email: String, purpose: CodePurpose): String? {
        val e = email.trim()
        if (e.isEmpty() || !EMAIL_REGEX.matches(e)) return msg(R.string.cloud_err_email_format)
        // 走 http 而不是 api：这两个接口是**公开的**，用户此刻还没登录。
        // 用 CloudApi 会先找 access token，找不到就抛 unauthorized，
        // 界面会显示成「登录已过期」—— 明明一次都没登录过。
        return runCatching {
            http.call("POST", "/auth/email/code", body = JSONObject().put("email", e).put("purpose", purpose.wire))
        }.fold(onSuccess = { null }, onFailure = { message(it) })
    }

    /** 重置密码。成功返回 null；服务端会把该账号所有会话作废，所以之后必须重新登录。 */
    override suspend fun resetPassword(email: String, code: String, newPassword: String): String? {
        val e = email.trim()
        if (e.isEmpty() || !EMAIL_REGEX.matches(e)) return msg(R.string.cloud_err_email_format)
        if (newPassword.length < PASSWORD_MIN) return msg(R.string.cloud_err_password_short)
        return runCatching {
            http.call(
                "POST",
                "/auth/password/reset",
                body = JSONObject().put("email", e).put("code", code.trim()).put("newPassword", newPassword)
            )
        }.fold(onSuccess = { null }, onFailure = { message(it) })
    }

    override suspend fun logout() {
        // 服务端撤销失败也不该把用户卡在登录态：本地照样退出，refresh 到期自然失效
        if (TokenStore.hasSession(context)) {
            runCatching { api.json("POST", "/auth/logout") }
        }
        clearLocal()
    }

    /**
     * 删除账号。服务端会级联删掉进度、头像、身份与所有会话（CLOUD-PLAN.md §7），
     * 所以这里**必须先成功再清本地**，否则会出现「本地清空、云端还在」的半删除状态。
     */
    override suspend fun deleteAccount(): Boolean {
        if (!TokenStore.hasSession(context)) return true
        val ok = runCatching { api.json("DELETE", "/auth/account") }.isSuccess
        if (ok) clearLocal()
        return ok
    }

    /** 改昵称。成功返回新的账号镜像；失败返回 null（界面保留原值让用户重试）。 */
    suspend fun updateNickname(nickname: String): Account? {
        val n = nickname.trim()
        if (n.isEmpty() || n.length > NICKNAME_MAX) return null
        val user = runCatching {
            api.json("PATCH", "/auth/profile", JSONObject().put("nickname", n))?.optJSONObject("user")
        }.getOrNull() ?: return null
        return mirror(user)
    }

    /** 登录 / 注册共用的提交路径。两者的返回形状一致（CLOUD-PLAN.md §3.1）。 */
    private suspend fun submit(path: String, body: JSONObject): AuthResult =
        runCatching { http.call("POST", path, body = body) }.fold(
            onSuccess = { if (it == null) AuthResult.Failure(msg(R.string.cloud_err_server)) else save(it) },
            onFailure = { AuthResult.Failure(message(it), (it as? CloudException)?.code ?: "") }
        )

    private fun save(json: JSONObject): AuthResult {
        val access = json.optString("access_token", "")
        val refresh = json.optString("refresh_token", "")
        val user = json.optJSONObject("user")
        if (access.isBlank() || refresh.isBlank() || user == null) {
            return AuthResult.Failure(msg(R.string.cloud_err_server))
        }
        TokenStore.save(context, access, refresh)
        // Google 首次登录会带回资料图地址，交给 UI 层下载成头像（这里不碰文件系统）
        json.optString("google_picture", "").takeIf { it.startsWith("https://") }?.let {
            ProgressStore(context).pendingAvatarUrl = it
        }
        return AuthResult.Success(mirror(user))
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

    private fun clearLocal() {
        TokenStore.clear(context)
        prefs.edit().clear().apply()
    }

    /** 设备信息只用于「已登录设备」列表，不含任何硬件标识。 */
    private fun device(): JSONObject = JSONObject()
        .put("device_id", ProgressStore(context).deviceId)
        .put("platform", "android")
        .put("app_version", BuildConfig.VERSION_NAME)

    private fun validate(email: String, password: String): String? = when {
        email.isEmpty() || !EMAIL_REGEX.matches(email) -> msg(R.string.cloud_err_email_format)
        password.length < PASSWORD_MIN -> msg(R.string.cloud_err_password_short)
        else -> null
    }

    private fun message(t: Throwable): String {
        val e = t as? CloudException ?: return msg(R.string.cloud_err_server)
        return when {
            e.isNetwork -> msg(R.string.cloud_err_network)
            e.code == "invalid_credentials" -> msg(R.string.cloud_err_credentials)
            e.code == "email_taken" -> msg(R.string.cloud_err_email_taken)
            e.code == "too_many_attempts" -> msg(R.string.cloud_err_too_many)
            e.code == "weak_password" -> msg(R.string.cloud_err_password_short)
            e.code == "invalid_email" -> msg(R.string.cloud_err_email_format)
            e.code == "invalid_nickname" -> msg(R.string.cloud_err_nickname)
            e.code == "identity_conflict" -> msg(R.string.cloud_err_identity_conflict)
            e.code == "identity_taken" -> msg(R.string.cloud_err_identity_taken)
            e.code == "account_locked" -> msg(R.string.cloud_err_locked)
            e.code == "invalid_code" -> msg(R.string.cloud_err_invalid_code)
            e.code == "invalid_google_token" -> msg(R.string.cloud_err_google_failed)
            e.code == "google_not_configured" -> msg(R.string.cloud_err_google_unavailable)
            // 服务器到不了 Google 的验签端点：不是用户的错，也不该让他反复重试
            e.code == "google_unreachable" -> msg(R.string.cloud_err_google_unavailable)
            e.code == "email_unverified" -> msg(R.string.cloud_err_google_unverified)
            e.code == "too_many_requests" -> msg(R.string.cloud_err_rate_limited)
            e.code == "mail_failed" -> msg(R.string.cloud_err_mail_failed)
            e.code == "mail_not_configured" || e.code == "code_not_configured" ->
                msg(R.string.cloud_err_mail_failed)
            e.code == "invalid_token" || e.code == "unauthorized" || e.code == "token_expired" ->
                msg(R.string.cloud_err_session_expired)
            else -> msg(R.string.cloud_err_server)
        }
    }

    private fun msg(id: Int): String = context.getString(id)
}
