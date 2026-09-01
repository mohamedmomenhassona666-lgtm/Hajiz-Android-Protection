package com.hajiz.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

class SecurePinManager(private val context: Context) {
    private val keyAlias = "hajiz_pin_key"
    private val random = SecureRandom()

    suspend fun setPin(pin: String) {
        require(pin.length >= 4) { "PIN must contain at least four digits" }
        val salt = ByteArray(16).also(random::nextBytes)
        val digest = derive(pin, salt)
        val encrypted = encrypt(digest)
        context.getSharedPreferences("hajiz_secure_state", Context.MODE_PRIVATE).edit()
            .putString("pin_ciphertext", encrypted.first)
            .putString("pin_iv", encrypted.second)
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val prefs = context.getSharedPreferences("hajiz_secure_state", Context.MODE_PRIVATE)
        val cipherText = prefs.getString("pin_ciphertext", null) ?: return false
        val iv = prefs.getString("pin_iv", null) ?: return false
        val salt = prefs.getString("pin_salt", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return try {
            val expected = Base64.decode(cipherText, Base64.NO_WRAP)
            java.security.MessageDigest.isEqual(decrypt(expected, iv), derive(pin, salt))
        } catch (_: Exception) {
            false
        }
    }

    fun hasPin(): Boolean =
        context.getSharedPreferences("hajiz_secure_state", Context.MODE_PRIVATE)
            .contains("pin_ciphertext")

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(keyAlias)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
                generateKey()
            }
        }
        return (store.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(value: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return Base64.encodeToString(cipher.doFinal(value), Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(value: ByteArray, encodedIv: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            keystoreKey(),
            GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)),
        )
        return cipher.doFinal(value)
    }
}