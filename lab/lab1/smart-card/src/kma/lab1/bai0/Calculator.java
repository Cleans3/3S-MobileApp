package kma.lab1.bai0;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * Bai 0: the pocket calculator from the lab guide (section 1.5).
 * CLA A0; INS 00 add, 01 subtract, 02 multiply, 03 divide; operands are P1 and P2 (signed bytes).
 * The result comes back as a 2-byte signed short.
 */
public class Calculator extends Applet {

    static final byte CALCULATOR_CLA = (byte) 0xA0;

    static final byte INS_CONG = (byte) 0x00; // add
    static final byte INS_TRU = (byte) 0x01;  // subtract
    static final byte INS_NHAN = (byte) 0x02; // multiply
    static final byte INS_CHIA = (byte) 0x03; // divide

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Calculator().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        // The operands travel in P1/P2, so there is no command data to receive.
        // The guide calls setIncomingAndReceive() here; it is not needed for these commands.
        byte[] buf = apdu.getBuffer();

        if (buf[ISO7816.OFFSET_CLA] != CALCULATOR_CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        short a = buf[ISO7816.OFFSET_P1];
        short b = buf[ISO7816.OFFSET_P2];
        short res = 0;
        switch (buf[ISO7816.OFFSET_INS]) {
            case INS_CONG:
                res = (short) (a + b);
                break;
            case INS_TRU:
                res = (short) (a - b);
                break;
            case INS_NHAN:
                res = (short) (a * b);
                break;
            case INS_CHIA:
                // Not in the guide: without this, dividing by zero crashes the applet (6F 00).
                if (b == 0) {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                res = (short) (a / b);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        Util.setShort(buf, (short) 0, res);

        // The three sending steps from section 1.3 of the guide.
        // A short reply like this one could use apdu.setOutgoingAndSend((short) 0, (short) 2) instead.
        short le = apdu.setOutgoing();          // 1: switch to sending; Le = bytes the host expects
        if (le < (short) 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        apdu.setOutgoingLength((short) 2);      // 2: tell the host how many bytes follow
        apdu.sendBytes((short) 0, (short) 2);   // 3: send them from the APDU buffer
    }
}
