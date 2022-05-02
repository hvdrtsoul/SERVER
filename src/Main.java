

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

public class Main {

    public static void main1(String[] args) {

        RSAKeyProvider keyProvider = new RSAKeyProvider();
        PublicKey publicKey = keyProvider.getPublicKey();

        Sanitizer sanitizer = new Sanitizer();
        System.out.println(sanitizer.sanitize(publicKey.getEncoded()));
        System.out.println(sanitizer.sanitize(keyProvider.getPrivateKey().getEncoded()));

    }

    public static void main(String[] args) {
        OUTSiDE server = new OUTSiDE(9674);

        server.start();
    }

    
}
