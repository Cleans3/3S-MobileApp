package shelftap.android.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM blob format shared by the Keystore keys and the software lock secret: `version(1) | iv(12) | ciphertext+tag`.
 * Pure JVM, so the format is unit-tested; the Keystore key itself is not.
 */
object AesGcm {
    const val KEY_BYTES = 32
    const val IV_BYTES = 12
    const val TAG_BITS = 128
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val VERSION: Byte = 1
    private val random = SecureRandom()

    fun newKeyBytes(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    fun key(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

    fun pack(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        require(iv.size == IV_BYTES) { "GCM IV must be $IV_BYTES bytes" }
        return byteArrayOf(VERSION) + iv + ciphertext
    }

    fun ivOf(blob: ByteArray): ByteArray = checked(blob).copyOfRange(1, 1 + IV_BYTES)

    fun ciphertextOf(blob: ByteArray): ByteArray = checked(blob).copyOfRange(1 + IV_BYTES, blob.size)

    fun decryptCipher(key: SecretKey, blob: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, ivOf(blob))) }

    /** Encrypts with a software key and a fresh random IV. */
    fun seal(key: SecretKey, plain: ByteArray, aad: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
        cipher.updateAAD(aad)
        return pack(iv, cipher.doFinal(plain))
    }

    /** Throws [javax.crypto.AEADBadTagException] on a wrong key, a changed blob or a different [aad]. */
    fun open(key: SecretKey, blob: ByteArray, aad: ByteArray): ByteArray = finish(decryptCipher(key, blob), blob, aad)

    /** Completes a decrypt on an already-initialised cipher (the Keystore path, after biometric auth). */
    fun finish(cipher: Cipher, blob: ByteArray, aad: ByteArray): ByteArray {
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertextOf(blob))
    }

    private fun checked(blob: ByteArray): ByteArray {
        require(blob.size > 1 + IV_BYTES && blob[0] == VERSION) { "not a ShelfTap GCM blob" }
        return blob
    }
}
