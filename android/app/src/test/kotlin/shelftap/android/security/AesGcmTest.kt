package shelftap.android.security

import javax.crypto.AEADBadTagException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import org.junit.Test

class AesGcmTest {
    private val aad = "shelftap.refresh-token.v1".toByteArray()
    private val key = AesGcm.key(AesGcm.newKeyBytes())

    @Test
    fun roundTrips() {
        val blob = AesGcm.seal(key, "refresh-token-123".toByteArray(), aad)
        assertEquals("refresh-token-123", String(AesGcm.open(key, blob, aad)))
    }

    @Test
    fun blobIsVersionIvCiphertextAndEveryEncryptionUsesAFreshIv() {
        val a = AesGcm.seal(key, "x".toByteArray(), aad)
        val b = AesGcm.seal(key, "x".toByteArray(), aad)
        assertEquals(1, a[0].toInt())
        assertEquals(1 + 12 + 1 + 16, a.size)
        assertNotEquals(AesGcm.ivOf(a).toList(), AesGcm.ivOf(b).toList())
    }

    @Test
    fun aChangedBlobIsRejected() {
        val blob = AesGcm.seal(key, "token".toByteArray(), aad)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        assertFailsWith<AEADBadTagException> { AesGcm.open(key, blob, aad) }
    }

    @Test
    fun aDifferentKeyOrAadIsRejected() {
        val blob = AesGcm.seal(key, "token".toByteArray(), aad)
        assertFailsWith<AEADBadTagException> { AesGcm.open(AesGcm.key(AesGcm.newKeyBytes()), blob, aad) }
        assertFailsWith<AEADBadTagException> { AesGcm.open(key, blob, "other".toByteArray()) }
    }

    @Test
    fun packAndUnpackAreInverse() {
        val iv = ByteArray(12) { it.toByte() }
        val ct = ByteArray(20) { 9 }
        val blob = AesGcm.pack(iv, ct)
        assertContentEquals(iv, AesGcm.ivOf(blob))
        assertContentEquals(ct, AesGcm.ciphertextOf(blob))
    }

    @Test
    fun rejectsABlobThatIsNotOurs() {
        assertFailsWith<IllegalArgumentException> { AesGcm.ivOf(ByteArray(30)) }
        assertFailsWith<IllegalArgumentException> { AesGcm.ivOf(byteArrayOf(1, 2, 3)) }
    }
}
