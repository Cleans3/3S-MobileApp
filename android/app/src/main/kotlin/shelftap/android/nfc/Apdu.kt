package shelftap.android.nfc

import java.io.IOException

/** Card errors, kept apart from [shelftap.android.api.ApiException] so the UI can tell "card" from "server". */
sealed class CardException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The tag left the field or the radio failed mid-exchange. */
    class Lost(cause: IOException) : CardException("card lost", cause)

    /** The card answered with a status word the flow does not accept (6986 = refused over contactless, 6A82 = not our applet, ...). */
    class Status(val sw: Int) : CardException("card status %04X".format(sw))

    /** The card answered, but not with what card-spec.md says. */
    class Protocol(message: String) : CardException(message)
}

/** Raw card transport: IsoDep in the app, a scripted fake in tests. */
fun interface CardLink {
    @Throws(IOException::class)
    fun transceive(command: ByteArray): ByteArray
}

/** ShelfTap applet APDUs, smart-card/card-spec.md §1 and §5. Short APDUs, CLA 80, only the NFC-allowed commands. */
object Apdu {
    val AID = byteArrayOf(0xF0.toByte(), 0x53, 0x48, 0x45, 0x4C, 0x46, 0x54, 0x41, 0x50, 0x01)
    const val SW_OK = 0x9000
    const val NONCE_LENGTH = 32
    const val SIGNATURE_HALF = 128

    fun select(): ByteArray = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID

    fun getStatus(): ByteArray = byteArrayOf(0x80.toByte(), 0x50, 0x00, 0x00, 0x00)

    fun authenticate(nonce: ByteArray): ByteArray {
        require(nonce.size == NONCE_LENGTH) { "nonce must be $NONCE_LENGTH bytes" }
        return byteArrayOf(0x80.toByte(), 0x60, 0x00, 0x00, NONCE_LENGTH.toByte()) + nonce + byteArrayOf(0x00)
    }

    fun authenticatePart2(): ByteArray = byteArrayOf(0x80.toByte(), 0x60, 0x01, 0x00, 0x00)

    class Response(val data: ByteArray, val sw: Int)

    fun parse(raw: ByteArray): Response {
        if (raw.size < 2) throw CardException.Protocol("short response")
        val sw = ((raw[raw.size - 2].toInt() and 0xFF) shl 8) or (raw[raw.size - 1].toInt() and 0xFF)
        return Response(raw.copyOfRange(0, raw.size - 2), sw)
    }
}

/** GET STATUS answer: cardId(8) | state(1) | triesLeft(1) | flags(1) | photoLen(2). */
class CardStatus(val cardId: String, val state: Int, val triesLeft: Int) {
    val issued get() = state != STATE_BLANK

    companion object {
        const val STATE_BLANK = 0x00
        private const val LENGTH = 13

        @OptIn(ExperimentalStdlibApi::class)
        fun parse(data: ByteArray): CardStatus {
            if (data.size != LENGTH) throw CardException.Protocol("GET STATUS returned ${data.size} bytes")
            return CardStatus(
                cardId = data.copyOfRange(0, 8).toHexString(HexFormat { upperCase = true }),
                state = data[8].toInt() and 0xFF,
                triesLeft = data[9].toInt() and 0xFF,
            )
        }
    }
}

/**
 * The phone's whole card conversation (card-spec.md §8, "Android card login"): identify the card, then sign the server's nonce.
 * No PIN and no profile over NFC; the card refuses both anyway (6986).
 */
class CardClient(private val link: CardLink) {
    /** SELECT the applet, GET STATUS. Throws [CardException.Protocol] for a blank (never issued) card. */
    fun readCardId(): String {
        send(Apdu.select())
        val status = CardStatus.parse(send(Apdu.getStatus()).data)
        if (!status.issued) throw CardException.Protocol("card not issued")
        return status.cardId
    }

    /** AUTHENTICATE part 1 and part 2: the 256-byte RSA signature over cardId ‖ nonce, made on the card. */
    fun sign(nonce: ByteArray): ByteArray {
        val first = send(Apdu.authenticate(nonce)).data
        val second = send(Apdu.authenticatePart2()).data
        if (first.size != Apdu.SIGNATURE_HALF || second.size != Apdu.SIGNATURE_HALF) {
            throw CardException.Protocol("signature halves are ${first.size} and ${second.size} bytes")
        }
        return first + second
    }

    private fun send(command: ByteArray): Apdu.Response {
        val response = try {
            Apdu.parse(link.transceive(command))
        } catch (e: IOException) {
            throw CardException.Lost(e)
        }
        if (response.sw != Apdu.SW_OK) throw CardException.Status(response.sw)
        return response
    }
}
