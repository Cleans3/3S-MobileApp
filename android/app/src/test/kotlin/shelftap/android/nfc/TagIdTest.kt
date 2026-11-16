package shelftap.android.nfc

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class TagIdTest {
    @Test
    fun formatsEightBytesAsSixteenUpperHexWithoutReversingThem() {
        val id = byteArrayOf(0xE0.toByte(), 0x04, 0x01, 0x50, 0x1A, 0x2B, 0x3C, 0x4D)
        assertEquals("E004015" + "01A2B3C4D", TagId.format(id))
        assertEquals(16, TagId.format(id)!!.length)
    }

    @Test
    fun keepsLeadingZerosAndUppercasesLetters() {
        assertEquals("000000000000ABCD", TagId.format(byteArrayOf(0, 0, 0, 0, 0, 0, 0xAB.toByte(), 0xCD.toByte())))
    }

    @Test
    fun rejectsAnythingThatIsNotAnEightByteUid() {
        assertNull(TagId.format(ByteArray(4)))
        assertNull(TagId.format(ByteArray(7)))
        assertNull(TagId.format(ByteArray(0)))
    }
}
