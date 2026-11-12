package shelftap.card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.MessageDigest;
import javacard.security.RSAPublicKey;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

/**
 * ShelfTap library card. The wire contract is smart-card/card-spec.md; section numbers below refer to it.
 */
public class LibraryCardApplet extends Applet {

    static final byte CLA = (byte) 0x80;

    static final byte INS_ISSUE = (byte) 0x10;
    static final byte INS_VERIFY_PIN = (byte) 0x20;
    static final byte INS_CHANGE_PIN = (byte) 0x22;
    static final byte INS_GET_CHALLENGE = (byte) 0x24;
    static final byte INS_ADMIN_AUTH = (byte) 0x26;
    static final byte INS_RESET_PIN = (byte) 0x28;
    static final byte INS_READ_PROFILE = (byte) 0x30;
    static final byte INS_UPDATE_PROFILE = (byte) 0x32;
    static final byte INS_PHOTO_BEGIN = (byte) 0x40;
    static final byte INS_PHOTO_WRITE = (byte) 0x42;
    static final byte INS_PHOTO_READ = (byte) 0x44;
    static final byte INS_GET_STATUS = (byte) 0x50;
    static final byte INS_AUTHENTICATE = (byte) 0x60;
    static final byte INS_GET_PUBLIC_KEY = (byte) 0x62;

    static final byte STATE_BLANK = 0x00;
    static final byte STATE_ACTIVE = 0x01;
    static final byte STATE_BLOCKED = 0x02; // reported only: ACTIVE with 0 PIN tries left

    static final short SW_PIN_BLOCKED = (short) 0x6983;
    static final short SW_NO_PROFILE = (short) 0x6A88;
    static final short SW_WRONG_PIN = (short) 0x63C0;

    static final byte PIN_TRIES = 3;
    static final byte PIN_LEN = 6;
    static final short ID_LEN = 8;
    static final short BLOCK = 16;
    static final short NONCE_LEN = 32;
    static final short PROFILE_MAX = 192;
    static final short PHOTO_MAX = 8192;
    static final short CHUNK_MAX = 240;
    static final short RSA_LEN = 256;
    static final short HALF = 128;

    // DER DigestInfo prefix for SHA-256 (RFC 8017 §9.2, note 1).
    private static final byte[] SHA256_DIGEST_INFO = {
        0x30, 0x31, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20
    };

    // Transient flag indexes.
    private static final byte F_ADMIN = 0;
    private static final byte F_CHALLENGE = 1;
    private static final byte F_SIGNATURE = 2;

    // Scratch layout: [0..256) RSA block / signature / modulus (258 for jCardSim's sign byte), [258..274) a key being wrapped.
    private static final short S_KEY = 258;

    // Persistent state (§4).
    private byte state;
    private final byte[] cardId = new byte[ID_LEN];
    private final OwnerPIN pin = new OwnerPIN(PIN_TRIES, PIN_LEN);
    private final byte[] salt = new byte[BLOCK];
    private final byte[] wrapPin = new byte[BLOCK];
    private final byte[] wrapAdmin = new byte[BLOCK];
    private final AESKey adminKey;
    private final KeyPair rsaKeys;
    private final byte[] profile = new byte[(short) (BLOCK + PROFILE_MAX)]; // IV ‖ ciphertext
    private short profileLen;
    private final byte[] photo = new byte[PHOTO_MAX];
    private final byte[] photoIv = new byte[BLOCK];
    private short photoLen;
    private short photoWritten;

    // Session state, cleared on select and deselect.
    private final AESKey dataKey;
    private final AESKey kek;
    private final boolean[] flags;
    private final byte[] challenge;
    private final byte[] scratch;

    private final Cipher aesCbc;
    private final Cipher aesEcb;
    private final Cipher rsaRaw;
    private final MessageDigest sha256;
    private final RandomData random;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new LibraryCardApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    private LibraryCardApplet() {
        adminKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        dataKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_128, false);
        kek = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_128, false);
        rsaKeys = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_2048);
        flags = JCSystem.makeTransientBooleanArray((short) 3, JCSystem.CLEAR_ON_DESELECT);
        challenge = JCSystem.makeTransientByteArray(BLOCK, JCSystem.CLEAR_ON_DESELECT);
        scratch = JCSystem.makeTransientByteArray((short) (S_KEY + BLOCK), JCSystem.CLEAR_ON_DESELECT);
        aesCbc = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        aesEcb = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        rsaRaw = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
        sha256 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
    }

    public boolean select() {
        clearSession(); // jCardSim does not clear transient-deselect keys on re-select (§9, quirk 6)
        return true;
    }

    public void deselect() {
        clearSession();
    }

    private void clearSession() {
        pin.reset();
        dataKey.clearKey();
        kek.clearKey();
        flags[F_ADMIN] = false;
        flags[F_CHALLENGE] = false;
        flags[F_SIGNATURE] = false;
        Util.arrayFillNonAtomic(challenge, (short) 0, BLOCK, (byte) 0);
        Util.arrayFillNonAtomic(scratch, (short) 0, (short) scratch.length, (byte) 0);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        if (buf[ISO7816.OFFSET_CLA] != CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        byte ins = buf[ISO7816.OFFSET_INS];
        boolean signaturePart2 = ins == INS_AUTHENTICATE && buf[ISO7816.OFFSET_P1] == 1;
        if (!signaturePart2) {
            flags[F_SIGNATURE] = false;
        }
        if (isContactless() && ins != INS_GET_STATUS && ins != INS_AUTHENTICATE && ins != INS_GET_PUBLIC_KEY) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }
        switch (ins) {
            case INS_GET_STATUS: getStatus(apdu); return;
            case INS_ISSUE: issue(apdu); return;
            case INS_VERIFY_PIN: verifyPin(apdu); return;
            case INS_CHANGE_PIN: changePin(apdu); return;
            case INS_GET_CHALLENGE: getChallenge(apdu); return;
            case INS_ADMIN_AUTH: adminAuth(apdu); return;
            case INS_RESET_PIN: resetPin(apdu); return;
            case INS_READ_PROFILE: readProfile(apdu); return;
            case INS_UPDATE_PROFILE: updateProfile(apdu); return;
            case INS_PHOTO_BEGIN: photoBegin(apdu); return;
            case INS_PHOTO_WRITE: photoWrite(apdu); return;
            case INS_PHOTO_READ: photoRead(apdu); return;
            case INS_AUTHENTICATE: authenticate(apdu, signaturePart2); return;
            case INS_GET_PUBLIC_KEY: getPublicKey(apdu); return;
            default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // --- C8 ---

    private void getStatus(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        Util.arrayCopyNonAtomic(cardId, (short) 0, buf, (short) 0, ID_LEN);
        byte tries = pin.getTriesRemaining();
        buf[8] = state == STATE_BLANK ? STATE_BLANK : (tries == 0 ? STATE_BLOCKED : STATE_ACTIVE);
        buf[9] = tries;
        byte f = 0;
        if (pin.isValidated()) f |= 0x01;
        if (flags[F_ADMIN]) f |= 0x02;
        if (state == STATE_ACTIVE) f |= 0x04; // ISSUE generates the key pair before it activates the card
        if (photoComplete()) f |= 0x08;
        buf[10] = f;
        Util.setShort(buf, (short) 11, photoLen);
        apdu.setOutgoingAndSend((short) 0, (short) 13);
    }

    // --- C1 ---

    private void issue(APDU apdu) {
        if (state != STATE_BLANK) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        byte[] buf = apdu.getBuffer();
        receive(apdu, (short) (ID_LEN + PIN_LEN + BLOCK));
        short off = ISO7816.OFFSET_CDATA;
        short pinOff = (short) (off + ID_LEN);
        checkPinFormat(buf, pinOff);

        // Slow step first: if the card is pulled here the state is still BLANK and ISSUE can simply be sent again.
        rsaKeys.genKeyPair();

        Util.arrayCopy(buf, off, cardId, (short) 0, ID_LEN);
        adminKey.setKey(buf, (short) (pinOff + PIN_LEN));
        random.generateData(salt, (short) 0, BLOCK);
        random.generateData(scratch, S_KEY, BLOCK);
        dataKey.setKey(scratch, S_KEY);
        aesEcb.init(adminKey, Cipher.MODE_ENCRYPT);
        aesEcb.doFinal(scratch, S_KEY, BLOCK, wrapAdmin, (short) 0);
        wrapDataKeyUnderPin(buf, pinOff);
        pin.update(buf, pinOff, PIN_LEN);
        Util.arrayCopy(scratch, (short) 0, wrapPin, (short) 0, BLOCK);
        profileLen = 0;
        photoLen = 0;
        photoWritten = 0;
        state = STATE_ACTIVE; // single-byte write: the commit point of ISSUE
        pin.check(buf, pinOff, PIN_LEN); // the issuing session may write the profile and photo
        Util.arrayFillNonAtomic(scratch, (short) 0, (short) scratch.length, (byte) 0);
    }

    // --- C2, C3 ---

    private void verifyPin(APDU apdu) {
        requireActive();
        byte[] buf = apdu.getBuffer();
        receive(apdu, PIN_LEN);
        checkPin(buf, ISO7816.OFFSET_CDATA);
    }

    private void changePin(APDU apdu) {
        requireActive();
        byte[] buf = apdu.getBuffer();
        receive(apdu, (short) (2 * PIN_LEN));
        short newOff = (short) (ISO7816.OFFSET_CDATA + PIN_LEN);
        checkPinFormat(buf, newOff);
        checkPin(buf, ISO7816.OFFSET_CDATA);
        setPin(buf, newOff);
    }

    /** Checks a PIN, counting the try; on success loads the data key. Throws 63Cx or 6983 on failure. */
    private void checkPin(byte[] buf, short off) {
        checkPinFormat(buf, off);
        if (pin.getTriesRemaining() == 0) {
            ISOException.throwIt(SW_PIN_BLOCKED);
        }
        if (!pin.check(buf, off, PIN_LEN)) {
            ISOException.throwIt((short) (SW_WRONG_PIN | pin.getTriesRemaining()));
        }
        deriveKek(buf, off);
        aesEcb.init(kek, Cipher.MODE_DECRYPT);
        aesEcb.doFinal(wrapPin, (short) 0, BLOCK, scratch, S_KEY);
        dataKey.setKey(scratch, S_KEY);
        Util.arrayFillNonAtomic(scratch, S_KEY, BLOCK, (byte) 0);
        kek.clearKey();
    }

    /** Sets a new PIN and rewraps the loaded data key under it, atomically (§3). Leaves the new PIN validated. */
    private void setPin(byte[] buf, short off) {
        dataKey.getKey(scratch, S_KEY);
        wrapDataKeyUnderPin(buf, off);
        JCSystem.beginTransaction();
        pin.update(buf, off, PIN_LEN);
        Util.arrayCopy(scratch, (short) 0, wrapPin, (short) 0, BLOCK);
        JCSystem.commitTransaction();
        pin.check(buf, off, PIN_LEN);
        Util.arrayFillNonAtomic(scratch, (short) 0, (short) scratch.length, (byte) 0);
    }

    /** scratch[S_KEY] holds the data key; writes AES-ECB(KEK_pin, DK) to scratch[0..16). */
    private void wrapDataKeyUnderPin(byte[] buf, short pinOff) {
        deriveKek(buf, pinOff);
        aesEcb.init(kek, Cipher.MODE_ENCRYPT);
        aesEcb.doFinal(scratch, S_KEY, BLOCK, scratch, (short) 0);
        kek.clearKey();
    }

    /** KEK_pin = first 16 bytes of SHA-256(salt ‖ PIN). Uses scratch[0..32). */
    private void deriveKek(byte[] buf, short pinOff) {
        sha256.reset();
        sha256.update(salt, (short) 0, BLOCK);
        sha256.doFinal(buf, pinOff, PIN_LEN, scratch, (short) 0);
        kek.setKey(scratch, (short) 0);
        Util.arrayFillNonAtomic(scratch, (short) 0, (short) 32, (byte) 0);
    }

    private static void checkPinFormat(byte[] buf, short off) {
        for (short i = 0; i < PIN_LEN; i++) {
            byte c = buf[(short) (off + i)];
            if (c < (byte) 0x30 || c > (byte) 0x39) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
        }
    }

    // --- C4 ---

    private void getChallenge(APDU apdu) {
        requireActive();
        byte[] buf = apdu.getBuffer();
        random.generateData(challenge, (short) 0, BLOCK);
        flags[F_CHALLENGE] = true;
        Util.arrayCopyNonAtomic(challenge, (short) 0, buf, (short) 0, BLOCK);
        apdu.setOutgoingAndSend((short) 0, BLOCK);
    }

    private void adminAuth(APDU apdu) {
        if (!flags[F_CHALLENGE]) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        flags[F_CHALLENGE] = false; // single use, whatever the outcome
        byte[] buf = apdu.getBuffer();
        receive(apdu, BLOCK);
        aesEcb.init(adminKey, Cipher.MODE_ENCRYPT);
        aesEcb.doFinal(challenge, (short) 0, BLOCK, scratch, (short) 0);
        Util.arrayFillNonAtomic(challenge, (short) 0, BLOCK, (byte) 0);
        byte diff = 0;
        for (short i = 0; i < BLOCK; i++) {
            diff |= (byte) (scratch[i] ^ buf[(short) (ISO7816.OFFSET_CDATA + i)]);
        }
        Util.arrayFillNonAtomic(scratch, (short) 0, BLOCK, (byte) 0);
        if (diff != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        aesEcb.init(adminKey, Cipher.MODE_DECRYPT);
        aesEcb.doFinal(wrapAdmin, (short) 0, BLOCK, scratch, S_KEY);
        dataKey.setKey(scratch, S_KEY);
        Util.arrayFillNonAtomic(scratch, S_KEY, BLOCK, (byte) 0);
        flags[F_ADMIN] = true;
    }

    private void resetPin(APDU apdu) {
        requireAdmin();
        byte[] buf = apdu.getBuffer();
        receive(apdu, PIN_LEN);
        checkPinFormat(buf, ISO7816.OFFSET_CDATA);
        setPin(buf, ISO7816.OFFSET_CDATA);
    }

    // --- C5, C6 ---

    private void readProfile(APDU apdu) {
        requirePin();
        if (profileLen == 0) {
            ISOException.throwIt(SW_NO_PROFILE);
        }
        byte[] buf = apdu.getBuffer();
        aesCbc.init(dataKey, Cipher.MODE_DECRYPT, profile, (short) 0, BLOCK);
        aesCbc.doFinal(profile, BLOCK, profileLen, buf, (short) 0);
        apdu.setOutgoingAndSend((short) 0, profileLen);
    }

    private void updateProfile(APDU apdu) {
        requirePinOrAdmin();
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        checkBlocks(len, PROFILE_MAX);
        random.generateData(scratch, (short) 0, BLOCK);
        aesCbc.init(dataKey, Cipher.MODE_ENCRYPT, scratch, (short) 0, BLOCK);
        aesCbc.doFinal(buf, ISO7816.OFFSET_CDATA, len, scratch, BLOCK);
        JCSystem.beginTransaction();
        Util.arrayCopy(scratch, (short) 0, profile, (short) 0, (short) (BLOCK + len));
        profileLen = len;
        JCSystem.commitTransaction();
    }

    // --- C9, C10 ---

    private void photoBegin(APDU apdu) {
        requirePinOrAdmin();
        byte[] buf = apdu.getBuffer();
        receive(apdu, (short) 2);
        short len = Util.getShort(buf, ISO7816.OFFSET_CDATA);
        checkBlocks(len, PHOTO_MAX);
        photoWritten = 0;
        photoLen = len;
        random.generateData(photoIv, (short) 0, BLOCK);
    }

    private void photoWrite(APDU apdu) {
        requirePinOrAdmin();
        byte[] buf = apdu.getBuffer();
        short offset = Util.getShort(buf, ISO7816.OFFSET_P1);
        if (offset != photoWritten || photoLen == 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
        short len = apdu.setIncomingAndReceive();
        checkBlocks(len, CHUNK_MAX);
        if (len > (short) (photoLen - offset)) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        // CBC continues from the last stored ciphertext block, so no cipher state survives between APDUs (§5.1).
        if (offset == 0) {
            aesCbc.init(dataKey, Cipher.MODE_ENCRYPT, photoIv, (short) 0, BLOCK);
        } else {
            aesCbc.init(dataKey, Cipher.MODE_ENCRYPT, photo, (short) (offset - BLOCK), BLOCK);
        }
        aesCbc.doFinal(buf, ISO7816.OFFSET_CDATA, len, photo, offset);
        photoWritten = (short) (offset + len);
    }

    private void photoRead(APDU apdu) {
        requirePin();
        if (!photoComplete()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        byte[] buf = apdu.getBuffer();
        short offset = Util.getShort(buf, ISO7816.OFFSET_P1);
        if (offset < 0 || offset >= photoLen || (offset % BLOCK) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
        // Fixed chunk size: jCardSim's setOutgoing() does not report the command's Le (§9, quirk 9).
        short n = (short) (photoLen - offset);
        if (n > CHUNK_MAX) {
            n = CHUNK_MAX;
        }
        if (offset == 0) {
            aesCbc.init(dataKey, Cipher.MODE_DECRYPT, photoIv, (short) 0, BLOCK);
        } else {
            aesCbc.init(dataKey, Cipher.MODE_DECRYPT, photo, (short) (offset - BLOCK), BLOCK);
        }
        aesCbc.doFinal(photo, offset, n, buf, (short) 0);
        apdu.setOutgoingAndSend((short) 0, n);
    }

    private boolean photoComplete() {
        return photoLen != 0 && photoWritten == photoLen;
    }

    // --- C7 ---

    private void authenticate(APDU apdu, boolean part2) {
        requireActive();
        byte[] buf = apdu.getBuffer();
        if (part2) {
            if (!flags[F_SIGNATURE]) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            flags[F_SIGNATURE] = false;
            Util.arrayCopyNonAtomic(scratch, HALF, buf, (short) 0, HALF);
            apdu.setOutgoingAndSend((short) 0, HALF);
            return;
        }
        if (buf[ISO7816.OFFSET_P1] != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
        receive(apdu, NONCE_LEN);

        // EMSA-PKCS1-v1_5: 00 01 FF..FF 00 ‖ DigestInfo ‖ SHA-256(cardId ‖ nonce)
        short hashOff = (short) (RSA_LEN - 32);
        short infoOff = (short) (hashOff - SHA256_DIGEST_INFO.length);
        sha256.reset();
        sha256.update(cardId, (short) 0, ID_LEN);
        sha256.doFinal(buf, ISO7816.OFFSET_CDATA, NONCE_LEN, scratch, hashOff);
        Util.arrayCopyNonAtomic(SHA256_DIGEST_INFO, (short) 0, scratch, infoOff, (short) SHA256_DIGEST_INFO.length);
        scratch[0] = 0x00;
        scratch[1] = 0x01;
        Util.arrayFillNonAtomic(scratch, (short) 2, (short) (infoOff - 3), (byte) 0xFF);
        scratch[(short) (infoOff - 1)] = 0x00;

        rsaRaw.init(rsaKeys.getPrivate(), Cipher.MODE_ENCRYPT);
        try {
            rsaRaw.doFinal(scratch, (short) 0, RSA_LEN, buf, (short) 0);
        } catch (CryptoException e) {
            // jCardSim takes the block without its leading 00 byte (§9, quirk 4); same value, same signature.
            if (e.getReason() != CryptoException.ILLEGAL_USE) {
                throw e;
            }
            rsaRaw.doFinal(scratch, (short) 1, (short) (RSA_LEN - 1), buf, (short) 0);
        }
        Util.arrayCopyNonAtomic(buf, (short) 0, scratch, (short) 0, RSA_LEN);
        flags[F_SIGNATURE] = true;
        apdu.setOutgoingAndSend((short) 0, HALF);
    }

    private void getPublicKey(APDU apdu) {
        requireActive();
        byte[] buf = apdu.getBuffer();
        RSAPublicKey pub = (RSAPublicKey) rsaKeys.getPublic();
        byte part = buf[ISO7816.OFFSET_P1];
        if (part == 2) {
            short len = pub.getExponent(buf, (short) 0);
            apdu.setOutgoingAndSend((short) 0, len);
            return;
        }
        if (part != 0 && part != 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
        // jCardSim writes a 257th leading 00 sign byte (§9, quirk 2): keep the last 256 bytes.
        short len = pub.getModulus(scratch, (short) 0);
        short start = (short) (len - RSA_LEN + part * HALF);
        Util.arrayCopyNonAtomic(scratch, start, buf, (short) 0, HALF);
        Util.arrayFillNonAtomic(scratch, (short) 0, (short) (RSA_LEN + 2), (byte) 0);
        apdu.setOutgoingAndSend((short) 0, HALF);
    }

    // --- helpers ---

    private static boolean isContactless() {
        byte media = (byte) (APDU.getProtocol() & APDU.PROTOCOL_MEDIA_MASK);
        return media == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_A || media == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_B;
    }

    private static void receive(APDU apdu, short expected) {
        if (apdu.setIncomingAndReceive() != expected) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
    }

    /** Length must be a non-zero multiple of 16; over the limit is 6A84. */
    private static void checkBlocks(short len, short max) {
        if (len <= 0 || (len % BLOCK) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        if (len > max) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
    }

    private void requireActive() {
        if (state != STATE_ACTIVE) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
    }

    private void requirePin() {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }

    private void requireAdmin() {
        if (!flags[F_ADMIN]) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }

    private void requirePinOrAdmin() {
        if (!pin.isValidated() && !flags[F_ADMIN]) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }
}
