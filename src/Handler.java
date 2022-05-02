

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

    private boolean handleJoinUs(OutputStream output, String name, String publicKey){

        DatabaseHandler database = new DatabaseHandler();
        UserNameHandler userNameHandler = new UserNameHandler();
        JSONObject jsonResponse = new JSONObject();

        boolean userExist = database.userExists(name);

        if(userExist){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.JOIN_US_USER_EXISTS);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!userNameHandler.isCorrect(name)){
            // only way to be there - username is incorrect (we check if it exists before)
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.JOIN_US_INCORRECT_NAME);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        String result = database.addUser(name, publicKey);

        if(result == Constants.JOIN_US_ERROR){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
        else{ // we've got the session
            database.updateLastActive(name);
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
            jsonResponse.put(Constants.SESSION_HEADER, result);
            sendResponse(output, jsonResponse.toJSONString());
            return true;
        }
    }

    private boolean handleHypnotize(OutputStream output, String userName, String nickName, String session){
        SessionHandler sessionHandler = new SessionHandler();
        DatabaseHandler database = new DatabaseHandler();
        UserNameHandler userNameHandler = new UserNameHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.userExists(userName)){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.HYPNOTIZE_USER_DOES_NOT_EXIST);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!sessionHandler.checkAuth(session, userName)){ // if not authorized
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!userNameHandler.isCorrectNickname(nickName)){ // if nickname is incorrect
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.HYPNOTIZE_INCORRECT_NICKNAME);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(database.userClaimedNickname(userName)){ // if user already claimed a nickname once
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.HYPNOTIZE_USER_ALREADY_TAKEN_NICKNAME);
            sendResponse(output, jsonResponse.toJSONString());
            return false;

        }

        if(database.nicknameExists(nickName)){ // if nickname is already taken
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.HYPNOTIZE_NICKNAME_TAKEN);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        // otherwise we add a nickname

        if(database.addNickName(nickName, userName)){ // if we successfully added a nickname
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
            sendResponse(output, jsonResponse.toJSONString());
            return true;
        }
        else // if something went wrong while adding a nickname
        {
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
    }

    private void handleNotConnected(OutputStream output){
        JSONObject jsonResponse = new JSONObject();

        jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
        jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.NOT_CONNECTED_MESSAGE);
        sendResponse(output, jsonResponse.toJSONString());
    }

    private boolean handleAuth(OutputStream output, String userName){

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

                case ("meet"): {
                    handleMeet((String) ((JSONObject) requestData).get("publicKey"), output, clientIp);
                    input.close();
                    output.close();
                    this.socket.close();
                    Log.write("HANDLED MEET REQUEST FROM " + clientIp);
                    break;
                }
                case ("keep_alive"): {
                    handleKeepAlive(clientIp, output);
                    input.close();
                    output.close();
                    this.socket.close();
                    Log.write("HANDLED KEEP_ALIVE REQUEST FROM " + clientIp);
                    break;
                }
                case ("auth"): {
                    // TODO: auth-request
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);
                    String clientUserName = (String) dataFromClient.get("userName");

                    boolean operationResult = handleAuth(output, clientUserName);

                    break;
                }
                case ("twisted"): {
                    // TODO: twisted-request
                    break;
                }
                case ("join_us"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);
                    String clientUserName = (String) dataFromClient.get("userName");
                    String clientPublicKey = (String) dataFromClient.get("publicKey");

                    boolean operationResult = handleJoinUs(output, clientUserName, clientPublicKey);

                    if (operationResult)
                        Log.write("ADDED USER " + clientUserName);
                    else
                        Log.write("ERROR WHILE ADDING USER " + clientUserName);
                    break;
                }
                case ("hypnotize"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);
                    String clientUserName = (String) dataFromClient.get("userName");
                    String clientNickName = (String) dataFromClient.get("nickName");
                    String clientSession = (String) dataFromClient.get("session");

                    boolean operationResult = handleHypnotize(output, clientUserName, clientNickName, clientSession);

                    if(operationResult)
                        Log.write("ADDED NICKNAME " + clientNickName + " FOR USER " + clientUserName);
                    else
                        Log.write("ERROR WHILE ADDING NICKNAME " + clientNickName + " FOR USER " + clientUserName);
                    break;
                }
                case ("send"): {
                    // TODO: send-request
                    break;
                }
                case("check_mail"): {
                    // TODO: check_mail
                    break;
                }
                default: {
                    // TODO: default
                    break;
                }
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
