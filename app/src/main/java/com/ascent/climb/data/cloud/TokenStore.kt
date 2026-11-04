package com.ascent.climb.data.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 登录凭证的本地保管。
 *
 * 两种 token（CLOUD-PLAN.md §10.1 A4）：
 *  - access  1 小时，随每个请求带上；
 *  - refresh 90 天，只在换 access 时用。
 * 分开存是为了「access 过期」能自动续，而不是把用户踢回登录页。
 *
 * 不用明文存 SharedPreferences：token 等同于账号，root 过的机器能直接读 XML。
 * 这里用 Android Keystore 生成一把**不可导出**的 AES 密钥，加密后把
 * IV + 密文一起存 prefs。密钥在硬件里，拷走文件也解不开。
 *
 * 刻意不引 androidx.security-crypto：工程一直零第三方运行时依赖，
 * 这点代码不到 90 行，没必要为它加一个库。
 */
object TokenStore {

    private const val PREFS = "ascent_auth"
    private const val KEY_ACCESS = "access_cipher"
    private const val KEY_REFRESH = "refresh_cipher"
    private const val KEY_LEGACY = "token_cipher"
    private const val KEY_ALIAS = "ascent_token_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    fun save(context: Context, access: String, refresh: String) {
        val a = runCatching { encrypt(access) }.getOrNull() ?: return
        val r = runCatching { encrypt(refresh) }.getOrNull() ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS, a)
            .putString(KEY_REFRESH, r)
            // 单 token 时代的残留，留着只会让「是否已登录」判断出错
            .remove(KEY_LEGACY)
            .apply()
    }

    fun access(context: Context): String? = read(context, KEY_ACCESS)

    fun refresh(context: Context): String? = read(context, KEY_REFRESH)

    /** 只要还有 refresh 就算「登录着」：access 过期是常态，不该让界面闪成未登录。 */
    fun hasSession(context: Context): Boolean = refresh(context) != null

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_LEGACY).apply()
    }

    private fun read(context: Context, key: String): String? {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null) ?: return null
        val plain = runCatching { decrypt(stored) }.getOrNull()
        // 解不开（换机、清过 Keystore、密钥被系统作废）就当没登录，让用户重新登
        if (plain == null) clear(context)
        return plain
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // 不要求解锁设备：学习 App 没必要在锁屏后让后台同步失败
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(1 + iv.size + body.size)
        packed[0] = iv.size.toByte()
        iv.copyInto(packed, 1)
        body.copyInto(packed, 1 + iv.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? {
        val packed = Base64.decode(stored, Base64.NO_WRAP)
        if (packed.size < 2) return null
        val ivSize = packed[0].toInt()
        if (ivSize <= 0 || packed.size <= 1 + ivSize) return null
        val iv = packed.copyOfRange(1, 1 + ivSize)
        val body = packed.copyOfRange(1 + ivSize, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }
}
