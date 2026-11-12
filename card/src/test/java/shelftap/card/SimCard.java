package shelftap.card;

import com.licel.jcardsim.base.Simulator;
import javacard.framework.AID;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/** One freshly installed ShelfTap applet on jCardSim, spoken to in raw APDUs as a host would. */
final class SimCard {
    static final byte[] AID_BYTES = {(byte) 0xF0, 0x53, 0x48, 0x45, 0x4C, 0x46, 0x54, 0x41, 0x50, 0x01};

    private final Simulator sim = new Simulator();
    private final AID aid = new AID(AID_BYTES, (short) 0, (byte) AID_BYTES.length);

    SimCard() {
        // Install parameters as GlobalPlatform passes them: AID length, AID, empty privileges, empty applet data.
        byte[] params = new byte[AID_BYTES.length + 3];
        params[0] = (byte) AID_BYTES.length;
        System.arraycopy(AID_BYTES, 0, params, 1, AID_BYTES.length);
        sim.installApplet(aid, LibraryCardApplet.class, params, (short) 0, (byte) params.length);
        select();
    }

    /** Re-select: ends the session the way pulling and re-inserting the card does. */
    void select() {
        if (!sim.selectApplet(aid)) {
            throw new IllegalStateException("applet did not select");
        }
    }

    ResponseAPDU send(int ins, int p1, int p2, byte[] data) {
        return transmit(new CommandAPDU(0x80, ins, p1, p2, data));
    }

    ResponseAPDU get(int ins, int p1, int p2, int le) {
        return transmit(new CommandAPDU(0x80, ins, p1, p2, le));
    }

    ResponseAPDU sendGet(int ins, int p1, int p2, byte[] data, int le) {
        return transmit(new CommandAPDU(0x80, ins, p1, p2, data, le));
    }

    ResponseAPDU transmit(CommandAPDU command) {
        return new ResponseAPDU(sim.transmitCommand(command.getBytes()));
    }
}
