package shelftap.android.session

import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import shelftap.android.api.ApiHarness
import shelftap.android.nfc.Apdu
import shelftap.android.nfc.CardLink

class CardLoginTest {
    private val nonce = ByteArray(32) { (it + 1).toByte() }
    private val cardId = "0123456789ABCDEF"

    @Test
    fun cardLoginRunsChallengeAuthenticateAuthAndSendsNoPin() = ApiHarness().use { h ->
        runBlocking {
            val sent = mutableListOf<ByteArray>()
            val link = CardLink { cmd ->
                sent += cmd
                when (cmd[1].toInt() and 0xFF) {
                    0xA4 -> byteArrayOf(0x90.toByte(), 0x00)
                    0x50 -> "0123456789ABCDEF".hexToByteArray() + byteArrayOf(1, 3, 4, 0, 0, 0x90.toByte(), 0x00)
                    0x60 -> ByteArray(128) { (if (cmd[2].toInt() == 0) 0x11 else 0x22) } + byteArrayOf(0x90.toByte(), 0x00)
                    else -> byteArrayOf(0x6D, 0x00)
                }
            }
            h.enqueue(200, """{"nonce":"${Base64.getEncoder().encodeToString(nonce)}"}""")
            h.enqueueTokens("cat1", "crt1")

            h.session.cardLogin(link)

            assertEquals("/api/cards/$cardId/challenge", h.server.takeRequest().url.encodedPath)
            val auth = h.server.takeRequest()
            assertEquals("/api/cards/$cardId/auth", auth.url.encodedPath)
            assertEquals("ok", auth.headers["X-Device-Integrity"])
            val body = auth.body!!.utf8()
            assertTrue(body.contains(""""nonce":"${Base64.getEncoder().encodeToString(nonce)}""""))
            val signature = Regex(""""signature":"([^"]+)"""").find(body)!!.groupValues[1]
            assertEquals(256, Base64.getDecoder().decode(signature).size)
            assertTrue(body.contains(""""deviceId":"device-1""""))

            // SELECT, GET STATUS, AUTHENTICATE(nonce), AUTHENTICATE part 2: nothing else, in particular no VERIFY PIN (INS 20) or READ PROFILE (INS 30).
            assertEquals(listOf(0xA4, 0x50, 0x60, 0x60), sent.map { it[1].toInt() and 0xFF })
            assertContentEquals(Apdu.authenticate(nonce), sent[2])
            assertEquals("crt1", h.vault.token)
            assertEquals(SessionState.Active, h.session.state.value)
        }
    }

    @Test
    fun aRefusedCardNeverReachesTheServer() = ApiHarness().use { h ->
        runBlocking {
            val link = CardLink { byteArrayOf(0x69, 0x86.toByte()) }
            try {
                h.session.cardLogin(link)
            } catch (_: shelftap.android.nfc.CardException) {
            }
            assertEquals(0, h.server.requestCount)
            assertNull(h.vault.token)
        }
    }
}
