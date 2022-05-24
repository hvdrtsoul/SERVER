import javax.crypto.spec.SecretKeySpec;

public class AESKeyProvider {

    AESKeyProvider(byte[] seed){
        SecretKeySpec secretKey = new SecretKeySpec(seed, "AES");

    }

}
