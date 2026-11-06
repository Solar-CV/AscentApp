package com.ascent.climb.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.ascent.climb.data.cloud.CloudConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest

/**
 * Google 登录：拿一枚 ID token 交给服务端验票。
 *
 * 为什么要 Credential Manager 而不是自己拼 OAuth：
 * 自己走授权码流程要在服务端保存 client secret、要处理重定向与 App Links，
 * 而 ID token 这条路客户端不需要任何密钥，服务端只验签。
 *
 * 前置条件（缺一个都会失败，且报错看起来都像代码问题）：
 *  1. Google Cloud 里有 **Android** OAuth client：包名 + 签名 SHA-1 都要对得上；
 *  2. 同意屏幕已发布到「生产」，否则只有测试名单里的账号能登录；
 *  3. [CloudConfig.GOOGLE_WEB_CLIENT_ID] 与服务端 `GOOGLE_CLIENT_ID` 是同一个值。
 *
 * 注意 [NoCredentialException] 是**有歧义**的：设备没有 Google 账号会抛它，
 * Android client 的包名 / SHA-1 没登记也会抛它（Google 找不到匹配的 client，
 * 于是当成「没有可用凭证」）。所以这里把环境信息一并带出去，便于定位。
 */
object GoogleSignIn {

    sealed interface Result {
        data class Success(val idToken: String) : Result

        /** 用户主动取消：不是错误，界面应静默回到原状。 */
        data object Cancelled : Result

        /** 设备上没有可用的 Google 账号。[detail] 仅供 debug 包展示。 */
        data class NoAccount(val detail: String) : Result

        /** 这台设备没有 Google Play 服务，或它被停用/版本过旧。 */
        data class NoPlayServices(val detail: String) : Result

        data class Failed(val code: String, val detail: String = "") : Result
    }

    suspend fun signIn(context: Context): Result {
        val activity = context.findActivity()
        if (activity == null) {
            Log.w("GoogleSignIn", "no activity from context=${context.javaClass.name}")
            return Result.Failed("no_activity")
        }
        if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return Result.Failed("not_configured")

        val env = environment(context)
        if (!env.gmsUsable) {
            Log.w("GoogleSignIn", "gms unavailable: $env")
            return Result.NoPlayServices(env.describe())
        }

        val option = GetGoogleIdOption.Builder()
            // false：把「使用其他账号」也列出来。只列已授权账号会让第一次使用的用户看不到入口。
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(CloudConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = CredentialManager.create(context).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Failed("unexpected_credential")
            }
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            // 这一条可能真是「没账号」，也可能是 Android client 没登记。把现场信息带出去。
            Log.w("GoogleSignIn", "no credential available, env=${env.describe()}", e)
            Result.NoAccount(env.describe())
        } catch (e: GetCredentialException) {
            // Credential Manager 把原因都塞在 type/message 里，不打出来就只能看到「登录失败」。
            // 账号选择器已经弹出、选完才失败，说明包名与签名都对，问题在换 token 这一步，
            // 最常见的是 serverClientId 不是「Web 应用」类型的 client。
            Log.w("GoogleSignIn", "credential failed: type=${e.type} msg=${e.message} env=${env.describe()}", e)
            Result.Failed(e.type, describeFailure(e) + " · " + env.describe())
        }
    }

    /** 异常类型去掉包名 + 原始 message 截断，够在截图里看出原因。 */
    private fun describeFailure(e: GetCredentialException): String {
        val type = e.type.substringAfterLast('.')
        val msg = e.message
            ?.replace('\n', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(96)
            ?.takeIf { it.isNotEmpty() }
        return if (msg != null) "$type: $msg" else type
    }

    // ---------------------------------------------------------------- 现场信息

    private data class Env(
        val packageName: String,
        val sha1: String,
        val gmsInstalled: Boolean,
        val gmsEnabled: Boolean,
        val gmsVersion: String,
        val playStore: Boolean
    ) {
        val gmsUsable: Boolean get() = gmsInstalled && gmsEnabled

        /** 只有 debug 包会把这一行显示给开发者；release 里只进日志。 */
        fun describe(): String =
            "$packageName · SHA1 ${sha1.take(17)} · GMS ${if (gmsInstalled) gmsVersion else "无"}" +
                (if (playStore) "" else " · 无 Play 商店")
    }

    private fun environment(context: Context): Env {
        val pm = context.packageManager
        val gms = runCatching { pm.getPackageInfo("com.google.android.gms", 0) }.getOrNull()
        val enabled = runCatching { pm.getApplicationInfo("com.google.android.gms", 0).enabled }
            .getOrDefault(false)
        return Env(
            packageName = context.packageName,
            sha1 = signatureSha1(context),
            gmsInstalled = gms != null,
            gmsEnabled = enabled,
            gmsVersion = gms?.versionName ?: "",
            playStore = runCatching { pm.getPackageInfo("com.android.vending", 0) }.isSuccess
        )
    }

    /**
     * 本机这个包的签名指纹。
     *
     * 用处很直接：登录失败时对比 Google Cloud 里登记的 SHA-1。
     * 换一台机器构建 debug 包，指纹就变了 —— 这是最容易误判成「代码写错」的一类问题。
     */
    private fun signatureSha1(context: Context): String = runCatching {
        val pm = context.packageManager
        val bytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.firstOrNull()?.toByteArray()
        }
        bytes?.let { MessageDigest.getInstance("SHA-1").digest(it).joinToString(":") { b -> "%02X".format(b) } }
    }.getOrNull() ?: "?"
}
