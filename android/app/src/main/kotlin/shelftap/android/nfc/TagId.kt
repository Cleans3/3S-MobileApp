package shelftap.android.nfc

/** Book-tag ID for `POST /api/loans`: the bytes of `Tag.getId()` for an NfcV tag, as Android returns them. */
object TagId {
    private const val LENGTH = 8

    /** 16 uppercase hex chars, no byte reversal, or null when the tag does not have an 8-byte ISO 15693 UID. */
    @OptIn(ExperimentalStdlibApi::class)
    fun format(id: ByteArray): String? =
        if (id.size == LENGTH) id.toHexString(HexFormat { upperCase = true }) else null
}
