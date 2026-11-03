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
 * 登录 token 的本地保管。
 *
 * 不用明文存 SharedPreferences：token 等同于账号，root 过的机器能直接读 XML。
 * 这里用 Android Keystore 生成一把**不可导出**的 AES 密钥，加密后把
 * IV + 密文一起存 prefs。密钥在硬件里，拷走文件也解不开。
 *
 * 刻意不引 androidx.security-crypto：工程一直零第三方运行时依赖，
 * 这点代码不到 80 行，没必要为它加一个库。
 */
object TokenStore {

    private const val PREFS = "ascent_auth"
    private const val KEY_CIPHER = "token_cipher"
    private const val KEY_ALIAS = "ascent_token_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    fun save(context: Context, token: String) {
        val encrypted = runCatching { encrypt(token) }.getOrNull() ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CIPHER, encrypted).apply()
    }

    fun load(context: Context): String? {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CIPHER, null) ?: return null
        val token = runCatching { decrypt(stored) }.getOrNull()
        // 解不开（换机、清过 Keystore、密钥被系统作废）就当没登录，让用户重新登
        if (token == null) clear(context)
        return token
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_CIPHER).apply()
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
