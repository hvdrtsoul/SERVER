

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main1(String[] args) {

        UserNameHandler userNameHandler = new UserNameHandler();
        System.out.println(userNameHandler.isCorrectNickname("Aarosha123"));
    }

    public static void main(String[] args) {
        OUTSiDE server = new OUTSiDE(9674);

        server.start();
    }

    
}
