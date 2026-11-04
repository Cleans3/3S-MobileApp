package kma.lab1.bai2;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

/** Bai 2: returns a fixed full name, date of birth, or both. */
public class ProfileApplet extends Applet {

    private static final byte INS_NAME = (byte) 0x01;
    private static final byte INS_DOB = (byte) 0x02;
    private static final byte INS_NAME_DOB = (byte) 0x03;

    // Fixed values required by the exercise.
    private static final byte[] NAME = {
        'T', 'r', 'a', 'n', ' ', 'H', 'o', 'a', 'n', 'g', ' ', 'D', 'u', 'n', 'g'
    };
    private static final byte[] DOB = {
        '3', '6', '/', '6', '7', '/', '4', '2', '0', '0'
    };
    private static final byte[] SEP = { ' ', '-', ' ' };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new ProfileApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        switch (apdu.getBuffer()[ISO7816.OFFSET_INS]) {
            case INS_NAME:
                apdu.setOutgoing();
                apdu.setOutgoingLength((short) NAME.length);
                apdu.sendBytesLong(NAME, (short) 0, (short) NAME.length);
                break;
            case INS_DOB:
                apdu.setOutgoing();
                apdu.setOutgoingLength((short) DOB.length);
                apdu.sendBytesLong(DOB, (short) 0, (short) DOB.length);
                break;
            case INS_NAME_DOB:
                apdu.setOutgoing();
                apdu.setOutgoingLength((short) (NAME.length + SEP.length + DOB.length));
                apdu.sendBytesLong(NAME, (short) 0, (short) NAME.length);
                apdu.sendBytesLong(SEP, (short) 0, (short) SEP.length);
                apdu.sendBytesLong(DOB, (short) 0, (short) DOB.length);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
