import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class GenerateVapidKeys {
    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String publicKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Utils.encode((ECPublicKey) keyPair.getPublic()));
        String privateKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Utils.encode((ECPrivateKey) keyPair.getPrivate()));

        System.out.println("TAI_VAPID_PUBLIC_KEY=" + publicKey);
        System.out.println("TAI_VAPID_PRIVATE_KEY=" + privateKey);
    }
}
