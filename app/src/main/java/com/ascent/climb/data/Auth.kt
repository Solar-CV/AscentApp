package com.ascent.climb.data

import android.content.Context
import kotlinx.coroutines.delay

/** 已登录账号。 */
data class Account(
    val id: String,
    val email: String,
    val nickname: String
)

/** 登录/注册结果。 */
sealed interface AuthResult {
    data class Success(val account: Account) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * 登录接口（Auth API）。
 *
 * 这一层是刻意抽出来的：界面只依赖 [AuthApi]，后端就绪后把
 * [MockAuthApi] 换成真实实现（HTTP / SDK）即可，UI 无需改动。
 *
 * 真实实现时的建议签名保持不变，仅在内部改为网络请求：
 *   POST /auth/login    { email, password } -> { token, user }
 *   POST /auth/register { email, password, nickname } -> { token, user }
 *   POST /auth/logout
 */
interface AuthApi {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String, nickname: String): AuthResult
    suspend fun logout()
}

/**
 * 本地模拟实现：不联网，只做格式校验，用于在真实后端接入前跑通完整界面与状态流转。
 * 任意满足格式要求的邮箱 + 6 位以上密码即可登录成功。
 */
class MockAuthApi(private val context: Context) : AuthApi {

    private val prefs = context.getSharedPreferences("ascent_account", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NICK = "nickname"
        private const val KEY_LOGGED_IN = "logged_in"

        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }

    /** 当前是否已登录。 */
    fun currentAccount(): Account? {
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return Account(
            id = prefs.getString(KEY_ID, "local") ?: "local",
            email = email,
            nickname = prefs.getString(KEY_NICK, email.substringBefore("@")) ?: email
        )
    }

    override suspend fun login(email: String, password: String): AuthResult {
        delay(650) // 模拟网络往返，让加载态可见
        val e = email.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        return save(e, e.substringBefore("@"))
    }

    override suspend fun register(email: String, password: String, nickname: String): AuthResult {
        delay(750)
        val e = email.trim()
        val n = nickname.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        if (n.isEmpty()) return AuthResult.Failure("请填写昵称")
        if (n.length > 16) return AuthResult.Failure("昵称不能超过 16 个字符")
        return save(e, n)
    }

    override suspend fun logout() {
        prefs.edit().clear().apply()
    }

    private fun validate(email: String, password: String): String? = when {
        email.isEmpty() -> "请填写邮箱"
        !EMAIL_REGEX.matches(email) -> "邮箱格式不正确"
        password.isEmpty() -> "请填写密码"
        password.length < 6 -> "密码至少 6 位"
        else -> null
    }

    private fun save(email: String, nickname: String): AuthResult {
        val id = "u_" + email.hashCode().toUInt().toString(16)
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_ID, id)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NICK, nickname)
            .apply()
        return AuthResult.Success(Account(id = id, email = email, nickname = nickname))
    }
}
