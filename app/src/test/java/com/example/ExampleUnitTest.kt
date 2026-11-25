package com.example

import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `india +91 phone normalization and hash lookup consistency`() {
        assertEquals("+919876543210", CryptoManager.normalizePhoneToE164("98765 43210"))
        assertEquals("+919876543210", CryptoManager.normalizePhoneToE164("09876543210"))
        assertEquals(
            CryptoManager.hashPhoneForLookup("9876543210"),
            CryptoManager.hashPhoneForLookup("+91 98765-43210")
        )
    }

    @Test
    fun `pin hashing uses per-user salt`() {
        val hashA = CryptoManager.hashPinWithSalt("2580", "uid_alice")
        val hashB = CryptoManager.hashPinWithSalt("2580", "uid_bob")
        assertNotEquals(hashA, hashB)
        assertEquals(64, hashA.length)
    }
}
