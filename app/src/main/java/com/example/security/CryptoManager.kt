package com.example.security

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val CURVE_NAME = "secp256r1"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    data class KeyPairEncoded(
        val publicKeyBase64: String,
        val privateKeyBase64: String
    )

    data class EncryptedPayload(
        val cipherTextBase64: String,
        val ivBase64: String
    )

    fun generateEcKeyPair(): KeyPairEncoded {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(CURVE_NAME))
        val pair = generator.generateKeyPair()
        return KeyPairEncoded(
            publicKeyBase64 = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP),
            privateKeyBase64 = Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP)
        )
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hashPinWithSalt(pin: String, userId: String): String {
        return sha256Hex("smile_pin_v1:$userId:$pin")
    }

    fun normalizePhoneToE164(raw: String, defaultCountryCode: String = "+91"): String {
        val trimmed = raw.trim().replace(Regex("[\\s\\-()]"), "")
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("+")) return trimmed
        if (trimmed.startsWith("0") && trimmed.length == 11) {
            return "$defaultCountryCode${trimmed.drop(1)}"
        }
        return if (trimmed.length == 10 && defaultCountryCode == "+91") {
            "+91$trimmed"
        } else {
            "$defaultCountryCode$trimmed"
        }
    }

    fun hashPhoneForLookup(rawPhone: String, defaultCountryCode: String = "+91"): String {
        val normalized = normalizePhoneToE164(rawPhone, defaultCountryCode)
        if (normalized.isBlank()) return ""
        return sha256Hex("smile_phone_v1:$normalized")
    }

    fun keyFingerprint(publicKeyBase64: String): String {
        if (publicKeyBase64.isBlank()) return "Not initialized"
        val hex = sha256Hex(publicKeyBase64).uppercase()
        return hex.take(32).chunked(4).joinToString(" ")
    }

    private fun deriveSharedSecretKey(
        myPrivateKeyBase64: String,
        peerPublicKeyBase64: String
    ): SecretKey {
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(Base64.decode(myPrivateKeyBase64, Base64.NO_WRAP))
        )
        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP))
        )
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val rawSharedSecret = agreement.generateSecret()
        val aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(rawSharedSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    fun encryptMessage(
        plaintext: String,
        myPrivateKeyBase64: String,
        peerPublicKeyBase64: String
    ): EncryptedPayload? {
        if (myPrivateKeyBase64.isBlank() || peerPublicKeyBase64.isBlank()) return null
        return try {
            val secretKey = deriveSharedSecretKey(myPrivateKeyBase64, peerPublicKeyBase64)
            val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            EncryptedPayload(
                cipherTextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decryptMessage(
        cipherTextBase64: String,
        ivBase64: String,
        myPrivateKeyBase64: String,
        peerPublicKeyBase64: String
    ): String? {
        if (cipherTextBase64.isBlank() || ivBase64.isBlank() ||
            myPrivateKeyBase64.isBlank() || peerPublicKeyBase64.isBlank()
        ) return null
        return try {
            val secretKey = deriveSharedSecretKey(myPrivateKeyBase64, peerPublicKeyBase64)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
