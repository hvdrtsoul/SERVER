

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.util.ArrayList;

public class Main {

    public static void main1(String[] args) {

        ANomalUSProvider anomalus = new ANomalUSProvider();

        DFHProvider dfhProvider = new DFHProvider();
        BigInteger key = dfhProvider.generateSharedKey(dfhProvider.generatePrivateKey(), dfhProvider.generatePublicKey(dfhProvider.generatePrivateKey()));

        String message = "АНАНАС"; // len = 12

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        byte[] encodedBytes = anomalus.encodeBytes(messageBytes, key);

        System.out.println("STOPPED");

    }

    public static void main(String[] args) {
        OUTSiDE server = new OUTSiDE(9674);

        server.start();
    }

    public static void main9(String[] args) {
        SecureRandom secureRandom = new SecureRandom("ABC".getBytes(StandardCharsets.UTF_8));

        System.out.println(secureRandom.nextInt());
    }

    public static void main0(String[] args) {
       String secret = "ABCD";
        Sanitizer sanitizer = new Sanitizer();

        byte[] seed = sanitizer.unSanitize("lZkHc4hQaqcmkKs=");
        RSAKeyProvider rsaKeyProvider = new RSAKeyProvider(seed);


        PublicKey publicKey = rsaKeyProvider.getPublicKey();
        PrivateKey privateKey = rsaKeyProvider.getPrivateKey();

        System.out.println(sanitizer.sanitize(privateKey.getEncoded()));

        RSAProvider rsaProvider = new RSAProvider();


        byte[] encodedBytes = sanitizer.unSanitize("piL8YM5cC8PTu09IRUIsriQ+wTdq+S8lHyQuT5HaBEtaZBsy/DmAbNLv28Ey8F9U94CCKiVlYwobK15Sw+xFIbsDDckoQwphNCLNCxn/LorApyQE4tiMDRi5aEQJ/s+sFhazxUM+cTx671adOHBgkD9jvuFIMBaS7pxbWCrBpPU=");

        byte[] decrypt = rsaProvider.decrypt(encodedBytes, "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKrhdorenbCKJ+xAkm6aHrr76vRFIf8rDiphGhceMiQZB/4YIC5SjU2M5RtylzR70Cur+jk7kako/IQqHndQOKNbG5G8tMii6PN+1en0jcKOZZjiD883x7+1ntOc+BwAyZ5+qfkTSrmjCquk4m5Oy15s1R+Kajw7gYpm1u1jSWqVAgMBAAECgYAeL7qScR2PrnQ7gLOtfKXhl7eLg7vr+PXlaCZk/5VReaeHDEmBDV2/XZyqct2BrL6bPGP1YM/MP3yON2eO4bnDXbVD33pNnTYss9lIx/DcCB+G+TMYQS1h3nZchNcCkbgegH2rx0+iA1G5edWjPlrscEw4R8sk42PXGL3FUiArAQJBAOA+TxoTEPmPsXuycjtSaZix6Q5HHe+F1SNY95XM0wUI8VpNyxkPwHvj5b1hzmaxSaCM4nVR5Pm7sOtDpNpEi5UCQQDDFIzmUyDPlHzWCQNO5s010LfaxRsVcLi3j2wOyUCIC7LWzBhfxavVK5ZHUArH/yGAjKnaSJHBHNs45gVSUaMBAkEAsFZPPeHgZ19n4i3hRmT4ROsiqeei0sgbY0CC8XmwNzVhFfI20+5fHw2Hi/VtqbOggnBHZJqRxRCjf14iNMHSQQJBAKWuEmybNUaXKd7uEK02QBrwhPV8aaAv9/GOEYwNSI1CwkTDWgAc9e858YvnYwCwold3H9qPv5pvU5BjaymaxAECQCqp/KXYZUTP3WVwkW1+L5jovHk8tZNNpU9E6nB60zshM1DJSYcYSOJbUGCDDfwWyO0xo5255X4PJf7yI00HV5w=");

        System.out.println(new String(decrypt));


    }

    
}
