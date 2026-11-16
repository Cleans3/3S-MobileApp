package shelftap.android.nfc

import java.io.IOException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

private fun hex(s: String): ByteArray = s.replace(" ", "").hexToByteArray()

/** A card that answers like card-spec.md §5 says, and remembers what it was sent. */
private class FakeCard(
    val state: Int = 0x01,
    val firstHalf: ByteArray = ByteArray(128) { 1 },
    val secondHalf: ByteArray = ByteArray(128) { 2 },
    val contactlessRefuses: Boolean = false,
) : CardLink {
    val sent = mutableListOf<ByteArray>()

    override fun transceive(command: ByteArray): ByteArray {
        sent += command
        if (contactlessRefuses) return hex("6986")
        return when {
            command[1] == 0xA4.toByte() -> hex("9000")
            command[1] == 0x50.toByte() -> hex("0123456789ABCDEF") + byteArrayOf(state.toByte(), 3, 0x04, 0, 0) + hex("9000")
            command[1] == 0x60.toByte() && command[2] == 0x00.toByte() -> firstHalf + hex("9000")
            command[1] == 0x60.toByte() && command[2] == 0x01.toByte() -> secondHalf + hex("9000")
            else -> hex("6D00")
        }
    }
}

class CardClientTest {
    @Test
    fun apduBytesAreExactlyTheCardSpec() {
        assertContentEquals(hex("00 A4 04 00 0A F0 53 48 45 4C 46 54 41 50 01"), Apdu.select())
        assertContentEquals(hex("80 50 00 00 00"), Apdu.getStatus())
        val nonce = ByteArray(32) { it.toByte() }
        assertContentEquals(hex("80 60 00 00 20") + nonce + hex("00"), Apdu.authenticate(nonce))
        assertContentEquals(hex("80 60 01 00 00"), Apdu.authenticatePart2())
    }

    @Test
    fun nonceMustBe32Bytes() {
        assertFailsWith<IllegalArgumentException> { Apdu.authenticate(ByteArray(31)) }
    }

    @Test
    fun readCardIdSelectsThenReadsStatusAndReturnsFirst8BytesAsUpperHex() {
        val card = FakeCard()
        assertEquals("0123456789ABCDEF", CardClient(card).readCardId())
        assertContentEquals(Apdu.select(), card.sent[0])
        assertContentEquals(Apdu.getStatus(), card.sent[1])
    }

    @Test
    fun aBlankCardIsRefused() {
        assertFailsWith<CardException.Protocol> { CardClient(FakeCard(state = 0x00)).readCardId() }
    }

    @Test
    fun aBlockedPinCardStillIdentifiesItself() {
        assertEquals("0123456789ABCDEF", CardClient(FakeCard(state = 0x02)).readCardId())
    }

    @Test
    fun signSendsTheNonceThenPart2AndConcatenatesTheTwoHalvesIn256Bytes() {
        val card = FakeCard()
        val nonce = ByteArray(32) { 7 }
        val signature = CardClient(card).sign(nonce)
        assertEquals(256, signature.size)
        assertContentEquals(card.firstHalf + card.secondHalf, signature)
        assertContentEquals(Apdu.authenticate(nonce), card.sent[0])
        assertContentEquals(Apdu.authenticatePart2(), card.sent[1])
    }

    @Test
    fun aRefusedCommandSurfacesItsStatusWord() {
        val e = assertFailsWith<CardException.Status> { CardClient(FakeCard(contactlessRefuses = true)).readCardId() }
        assertEquals(0x6986, e.sw)
    }

    @Test
    fun aShortSignatureHalfIsAProtocolError() {
        assertFailsWith<CardException.Protocol> { CardClient(FakeCard(firstHalf = ByteArray(100))).sign(ByteArray(32)) }
    }

    @Test
    fun aTagLostMidExchangeIsReportedAsLost() {
        val link = CardLink { throw IOException("Tag was lost.") }
        assertFailsWith<CardException.Lost> { CardClient(link).readCardId() }
    }

    @Test
    fun aResponseWithoutAStatusWordIsAProtocolError() {
        assertFailsWith<CardException.Protocol> { Apdu.parse(byteArrayOf(0x01)) }
    }
}
