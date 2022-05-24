
import org.bouncycastle.util.test.FixedSecureRandom;

import java.security.*;

public class RSAKeyProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RSAKeyProvider(byte[] seed){
        FixedSecureRandom random = new FixedSecureRandom(seed);

        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        generator.initialize(1024, random);
        KeyPair pair = generator.generateKeyPair();

        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    public PrivateKey getPrivateKey(){
        return privateKey;
    }

    public PublicKey getPublicKey(){
        return publicKey;
    }
}


