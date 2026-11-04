package kma.lab1.bai4;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 * Bai 4: stores one string "studentId|fullName|dateOfBirth|hometown" and returns
 * each field on its own or the whole string.
 */
public class StudentApplet extends Applet {

    private static final byte INS_STORE = (byte) 0x01;
    private static final byte INS_GET_FIELD = (byte) 0x02; // P1 = 1 id, 2 name, 3 date of birth, 4 hometown
    private static final byte INS_GET_ALL = (byte) 0x03;

    private static final byte SEP = (byte) '|';
    private static final byte FIELDS = 4;
    private static final short MAX_LEN = 255;

    private final byte[] data = new byte[MAX_LEN];
    private short dataLen;
    private final byte[] scratch = JCSystem.makeTransientByteArray(MAX_LEN, JCSystem.CLEAR_ON_DESELECT);

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new StudentApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
            case INS_STORE:
                store(apdu, buf);
                break;
            case INS_GET_FIELD:
                sendField(apdu, buf[ISO7816.OFFSET_P1]);
                break;
            case INS_GET_ALL:
                requireData();
                send(apdu, (short) 0, dataLen);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void store(APDU apdu, byte[] buf) {
        short total = (short) (buf[ISO7816.OFFSET_LC] & 0xFF);
        short pos = 0;
        short got = apdu.setIncomingAndReceive();
        while (got > 0) {
            Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, scratch, pos, got);
            pos += got;
            got = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }
        if (pos != total) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        short seps = 0;
        for (short i = 0; i < pos; i++) {
            if (scratch[i] == SEP) {
                seps++;
            }
        }
        if (seps != FIELDS - 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }

        // Data and length change together, so a card pulled mid-write keeps the old record.
        JCSystem.beginTransaction();
        Util.arrayCopy(scratch, (short) 0, data, (short) 0, pos);
        dataLen = pos;
        JCSystem.commitTransaction();
    }

    private void sendField(APDU apdu, byte field) {
        if (field < 1 || field > FIELDS) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        requireData();
        short start = 0;
        byte n = 1;
        for (short i = 0; i < dataLen; i++) {
            if (data[i] == SEP) {
                if (n == field) {
                    send(apdu, start, (short) (i - start));
                    return;
                }
                n++;
                start = (short) (i + 1);
            }
        }
        send(apdu, start, (short) (dataLen - start)); // last field
    }

    private void requireData() {
        if (dataLen == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
    }

    private void send(APDU apdu, short off, short len) {
        apdu.setOutgoing();
        apdu.setOutgoingLength(len);
        apdu.sendBytesLong(data, off, len);
    }
}
