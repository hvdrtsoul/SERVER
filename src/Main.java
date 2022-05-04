

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

public class Main {

    public static void main(String[] args) {

        ANomalUSProvider anomalus = new ANomalUSProvider();

        DFHProvider dfhProvider = new DFHProvider();
        BigInteger key = dfhProvider.generateSharedKey(dfhProvider.generatePrivateKey(), dfhProvider.generatePublicKey(dfhProvider.generatePrivateKey()));

        String message = "АНАНАС"; // len = 12

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        byte[] encodedBytes = anomalus.encodeBytes(messageBytes, key);

        System.out.println("STOPPED");

    }

    public static void main1(String[] args) {
        OUTSiDE server = new OUTSiDE(9674);

        server.start();
    }

    
}
