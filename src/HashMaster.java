

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashMaster {

    private final int PRIME_MULT = 1717;

    private static String bytesToHex(byte[] hash){
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for(int i = 0;i < hash.length;++i) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getHashSha256(byte[] input){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256"); // exception will never be thrown
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] byteHash = digest.digest(input);

        return bytesToHex(byteHash);
    }

    public Integer getSimpleHash(byte[] input){

        Integer result = 0;

        for(int i = 0;i < input.length;++i){
            result = (result + ((input[i] * PRIME_MULT) << 3) % 17171717) % 17171717;
        }

        return result;
    }

}
