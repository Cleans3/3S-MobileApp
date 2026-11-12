package shelftap.card;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every case runs the wire contract in smart-card/card-spec.md against a fresh simulated card. */
class LibraryCardAppletTest {
    static final byte[] CARD_ID = {1, 2, 3, 4, 5, 6, 7, 8};
    static final byte[] ADMIN_KEY = hex("00112233445566778899AABBCCDDEEFF");
    static final byte[] PIN = ascii("482916");
    static final byte[] NEW_PIN = ascii("703154");
    static final byte[] WRONG_PIN = ascii("111111");

    static final int OK = 0x9000;

    SimCard card;

    @BeforeEach
    void freshCard() {
        card = new SimCard();
    }

    // --- C1, C8 ---

    @Test
    void blankCardReportsBlankState() {
        byte[] status = status();
        assertArrayEquals(new byte[8], Arrays.copyOf(status, 8));
        assertEquals(0, status[8]);
        assertSw(0x6985, card.send(0x20, 0, 0, PIN));
    }

    @Test
    void issueActivatesOnceAndLeavesPinValidated() {
        issue();
        byte[] status = status();
        assertArrayEquals(CARD_ID, Arrays.copyOf(status, 8));
        assertEquals(1, status[8]);
        assertEquals(3, status[9]);
        assertEquals(0x05, status[10]); // PIN validated + key pair present
        assertSw(0x6985, card.send(0x10, 0, 0, issueData()));
    }

    @Test
    void issueRejectsNonDigitPinAndWrongLength() {
        byte[] data = issueData();
        data[8] = 'a';
        assertSw(0x6A80, card.send(0x10, 0, 0, data));
        assertSw(0x6700, card.send(0x10, 0, 0, Arrays.copyOf(issueData(), 29)));
        assertEquals(0, status()[8]);
    }

    // --- C2, C3 ---

    @Test
    void threeWrongPinsBlockTheCard() {
        issue();
        card.select();
        assertSw(0x63C2, card.send(0x20, 0, 0, WRONG_PIN));
        assertSw(0x63C1, card.send(0x20, 0, 0, WRONG_PIN));
        assertSw(0x63C0, card.send(0x20, 0, 0, WRONG_PIN));
        assertSw(0x6983, card.send(0x20, 0, 0, PIN));
        assertEquals(2, status()[8]);
        assertEquals(0, status()[9]);
    }

    @Test
    void correctPinResetsTheTryCounter() {
        issue();
        card.select();
        assertSw(0x63C2, card.send(0x20, 0, 0, WRONG_PIN));
        assertSw(OK, card.send(0x20, 0, 0, PIN));
        assertEquals(3, status()[9]);
    }

    @Test
    void changePinKeepsDataReadable() {
        issue();
        byte[] profile = padded(ascii("profile data"));
        assertSw(OK, card.send(0x32, 0, 0, profile));
        card.select();
        assertSw(0x63C2, card.send(0x22, 0, 0, concat(WRONG_PIN, NEW_PIN)));
        assertSw(OK, card.send(0x22, 0, 0, concat(PIN, NEW_PIN)));
        card.select();
        assertSw(0x63C2, card.send(0x20, 0, 0, PIN));
        assertSw(OK, card.send(0x20, 0, 0, NEW_PIN));
        assertArrayEquals(profile, ok(card.get(0x30, 0, 0, 255)));
    }

    @Test
    void changePinRejectsBadNewPinWithoutUsingATry() {
        issue();
        card.select();
        assertSw(0x6A80, card.send(0x22, 0, 0, concat(PIN, ascii("12 456"))));
        assertEquals(3, status()[9]);
    }

    // --- C4 ---

    @Test
    void adminResetUnblocksAndKeepsDataReadable() throws Exception {
        issue();
        byte[] profile = padded(ascii("profile data"));
        assertSw(OK, card.send(0x32, 0, 0, profile));
        card.select();
        for (int i = 0; i < 3; i++) {
            card.send(0x20, 0, 0, WRONG_PIN);
        }
        adminLogin();
        assertSw(OK, card.send(0x28, 0, 0, NEW_PIN));
        card.select();
        assertEquals(3, status()[9]);
        assertSw(OK, card.send(0x20, 0, 0, NEW_PIN));
        assertArrayEquals(profile, ok(card.get(0x30, 0, 0, 255)));
    }

    @Test
    void adminResponseIsCheckedAndChallengeIsSingleUse() throws Exception {
        issue();
        card.select();
        byte[] challenge = ok(card.get(0x24, 0, 0, 16));
        byte[] wrong = aesEcb(hex("FFEEDDCCBBAA99887766554433221100"), challenge);
        assertSw(0x6982, card.send(0x26, 0, 0, wrong));
        assertSw(0x6985, card.send(0x26, 0, 0, aesEcb(ADMIN_KEY, challenge)));
        assertSw(0x6982, card.send(0x28, 0, 0, NEW_PIN));
    }

    // --- C5, C6 ---

    @Test
    void profileNeedsPinToReadAndRoundTrips() {
        issue();
        assertSw(0x6A88, card.get(0x30, 0, 0, 255));
        byte[] profile = padded(profileTlv());
        assertSw(OK, card.send(0x32, 0, 0, profile));
        card.select();
        assertSw(0x6982, card.get(0x30, 0, 0, 255));
        assertSw(0x6982, card.send(0x32, 0, 0, profile));
        assertSw(OK, card.send(0x20, 0, 0, PIN));
        assertArrayEquals(profile, ok(card.get(0x30, 0, 0, 255)));
    }

    @Test
    void profileLengthIsChecked() {
        issue();
        assertSw(0x6A80, card.send(0x32, 0, 0, new byte[15]));
        assertSw(0x6A84, card.send(0x32, 0, 0, new byte[208]));
    }

    // --- C9, C10 ---

    @Test
    void eightKilobytePhotoRoundTrips() {
        issue();
        byte[] jpeg = new byte[8191];
        new SecureRandom().nextBytes(jpeg);
        byte[] photo = padded(jpeg);
        assertEquals(8192, photo.length);
        writePhoto(photo);
        card.select();
        assertSw(OK, card.send(0x20, 0, 0, PIN));
        byte[] status = status();
        assertEquals(0x08, status[10] & 0x08);
        assertEquals(8192, ((status[11] & 0xFF) << 8) | (status[12] & 0xFF));

        ByteArrayOutputStream read = new ByteArrayOutputStream();
        for (int off = 0; off < photo.length; off += 240) {
            read.writeBytes(ok(card.get(0x44, off >> 8, off & 0xFF, 240)));
        }
        assertArrayEquals(photo, read.toByteArray());
        // Random access from any 16-byte boundary.
        assertArrayEquals(Arrays.copyOfRange(photo, 4000, 4240), ok(card.get(0x44, 4000 >> 8, 4000 & 0xFF, 240)));
        assertSw(0x6B00, card.get(0x44, 0, 8, 240)); // not on a 16-byte boundary
    }

    @Test
    void photoChunksMustArriveInOrder() {
        issue();
        assertSw(OK, card.send(0x40, 0, 0, new byte[]{0x01, 0x00}));
        assertSw(0x6B00, card.send(0x42, 0, 16, new byte[16]));
        assertSw(0x6985, card.get(0x44, 0, 0, 16)); // incomplete photo
        assertSw(0x6A84, card.send(0x40, 0, 0, new byte[]{0x20, 0x10}));
    }

    // --- C7 ---

    @Test
    void signatureVerifiesWithPublicKey() throws Exception {
        issue();
        card.select(); // no PIN needed
        PublicKey key = publicKey();
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        byte[] signature = concat(ok(card.sendGet(0x60, 0, 0, nonce, 128)), ok(card.get(0x60, 1, 0, 128)));
        assertEquals(256, signature.length);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key);
        verifier.update(CARD_ID);
        verifier.update(nonce);
        assertTrue(verifier.verify(signature));

        verifier.initVerify(key);
        verifier.update(CARD_ID);
        nonce[0] ^= 1;
        verifier.update(nonce);
        assertFalse(verifier.verify(signature));
    }

    @Test
    void signaturePartTwoOnlyRightAfterPartOne() {
        issue();
        assertSw(0x6985, card.get(0x60, 1, 0, 128));
        ok(card.sendGet(0x60, 0, 0, new byte[32], 128));
        status();
        assertSw(0x6985, card.get(0x60, 1, 0, 128));
    }

    @Test
    void signingWorksWhilePinIsBlocked() {
        issue();
        card.select();
        for (int i = 0; i < 3; i++) {
            card.send(0x20, 0, 0, WRONG_PIN);
        }
        ok(card.sendGet(0x60, 0, 0, new byte[32], 128));
    }

    @Test
    void unknownInstructionAndClass() {
        assertSw(0x6D00, card.get(0x7F, 0, 0, 0));
        assertSw(0x6E00, card.transmit(new CommandAPDU(0x00, 0x50, 0, 0, 13)));
    }

    // --- helpers ---

    void issue() {
        assertSw(OK, card.send(0x10, 0, 0, issueData()));
    }

    static byte[] issueData() {
        return concat(CARD_ID, PIN, ADMIN_KEY);
    }

    byte[] status() {
        return ok(card.get(0x50, 0, 0, 13));
    }

    /** The server's half of C4: it holds K_admin and answers the card's challenge. */
    void adminLogin() throws Exception {
        byte[] challenge = ok(card.get(0x24, 0, 0, 16));
        assertSw(OK, card.send(0x26, 0, 0, aesEcb(ADMIN_KEY, challenge)));
    }

    void writePhoto(byte[] photo) {
        assertSw(OK, card.send(0x40, 0, 0, new byte[]{(byte) (photo.length >> 8), (byte) photo.length}));
        for (int off = 0; off < photo.length; off += 240) {
            byte[] chunk = Arrays.copyOfRange(photo, off, Math.min(off + 240, photo.length));
            assertSw(OK, card.send(0x42, off >> 8, off & 0xFF, chunk));
        }
    }

    PublicKey publicKey() throws Exception {
        byte[] modulus = concat(ok(card.get(0x62, 0, 0, 128)), ok(card.get(0x62, 1, 0, 128)));
        byte[] exponent = ok(card.get(0x62, 2, 0, 3));
        return KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent)));
    }

    static byte[] profileTlv() {
        ByteArrayOutputStream tlv = new ByteArrayOutputStream();
        tlv(tlv, 1, ascii("M2026000123"));
        tlv(tlv, 2, "Nguyễn Văn An".getBytes(StandardCharsets.UTF_8));
        tlv(tlv, 3, ascii("20040517"));
        tlv(tlv, 4, ascii("001204012345"));
        tlv(tlv, 5, ascii("0912345678"));
        tlv(tlv, 6, new byte[]{0x01});
        tlv(tlv, 7, ascii("20301231"));
        return tlv.toByteArray();
    }

    static void tlv(ByteArrayOutputStream out, int tag, byte[] value) {
        out.write(tag);
        out.write(value.length);
        out.writeBytes(value);
    }

    /** ISO/IEC 7816-4 padding: 80 then 00 up to a multiple of 16. */
    static byte[] padded(byte[] data) {
        byte[] out = Arrays.copyOf(data, (data.length / 16 + 1) * 16);
        out[data.length] = (byte) 0x80;
        return out;
    }

    static byte[] aesEcb(byte[] key, byte[] block) throws Exception {
        Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return aes.doFinal(block);
    }

    static byte[] ok(ResponseAPDU response) {
        assertSw(OK, response);
        return response.getData();
    }

    static void assertSw(int expected, ResponseAPDU response) {
        assertEquals(Integer.toHexString(expected), Integer.toHexString(response.getSW()));
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] hex(String s) {
        return java.util.HexFormat.of().parseHex(s);
    }
}
