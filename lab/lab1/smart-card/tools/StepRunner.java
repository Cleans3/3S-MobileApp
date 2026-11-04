import com.licel.jcardsim.smartcardio.JCardSimProvider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 * Runs an apdutool .scr script on jCardSim and explains every step.
 * The comment above a command describes it; text after "->" is the expected reply.
 * With -i it stops after SELECT and lets you type your own APDUs.
 * Usage: java -cp jcardsim.jar:.build StepRunner jcardsim.cfg apdu/bai1.scr [-i]
 */
public class StepRunner {

    record Step(String note, String text) {}

    static final Map<Integer, String> SW = Map.of(
            0x9000, "success",
            0x6700, "wrong length: the data length does not match what the applet expects",
            0x6985, "conditions of use not satisfied: nothing has been stored yet",
            0x6A80, "wrong data: the command data has the wrong format",
            0x6A86, "incorrect P1/P2",
            0x6B00, "wrong parameters P1/P2 (for example, dividing by zero)",
            0x6D00, "instruction (INS) not supported by this applet",
            0x6E00, "class (CLA) not supported",
            0x6999, "applet selection failed: the applet is not installed",
            0x6F00, "unknown error inside the applet");

    static Map<String, String> classByAid = new HashMap<>();

    public static void main(String[] args) throws Exception {
        boolean interactive = args.length > 2 && args[2].equals("-i");
        Properties cfg = new Properties();
        try (var in = Files.newInputStream(Path.of(args[0]))) {
            cfg.load(in);
        }
        for (String key : cfg.stringPropertyNames()) {
            System.setProperty(key, cfg.getProperty(key));
            if (key.endsWith(".AID")) {
                classByAid.put(cfg.getProperty(key).toUpperCase(), cfg.getProperty(key.replace(".AID", ".Class")));
            }
        }
        Security.addProvider(new JCardSimProvider());

        String title = null;
        List<String> notes = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        StringBuilder cmd = new StringBuilder();
        for (String raw : Files.readAllLines(Path.of(args[1]))) {
            String line = raw.trim();
            if (line.startsWith("//")) {
                String c = line.substring(2).trim();
                if (title == null) {
                    title = c;
                } else {
                    notes.add(c);
                }
            } else if (!line.isEmpty()) {
                cmd.append(' ').append(line);
                if (line.endsWith(";")) {
                    steps.add(new Step(String.join(" ", notes), cmd.toString().replace(";", "").trim()));
                    notes.clear();
                    cmd.setLength(0);
                }
            }
        }

        // Interactive mode runs the setup only: power up, install, select.
        int end = steps.size();
        if (interactive) {
            for (int i = 0; i < steps.size(); i++) {
                CommandAPDU c = parse(steps.get(i).text());
                if (c != null && c.getCLA() == 0x00 && c.getINS() == 0xA4) {
                    end = i + 1;
                    break;
                }
            }
        }

        System.out.println();
        System.out.println("=== " + title + " ===");
        Card card = null;
        CardChannel channel = null;
        for (int i = 0; i < end; i++) {
            Step s = steps.get(i);
            System.out.println();
            String head = "Step " + (i + 1) + "/" + end + ": ";
            if (s.text().equalsIgnoreCase("powerup")) {
                System.out.println(head + "power up the card");
                card = TerminalFactory.getInstance("jCardSim", null).terminals().list().get(0).connect("T=0");
                channel = card.getBasicChannel();
                System.out.println("  The reader powers and resets the card. The card answers with its ATR:");
                System.out.println("  ATR     : " + hex(card.getATR().getBytes()));
            } else if (s.text().equalsIgnoreCase("powerdown")) {
                powerDown(head, card);
            } else {
                String what = s.note(), expected = null;
                int arrow = what.indexOf("->");
                if (arrow >= 0) {
                    expected = what.substring(arrow + 2).trim();
                    what = what.substring(0, arrow).trim();
                }
                runApdu(head + what, expected, parse(s.text()), channel);
            }
        }

        if (interactive) {
            Step example = end < steps.size() && parse(steps.get(end).text()) != null ? steps.get(end) : null;
            interact(channel, example);
            System.out.println();
            powerDown("End: ", card);
        }
        System.out.println();
    }

    static void interact(CardChannel channel, Step example) throws Exception {
        System.out.println();
        System.out.println("Interactive mode: the applet is selected, now type your own command APDUs.");
        System.out.println("  Format : CLA INS P1 P2 [Lc data...] [Le], every byte in hex (spaces optional)");
        System.out.println("           A \"/send\" prefix, as in the lab guide, is accepted too.");
        if (example != null) {
            String note = example.note().split("->")[0].trim();
            System.out.println("  Example: " + hex(parse(example.text()).getBytes()) + "   (" + note + ")");
        }
        System.out.println("  Type exit, or press Ctrl+D, to power down and quit.");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int n = 0;
        while (true) {
            System.out.print("\n>> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null || line.trim().equalsIgnoreCase("exit")) {
                break;
            }
            String h = line.trim().replaceFirst("^/send\\s*", "").replaceAll("\\s+", "");
            if (h.isEmpty()) {
                continue;
            }
            CommandAPDU c;
            try {
                c = new CommandAPDU(HexFormat.of().parseHex(h));
            } catch (IllegalArgumentException e) {
                System.out.println("  Not a valid APDU (" + e.getMessage() + "). Use hex bytes: CLA INS P1 P2 [Lc data...] [Le]");
                continue;
            }
            try {
                runApdu("Command " + (++n) + ": typed by you", null, c, channel);
            } catch (Exception e) {
                System.out.println("  The simulator rejected it: " + e);
            }
        }
    }

    static void powerDown(String head, Card card) throws Exception {
        System.out.println(head + "power down the card");
        card.disconnect(false);
        System.out.println("  RAM (transient) data is lost; EEPROM (persistent) data stays on the card.");
    }

    /** Script format "CLA INS P1 P2 Lc [data] Le" to an APDU; null for powerup/powerdown. */
    static CommandAPDU parse(String text) {
        String[] words = text.split("\\s+");
        if (words.length < 6) {
            return null;
        }
        int[] b = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            b[i] = Integer.decode(words[i]);
        }
        byte[] data = new byte[b[4]];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) b[5 + i];
        }
        int le = b[b.length - 1];
        return data.length == 0 ? new CommandAPDU(b[0], b[1], b[2], b[3], le) : new CommandAPDU(b[0], b[1], b[2], b[3], data, le);
    }

    static void runApdu(String title, String expected, CommandAPDU c, CardChannel channel) throws Exception {
        int cla = c.getCLA(), ins = c.getINS();
        byte[] data = c.getData();
        System.out.println(title);

        if (cla == 0x80 && ins == 0xB8 && data.length > 0) {
            String aid = hex(data, 1, data[0] & 0xFF).replace(" ", "");
            System.out.println("  CREATE APPLET: the simulator creates " + classByAid.getOrDefault(aid, "?")
                    + " by calling its install() method, which calls register() with AID " + aid + ".");
        } else if (cla == 0x00 && ins == 0xA4) {
            System.out.println("  SELECT by AID: the applet becomes active, so the next commands go to its process() method.");
        } else {
            System.out.println("  Sent to the selected applet's process() method.");
        }

        System.out.println("  Command : " + hex(c.getBytes()));
        System.out.printf("            CLA %02X (%s), INS %02X, P1 %02X, P2 %02X, Lc %d, Le %s%n",
                cla, cla == 0x00 ? "ISO 7816 standard" : "proprietary, handled by the applet",
                ins, c.getP1(), c.getP2(), data.length,
                c.getNe() == 0 ? "none" : c.getNe() + " (max reply length)");
        if (data.length > 0) {
            System.out.println("  Data    : " + hex(data) + "  (" + data.length + " bytes)" + decode(data));
        }

        ResponseAPDU r = channel.transmit(c);
        byte[] reply = r.getData();
        System.out.println("  Reply   : " + (reply.length == 0 ? "(no data)" : hex(reply) + "  (" + reply.length + " bytes)" + decode(reply)));
        System.out.printf("  Status  : %02X %02X = %s%n", r.getSW1(), r.getSW2(), SW.getOrDefault(r.getSW(), "see ISO 7816-4"));
        if (expected != null) {
            System.out.println("  Expected: " + expected);
        }
    }

    /** Printable ASCII is shown as text; a short binary value as an unsigned number. */
    static String decode(byte[] d) {
        boolean text = true;
        for (byte x : d) {
            text &= x >= 0x20 && x < 0x7F;
        }
        if (text) {
            return "\n            as text: \"" + new String(d, StandardCharsets.US_ASCII) + "\"";
        }
        if (d.length <= 4) {
            long n = 0;
            for (byte x : d) {
                n = (n << 8) | (x & 0xFF);
            }
            String result = "\n            as number: " + n;
            if (d.length == 2 && d[0] < 0) {
                result += " (as a signed short: " + (short) n + ")";
            }
            return result;
        }
        return "";
    }

    static String hex(byte[] d) {
        return hex(d, 0, d.length);
    }

    static String hex(byte[] d, int off, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < off + len; i++) {
            sb.append(String.format("%02X ", d[i]));
        }
        return sb.toString().trim();
    }
}
