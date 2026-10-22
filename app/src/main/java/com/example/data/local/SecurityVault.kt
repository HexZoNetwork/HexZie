package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityVault(private val context: Context) {

    private val keyAlias = "hexzie_master_key"
    private val prefs = context.getSharedPreferences("hexzie_secure_vault", Context.MODE_PRIVATE)

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (_: Exception) {
        null
    }

    private var fallbackKey: SecretKey? = null

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        if (keyStore != null) {
            try {
                if (!keyStore.containsAlias(keyAlias)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        "AndroidKeyStore"
                    )
                    val spec = KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                    keyGenerator.init(spec)
                    keyGenerator.generateKey()
                }
            } catch (_: Exception) {
                // If hardware keystore fails, initialize fallback
                initFallbackKey()
            }
        } else {
            initFallbackKey()
        }
    }

    private fun initFallbackKey() {
        val raw = "hexzie_sec_vault_32byte_fallback".toByteArray(Charsets.UTF_8)
        fallbackKey = SecretKeySpec(raw, 0, 32, "AES")
    }

    private fun getSecretKey(): SecretKey {
        return try {
            keyStore?.getKey(keyAlias, null) as? SecretKey ?: fallbackKey ?: initFallbackKey().let { fallbackKey!! }
        } catch (_: Exception) {
            fallbackKey ?: initFallbackKey().let { fallbackKey!! }
        }
    }

    fun storeApiKey(gatewayId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            prefs.edit().remove("key_$gatewayId").apply()
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString("key_$gatewayId", encoded).apply()
        } catch (_: Exception) {}
    }

    fun getApiKey(gatewayId: String): String {
        val encoded = prefs.getString("key_$gatewayId", null) ?: return ""
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < 12) return ""
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val encrypted = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}
