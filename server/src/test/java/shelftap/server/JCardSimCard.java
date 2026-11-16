package shelftap.server;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The real ShelfTap applet in jCardSim, driven with the host-side APDUs of smart-card/card-spec.md §5.
 * jCardSim and the applet load in a private class loader (parent: the JDK only) because jCardSim bundles an old
 * BouncyCastle that would shadow the one the server's Argon2 needs.
 */
final class JCardSimCard {
    private static final byte[] AID_BYTES = {(byte) 0xF0, 0x53, 0x48, 0x45, 0x4C, 0x46, 0x54, 0x41, 0x50, 0x01};
    private static final byte[] PIN = "482916".getBytes(StandardCharsets.US_ASCII);
    private static final ClassLoader ISOLATED = isolatedLoader();

    private final Object simulator;
    private final Method transmit;

    /** Installs the applet (GlobalPlatform-style install parameters, card-spec §9 quirk 10) and issues it (C1). */
    JCardSimCard(String cardId, byte[] adminKey) {
        try {
            Class<?> simulatorClass = ISOLATED.loadClass("com.licel.jcardsim.base.Simulator");
            Class<?> aidClass = ISOLATED.loadClass("javacard.framework.AID");
            Class<?> appletClass = ISOLATED.loadClass("shelftap.card.LibraryCardApplet");
            simulator = simulatorClass.getConstructor().newInstance();
            Constructor<?> aidConstructor = aidClass.getConstructor(byte[].class, short.class, byte.class);
            Object aid = aidConstructor.newInstance(AID_BYTES, (short) 0, (byte) AID_BYTES.length);

            byte[] params = new byte[AID_BYTES.length + 3];
            params[0] = (byte) AID_BYTES.length;
            System.arraycopy(AID_BYTES, 0, params, 1, AID_BYTES.length);
            simulatorClass.getMethod("installApplet", aidClass, Class.class, byte[].class, short.class, byte.class)
                    .invoke(simulator, aid, appletClass, params, (short) 0, (byte) params.length);
            boolean selected = (boolean) simulatorClass.getMethod("selectApplet", aidClass).invoke(simulator, aid);
            if (!selected) {
                throw new IllegalStateException("applet did not select");
            }
            transmit = simulatorClass.getMethod("transmitCommand", byte[].class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        ByteArrayOutputStream issue = new ByteArrayOutputStream();
        issue.writeBytes(HexFormat.of().parseHex(cardId));
        issue.writeBytes(PIN);
        issue.writeBytes(adminKey);
        ok(send(new CommandAPDU(0x80, 0x10, 0, 0, issue.toByteArray())));
    }

    /** GET PUBLIC KEY x3: 256-byte modulus and the exponent, exactly as the desktop reads them at issue. */
    byte[][] publicKey() {
        byte[] modulus = concat(ok(send(new CommandAPDU(0x80, 0x62, 0, 0, 128))), ok(send(new CommandAPDU(0x80, 0x62, 1, 0, 128))));
        byte[] exponent = ok(send(new CommandAPDU(0x80, 0x62, 2, 0, 3)));
        return new byte[][]{modulus, exponent};
    }

    /** C7 AUTHENTICATE, both halves. */
    byte[] sign(byte[] nonce) {
        return concat(ok(send(new CommandAPDU(0x80, 0x60, 0, 0, nonce, 128))), ok(send(new CommandAPDU(0x80, 0x60, 1, 0, 128))));
    }

    private ResponseAPDU send(CommandAPDU command) {
        try {
            return new ResponseAPDU((byte[]) transmit.invoke(simulator, (Object) command.getBytes()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] ok(ResponseAPDU response) {
        assertEquals(0x9000, response.getSW());
        return response.getData();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static ClassLoader isolatedLoader() {
        try {
            String[] entries = System.getProperty("shelftap.cardsim.classpath").split(File.pathSeparator);
            URL[] urls = new URL[entries.length];
            for (int i = 0; i < entries.length; i++) {
                urls[i] = new File(entries[i]).toURI().toURL();
            }
            return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
        } catch (java.net.MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
