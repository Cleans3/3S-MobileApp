package shelftap.server.card;

import org.springframework.stereotype.Component;
import shelftap.server.config.ShelfTapProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.HexFormat;

/** The two cryptographic duties of the card contract (card-spec §2): admin-key derivation and C7 signature check. */
@Component
public class CardCrypto {
    private final byte[] master;

    CardCrypto(ShelfTapProperties props) {
        this.master = props.master().keyBytes();
    }

    /** K_admin = first 16 bytes of HMAC-SHA256(K_master, "ShelfTap-admin" || cardId). Derived on demand, never stored. */
    public byte[] adminKey(String cardId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(master, "HmacSHA256"));
            mac.update("ShelfTap-admin".getBytes(StandardCharsets.US_ASCII));
            mac.update(cardIdBytes(cardId));
            return Arrays.copyOf(mac.doFinal(), 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] cardIdBytes(String cardId) {
        return HexFormat.of().parseHex(cardId);
    }

    /** SHA256withRSA over cardId (8 raw bytes) || nonce (32 bytes), exactly what the applet signs. Any failure is a plain false. */
    public boolean verify(Card card, byte[] nonce, byte[] signature) {
        if (card.publicKeyModulus == null || card.publicKeyExponent == null) {
            return false;
        }
        try {
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    new BigInteger(1, card.publicKeyModulus), new BigInteger(1, card.publicKeyExponent)));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(cardIdBytes(card.cardId));
            verifier.update(nonce);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
