

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Handler extends Thread{

    private final Socket socket;

    Handler(Socket socket){
        this.socket = socket;
    }

    private void sendResponse(OutputStream output, String response){
        PrintStream out = new PrintStream(output);

        out.print(response + '\n');
        out.flush();

        return;
    }

    private void handleMeet(String clientPublicKey, OutputStream output, String client){
        DFHProvider keyGenerator = new DFHProvider();
        BigInteger privateKey = keyGenerator.generatePrivateKey();
        String myPublicKey = keyGenerator.generatePublicKey(privateKey).toString();
        String sharedKey = keyGenerator.generateSharedKey(privateKey, new BigInteger(clientPublicKey)).toString();

        DatabaseHandler database = new DatabaseHandler();

        boolean operationResult = database.addConnection(client, sharedKey);

        JSONObject jsonResponse = new JSONObject();

        if(operationResult){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
            jsonResponse.put(Constants.RESPONSE_PUBLIC_DFH_KEY_HEADER, myPublicKey);
        }else{
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
        }

        sendResponse(output, jsonResponse.toJSONString());

        return;
    }

    private void handleKeepAlive(String client, OutputStream output){
        DatabaseHandler database = new DatabaseHandler();

        boolean operationResult = database.updateConnection(client);
        JSONObject jsonResponse = new JSONObject();

        if(operationResult){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
        }else{
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
        }

        sendResponse(output, jsonResponse.toJSONString());
        return;
    }

    private void handleJoinUs(String client, OutputStream output, String name, String publicKey){



    }

    private JSONObject getDataFromClient(Object encodedDataString, String sharedKey) throws ParseException {

        ANomalUSProvider anomalus = new ANomalUSProvider();
        JSONParser parser = new JSONParser();
        Sanitizer sanitizer = new Sanitizer();

        byte[] encodedBytes = sanitizer.unSanitize((String)encodedDataString);

        String result = new String(anomalus.decodeBytes(encodedBytes, new BigInteger(sharedKey)));

        System.out.println(result);


        return (JSONObject) parser.parse(result);
    }

    @Override
    public void run() {
        try(InputStream input = this.socket.getInputStream(); OutputStream output = this.socket.getOutputStream()){

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String message = reader.readLine();
            JSONParser parser = new JSONParser();
            JSONObject requestJson = (JSONObject) parser.parse(message);

            String requestType = (String)requestJson.get("request-type");
            Object requestData = requestJson.get("data");

            String clientIp = ((InetSocketAddress)this.socket.getRemoteSocketAddress()).getAddress().getHostAddress();

            if(requestType == null || (!requestType.contentEquals("keep_alive") && requestData == null)){
                System.out.println("PARSED NULL REQUEST OR DATA FROM " + clientIp);
                return;
            }

            switch(requestType){

                case ("meet"):
                    handleMeet((String)((JSONObject)requestData).get("publicKey"), output, clientIp);
                    input.close();
                    output.close();
                    this.socket.close();
                    Log.write("HANDLED MEET REQUEST FROM " + clientIp);
                    break;
                case ("keep_alive"):
                    handleKeepAlive(clientIp, output);
                    input.close();
                    output.close();
                    this.socket.close();
                    Log.write("HANDLED KEEP_ALIVE REQUEST FROM " + clientIp);
                    break;
                case ("auth"):
                    // TODO: auth-request
                    break;
                case ("twisted"):
                    // TODO: twisted-request
                    break;
                case ("join_us"):
                    // TODO: join_us-request
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    System.out.println("SHARED KEY IS " + sharedKey);

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    System.out.println(dataFromClient.get("publicKey"));

                    break;
                case ("hypnotize"):
                    // TODO: hypnotize-request
                    break;
                case ("send"):
                    // TODO: send-request
                    break;
                case("check_mail"):
                    // TODO: check_mail
                    break;
                default:
                    // TODO: default
                    break;
            }

        }catch(IOException e){
            System.out.println("INPUT-OUTPUT EXCEPTION WHILE READING REQUEST");
            e.printStackTrace();

        }catch (ParseException e) {
            System.out.println("EXCEPTION WHILE PARSING JSON FROM REQUEST");
            e.printStackTrace();
        }
    }
}
