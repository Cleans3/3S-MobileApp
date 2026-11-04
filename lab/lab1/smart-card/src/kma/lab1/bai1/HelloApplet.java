package kma.lab1.bai1;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

/** Bai 1: returns "Xin chao, KMA". */
public class HelloApplet extends Applet {

    private static final byte INS_HELLO = (byte) 0x01;

    private static final byte[] HELLO = {
        'X', 'i', 'n', ' ', 'c', 'h', 'a', 'o', ',', ' ', 'K', 'M', 'A'
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HelloApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        if (buf[ISO7816.OFFSET_INS] != INS_HELLO) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) HELLO.length);
        apdu.sendBytesLong(HELLO, (short) 0, (short) HELLO.length);
    }
}
