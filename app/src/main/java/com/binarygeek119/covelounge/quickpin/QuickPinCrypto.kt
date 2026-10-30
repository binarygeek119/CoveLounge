package com.binarygeek119.covelounge.quickpin

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object QuickPinCrypto {
    fun decrypt(pin: String, ciphertextB64: String): String {
        val normalized = pin.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(("Cove QuickPin v1" + normalized).toByteArray(Charsets.US_ASCII))
        val blob = Base64.decode(ciphertextB64, Base64.DEFAULT)
        val nonce = blob.copyOfRange(0, 12)
        val ctAndTag = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
        val plain = cipher.doFinal(ctAndTag)
        return String(plain, Charsets.UTF_8)
    }
}
