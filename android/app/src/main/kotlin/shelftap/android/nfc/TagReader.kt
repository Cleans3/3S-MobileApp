package shelftap.android.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import android.os.Bundle

enum class NfcAvailability { READY, DISABLED, MISSING }

/**
 * NFC in the foreground only: reader mode while a screen that needs a tag is resumed, nothing in the manifest.
 * The system never launches the app because a card or a tag was tapped.
 */
object TagReader {
    const val CARD_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B
    const val BOOK_FLAGS = NfcAdapter.FLAG_READER_NFC_V

    fun availability(context: Context): NfcAvailability {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.MISSING
        return if (adapter.isEnabled) NfcAvailability.READY else NfcAvailability.DISABLED
    }

    fun enable(activity: Activity, flags: Int, onTag: (Tag) -> Unit) {
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_MS) }
        NfcAdapter.getDefaultAdapter(activity)
            ?.enableReaderMode(activity, onTag, flags or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, extras)
    }

    fun disable(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }

    /** The tag as a card link: connected IsoDep. Caller closes it. */
    fun isoDep(tag: Tag): IsoDep? = IsoDep.get(tag)?.apply { timeout = ISO_DEP_TIMEOUT_MS }

    /** The 16-hex ID of an NfcV (ISO 15693) book tag, or null for anything else. */
    fun bookTagId(tag: Tag): String? = if (NfcV.get(tag) != null) TagId.format(tag.id) else null

    private const val PRESENCE_CHECK_MS = 250
    private const val ISO_DEP_TIMEOUT_MS = 5000
}
