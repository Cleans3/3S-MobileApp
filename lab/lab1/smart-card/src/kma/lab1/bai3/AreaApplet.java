package kma.lab1.bai3;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * Bai 3: area of a square (data = side) or a rectangle (data = length, width).
 * Each side is one unsigned byte (0..255); the area comes back as 2 bytes, unsigned big-endian.
 */
public class AreaApplet extends Applet {

    private static final byte INS_SQUARE = (byte) 0x01;
    private static final byte INS_RECTANGLE = (byte) 0x02;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AreaApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        byte ins = buf[ISO7816.OFFSET_INS];

        short expected;
        if (ins == INS_SQUARE) {
            expected = 1;
        } else if (ins == INS_RECTANGLE) {
            expected = 2;
        } else {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            return;
        }
        if (apdu.setIncomingAndReceive() != expected) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // ponytail: 1-byte sides keep the area under 65536 so plain short math is exact;
        // larger shapes need 2-byte sides and a 4-byte result.
        short a = (short) (buf[ISO7816.OFFSET_CDATA] & 0xFF);
        short b = (ins == INS_SQUARE) ? a : (short) (buf[ISO7816.OFFSET_CDATA + 1] & 0xFF);
        Util.setShort(buf, (short) 0, (short) (a * b));
        apdu.setOutgoingAndSend((short) 0, (short) 2);
    }
}
