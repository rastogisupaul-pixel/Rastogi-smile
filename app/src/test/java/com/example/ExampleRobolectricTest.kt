package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read smile app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SMILE", appName)
    }

    @Test
    fun `ecdh aes gcm roundtrip encrypts and decrypts bilingual text`() {
        val alice = CryptoManager.generateEcKeyPair()
        val bob = CryptoManager.generateEcKeyPair()
        val original = "नमस्ते! Let's meet at 5 PM 😊"

        val encrypted = CryptoManager.encryptMessage(original, alice.privateKeyBase64, bob.publicKeyBase64)
        assertNotNull(encrypted)
        assertNotEquals(original, encrypted!!.cipherTextBase64)

        val decryptedByBob = CryptoManager.decryptMessage(
            cipherTextBase64 = encrypted.cipherTextBase64,
            ivBase64 = encrypted.ivBase64,
            myPrivateKeyBase64 = bob.privateKeyBase64,
            peerPublicKeyBase64 = alice.publicKeyBase64
        )
        assertEquals(original, decryptedByBob)

        val eve = CryptoManager.generateEcKeyPair()
        val failedByEve = CryptoManager.decryptMessage(
            cipherTextBase64 = encrypted.cipherTextBase64,
            ivBase64 = encrypted.ivBase64,
            myPrivateKeyBase64 = eve.privateKeyBase64,
            peerPublicKeyBase64 = alice.publicKeyBase64
        )
        assertNull(failedByEve)
    }

    @Test
    fun `india +91 phone normalization and pin salt hashing`() {
        assertEquals("+919876543210", CryptoManager.normalizePhoneToE164("98765 43210"))
        assertEquals("+919876543210", CryptoManager.normalizePhoneToE164("09876543210"))
        assertEquals(
            CryptoManager.hashPhoneForLookup("9876543210"),
            CryptoManager.hashPhoneForLookup("+91 98765-43210")
        )
        assertNotEquals(
            CryptoManager.hashPinWithSalt("1234", "userA"),
            CryptoManager.hashPinWithSalt("1234", "userB")
        )
    }
}
