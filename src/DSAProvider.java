import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class DSAProvider {

    private PrivateKey getPrivateKey(String sanitizedKey){
        PrivateKey privateKey = null;
        try{
            Sanitizer sanitizer = new Sanitizer();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(sanitizer.unSanitize(sanitizedKey));


            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public PublicKey getPublicKey(String sanitizedKey){
        PublicKey publicKey = null;
        try{
            Sanitizer sanitizer = new Sanitizer();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(sanitizer.unSanitize(sanitizedKey));

            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public String sign(String message, String sanitizedPrivateKey){
        Signature signature = null;
        try {
            signature = Signature.getInstance("DSA");
            signature.initSign(getPrivateKey(sanitizedPrivateKey));
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] result = signature.sign();

            Sanitizer sanitizer = new Sanitizer();
            return sanitizer.sanitize(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return "undefined";
    }

    public boolean verify(String message, String sanitizedSignature, String sanitizedPublicKey){
        try{
            Sanitizer sanitizer = new Sanitizer();
            Signature signature = Signature.getInstance("DSA");
            signature.initVerify(getPublicKey(sanitizedPublicKey));
            signature.update(message.getBytes(StandardCharsets.UTF_8));

            return signature.verify(sanitizer.unSanitize(sanitizedSignature));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }
}
