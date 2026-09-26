package com.ministerio.jovenes.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredSupabaseSession(val accessToken: String, val refreshToken: String, val expiresAt: Long)

class SecureSessionStore(context: Context) {
    private val prefs=context.getSharedPreferences("secure_supabase_session",Context.MODE_PRIVATE)
    private val alias="impulso_joven_supabase_session"

    fun save(session: StoredSupabaseSession) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val plain=JSONObject().put("access",session.accessToken).put("refresh",session.refreshToken).put("expires",session.expiresAt).toString().toByteArray()
        val encrypted=cipher.doFinal(plain)
        prefs.edit().putString("payload",Base64.encodeToString(encrypted,Base64.NO_WRAP))
            .putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)).apply()
    }

    fun load(): StoredSupabaseSession? = runCatching {
        val payload=Base64.decode(prefs.getString("payload",null) ?: return null,Base64.NO_WRAP)
        val iv=Base64.decode(prefs.getString("iv",null) ?: return null,Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv)) }
        JSONObject(String(cipher.doFinal(payload))).let { StoredSupabaseSession(it.getString("access"),it.getString("refresh"),it.getLong("expires")) }
    }.getOrNull()

    fun clear() { prefs.edit().clear().apply() }
    fun hasSession()=load()!=null

    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
