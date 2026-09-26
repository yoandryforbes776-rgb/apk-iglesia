package com.ministerio.jovenes.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }
        .let { Base64.encodeToString(it, Base64.NO_WRAP) }
    fun hash(password: String, salt: String): String {
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 120_000, 256)).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    fun verify(password: String, salt: String, expected: String): Boolean =
        hash(password, salt).toByteArray().contentEquals(expected.toByteArray())
}
