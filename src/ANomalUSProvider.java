

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class ANomalUSProvider {

    public byte[] encodeBytes(byte[] input, BigInteger key){
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        HashMaster hasher = new HashMaster();
        byte[] keyBytes = key.toByteArray();

        byte helpingHash = hasher.getSimpleHash(keyBytes).byteValue();
        int keyCounter = 0;

        for(int i = 0;i < input.length;++i){
            byte newByte = (byte)(input[i] ^ keyBytes[keyCounter] ^ helpingHash);

            result.write(newByte);
            ++keyCounter;

            if(keyCounter == keyBytes.length){
                keyCounter = 0;
            }
        }

        return result.toByteArray();
    }

    public byte[] decodeBytes(byte[] input, BigInteger key){
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        HashMaster hasher = new HashMaster();
        byte[] keyBytes = key.toByteArray();

        byte helpingHash = hasher.getSimpleHash(keyBytes).byteValue();
        int keyCounter = 0;

        for(int i = 0;i < input.length;++i){
            byte newByte = (byte)(input[i] ^ helpingHash ^ keyBytes[keyCounter]);

            result.write(newByte);
            ++keyCounter;

            if(keyCounter == keyBytes.length) {
                keyCounter = 0;
            }
        }

        return result.toByteArray();
    }
}
