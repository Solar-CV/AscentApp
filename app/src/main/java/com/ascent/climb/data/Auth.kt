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

/** 验证码用途。注册与找回密码共用同一条通道，靠它区分。 */
enum class CodePurpose(val wire: String) { Register("register"), Reset("reset") }

/**
 * 登录接口（Auth API）。
 *
 * 这一层是刻意抽出来的：界面只依赖 [AuthApi]，后端就绪后把
 * [MockAuthApi] 换成真实实现（HTTP / SDK）即可，UI 无需改动。
 *
 * 真实实现时的建议签名保持不变，仅在内部改为网络请求：
 *   POST /auth/login    { email, password } -> { token, user }
 *   POST /auth/register { email, password, nickname, code } -> { token, user }
 *   POST /auth/logout
 */
interface AuthApi {
    /**
     * 当前已登录账号；未登录返回 null。
     *
     * 放进接口是因为界面不该知道自己接的是 Mock 还是云端实现——
     * 换实现时一行 UI 代码都不用动。
     */
    fun currentAccount(): Account?

    suspend fun login(email: String, password: String): AuthResult

    /**
     * 注册。[code] 是邮箱验证码；服务端 `REQUIRE_EMAIL_CODE=false` 时可以不填。
     * 默认值让「还没接发信」的环境（Mock）不必跟着改调用点。
     */
    suspend fun register(email: String, password: String, nickname: String, code: String = ""): AuthResult

    suspend fun logout()

    /**
     * 发送验证码。**返回 null 表示已发出**，否则是可展示的错误文案。
     *
     * 用 `String?` 而不是 `AuthResult`：发码不产生登录态，不该被误当成一次成功登录。
     * 与服务端一致：无论邮箱是否已注册都会返回成功，避免这个接口变成注册状态查询器。
     */
    suspend fun sendCode(email: String, purpose: CodePurpose = CodePurpose.Register): String?

    /** 用验证码重置密码。返回 null 表示成功。成功后会踢掉该账号所有会话，需要重新登录。 */
    suspend fun resetPassword(email: String, code: String, newPassword: String): String?

    /**
     * 删除账号。真实实现对应 `DELETE /auth/account`：**服务端要把该账号的数据一并删掉**，
     * 不是只清登录态（Google Play 的账号删除要求）。
     *
     * @return 是否成功。失败时界面保持原状，让用户重试。
     */
    suspend fun deleteAccount(): Boolean
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
    override fun currentAccount(): Account? {
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

    override suspend fun register(email: String, password: String, nickname: String, code: String): AuthResult {
        delay(750)
        val e = email.trim()
        val n = nickname.trim()
        validate(e, password)?.let { return AuthResult.Failure(it) }
        if (n.isEmpty()) return AuthResult.Failure("请填写昵称")
        if (n.length > 16) return AuthResult.Failure("昵称不能超过 16 个字符")
        return save(e, n)
    }

    /** 本地没有发信通道：假装发成功，让界面的倒计时与错误态仍然可测。 */
    override suspend fun sendCode(email: String, purpose: CodePurpose): String? {
        delay(400)
        if (!EMAIL_REGEX.matches(email.trim())) return "邮箱格式不正确"
        return null
    }

    /** 本地重置：只校验格式，不真的改密码（没有云端就没有密码）。 */
    override suspend fun resetPassword(email: String, code: String, newPassword: String): String? {
        delay(500)
        if (!EMAIL_REGEX.matches(email.trim())) return "邮箱格式不正确"
        if (!Regex("^\\d{6}$").matches(code.trim())) return "验证码不正确或已失效"
        if (newPassword.length < 6) return "密码至少 6 位"
        return null
    }

    override suspend fun logout() {
        prefs.edit().clear().apply()
    }

    /**
     * 本地实现的删除账号：清掉账号 prefs 即可，没有服务端记录要删。
     * 学习进度等本机数据不在这里动 —— 那是 ProgressStore 的事，由界面层统一清
     *（见 ProgressStore.wipeUserData），免得「账号删了、进度还在」。
     */
    override suspend fun deleteAccount(): Boolean {
        delay(450) // 与登录一致，让确认框里的忙碌态可见
        return runCatching { prefs.edit().clear().commit() }.getOrDefault(false)
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
