

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class Handler extends Thread{

    private final Socket socket;

    Handler(Socket socket){
        this.socket = socket;
    }

    private void sendResponse(OutputStream output, String response){

        //System.out.println("RESPONSE IS " + response);

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
            JSONObject data = new JSONObject();
            data.put(Constants.RESPONSE_PUBLIC_DFH_KEY_HEADER, myPublicKey);

            jsonResponse.put(Constants.RESPONSE_HEADER_DATA, data);
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

    private boolean handleJoinUs(OutputStream output, String name, String publicKey, String sharedKey){

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
            database.insertLastActive(name);
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);

            JSONObject data = new JSONObject();
            data.put(Constants.SESSION_HEADER, result);
            Sanitizer sanitizer = new Sanitizer();
            ANomalUSProvider anomalus = new ANomalUSProvider();
            byte[] encodedBytes = anomalus.encodeBytes(data.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

            jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));
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

    private void handleBadRequest(OutputStream output){
        JSONObject jsonRespose = new JSONObject();

        jsonRespose.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
        jsonRespose.put(Constants.ADDITIONAL_INFO_HEADER, Constants.BAD_REQUEST_MESSAGE);
        sendResponse(output, jsonRespose.toJSONString());
    }

    private boolean handleAuth(OutputStream output, String userName, String sharedKey){
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.userExists(userName)){ // if user does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.AUTH_USER_DOES_NOT_EXIST);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        UserNameHandler userNameHandler = new UserNameHandler();
        Sanitizer sanitizer = new Sanitizer();

        String secret = userNameHandler.generateSecret();

        if(!database.addAuth(userName, secret)){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
        else {
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
            ANomalUSProvider anomalus = new ANomalUSProvider();

            JSONObject data = new JSONObject();

            byte[] encodedSecret = anomalus.encodeBytes(secret.getBytes(StandardCharsets.UTF_8), new BigInteger(database.getPublicKey(userName)));

            data.put(Constants.AUTH_CHALLENGE_HEADER, sanitizer.sanitize(encodedSecret));


            byte[] encodedBytes = anomalus.encodeBytes(data.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

            jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));
            sendResponse(output, jsonResponse.toJSONString());
            return true;
        }

    }

    private boolean handleTwisted(OutputStream output, String userName, String solution, String sharedKey) {
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if (!database.userExists(userName)) { // if user does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.TWISTED_USER_DOES_NOT_EXIST);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        String secret = database.getSecret(userName, System.currentTimeMillis() / 1000L);

        if (secret.equals(Constants.TWISTED_SECRET_NOT_FOUND)) { // secret not found
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.TWISTED_SECRET_NOT_FOUND);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        } else if (secret.equals(solution)) { // if challenge solved right
            SessionHandler sessionHandler = new SessionHandler();
            String newSession = sessionHandler.generateSession();

            database.removeAuth(userName); // we remove this auth attempt - challenge was solved

            if (database.setSession(userName, newSession)) { // if we sucessfully added new session
                jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);

                JSONObject data = new JSONObject();
                data.put(Constants.TWISTED_NEW_SESSION_HEADER, newSession);

                Sanitizer sanitizer = new Sanitizer();
                ANomalUSProvider anomalus = new ANomalUSProvider();
                byte[] encodedBytes = anomalus.encodeBytes(data.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

                jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));

                database.updateLastActive(userName); // updating last active for this user

                sendResponse(output, jsonResponse.toJSONString());
                return true;
            } else { // if something went wrong when adding new session
                jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
                jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
                sendResponse(output, jsonResponse.toJSONString());
                return false;
            }
        } else { // if challenge solved wrong
            database.removeAuth(userName); // we remove this auth attempt - only one try to solve allowed
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.TWISTED_WRONG_SOLUTION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
    }


    private boolean handleGetUsername(OutputStream output, String nickName, String sharedKey){
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.nicknameExists(nickName)){ // if nickname does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.NICKNAME_NOT_FOUND);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        String userName = database.getUserNameByNickName(nickName);

        if(userName.equals(Constants.NICKNAME_NOT_FOUND)){
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }else{
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);

            JSONObject data = new JSONObject();
            Sanitizer sanitizer = new Sanitizer();
            data.put(Constants.GET_USERNAME_USERNAME_HEADER, userName);
            ANomalUSProvider anomalus = new ANomalUSProvider();
            byte[] encodedBytes = anomalus.encodeBytes(data.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

            jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));

            sendResponse(output, jsonResponse.toJSONString());
            return true;
        }

    }

    private boolean handleSend(OutputStream output, String session, String to, String from, String type, String data, String sharedKey){
        SessionHandler sessionHandler = new SessionHandler();
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.userExists(to)){ // if recipient does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SEND_RECIPIENT_DOES_NOT_EXIST);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!database.userExists(from)){ // if sender does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION+"NOSENDER");
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!sessionHandler.checkAuth(session, from)){ // if sender is not authorized
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!Arrays.asList(Constants.SEND_MESSAGE_TYPES).contains(type)){ // if message type is unknown
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SEND_UNKNOWN_MESSAGE_TYPE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        long sentTime = database.saveMessage(to, from, type, data);

        if(sentTime == -1){ // message was not saved
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }else{ // everything's okay
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
            JSONObject dataJSON = new JSONObject();

            dataJSON.put(Constants.SEND_TIMESTAMP_HEADER, String.valueOf(sentTime));

            Sanitizer sanitizer = new Sanitizer();
            ANomalUSProvider anomalus = new ANomalUSProvider();
            byte[] encodedBytes = anomalus.encodeBytes(dataJSON.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

            jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));

            sendResponse(output, jsonResponse.toJSONString());
            return true;
        }
    }

    private boolean handleCheckMail(OutputStream output, String userName, String session, String sharedKey) {
        SessionHandler sessionHandler = new SessionHandler();
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.userExists(userName)){ // if user does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!sessionHandler.checkAuth(session, userName)){ // if user is not authorized
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        String mail = database.checkMail(userName);

        if(mail.equals(Constants.SOMETHING_WENT_WRONG_MESSAGE)){ // if something went wrong we send an error
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
        // otherwise everyting's okay

        jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);

        JSONObject data = new JSONObject();
        data.put(Constants.CHECK_MAIL_MESSAGES_HEADER_NAME, mail);
        Sanitizer sanitizer = new Sanitizer();
        ANomalUSProvider anomalus = new ANomalUSProvider();
        byte[] encodedBytes = anomalus.encodeBytes(data.toJSONString().getBytes(StandardCharsets.UTF_8), new BigInteger(sharedKey));

        jsonResponse.put(Constants.RESPONSE_HEADER_DATA, sanitizer.sanitize(encodedBytes));

        sendResponse(output, jsonResponse.toJSONString());
        return true;
    }

    private boolean handleGetMessage(OutputStream output, String userName, String session, String id, String sharedKey) {
        SessionHandler sessionHandler = new SessionHandler();
        DatabaseHandler database = new DatabaseHandler();
        JSONObject jsonResponse = new JSONObject();

        if(!database.userExists(userName)){ // if user does not exist
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(!sessionHandler.checkAuth(session, userName)){ // if user is not authorized
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.INCORRECT_SESSION);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }

        if(database.messageExistsAndMine(userName, id)){
            jsonResponse = database.getMessageById(id, sharedKey);

            sendResponse(output, jsonResponse.toJSONString()); // if something went wrong jsonResponse
                                                               // contains SOMETHING_WENT_WRONG response

            if(((String)(jsonResponse.get(Constants.RESPONSE_HEADER_NAME))).equals(Constants.RESPONSE_HEADER_ERROR))
                return false;

            // otherwise we've sent OKAY-result response
            // at this point message is successfully sent and we can delete it from database

            database.removeMessageById(id);

            return true;
        }else{
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.GET_MESSAGE_FAIL);
            sendResponse(output, jsonResponse.toJSONString());
            return false;
        }
    }

    private JSONObject getDataFromClient(Object encodedDataString, String sharedKey){

        ANomalUSProvider anomalus = new ANomalUSProvider();
        JSONParser parser = new JSONParser();
        Sanitizer sanitizer = new Sanitizer();

        byte[] encodedBytes = sanitizer.unSanitize((String)encodedDataString);

        String result = new String(anomalus.decodeBytes(encodedBytes, new BigInteger(sharedKey)));

        System.out.println(result);

        try {
            return (JSONObject)parser.parse(result);
        } catch (ParseException e) {
            return null;
        }
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
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientUserName = (String) dataFromClient.get("userName");

                    boolean operationResult = handleAuth(output, clientUserName, sharedKey);

                    if(operationResult)
                        Log.write("CREATED AUTH WITH " + clientUserName);
                    else
                        Log.write("ERROR WHILE CREATING AUTH WITH " + clientUserName);
                    break;
                }
                case ("twisted"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName") || !dataFromClient.containsKey("solution")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientUserName = (String) dataFromClient.get("userName");
                    String clientSolution = (String) dataFromClient.get("solution");

                    boolean operationResult = handleTwisted(output, clientUserName, clientSolution, sharedKey);

                    if(operationResult)
                        Log.write("SUCCESSFULLY AUTH WITH " + clientUserName);
                    else
                        Log.write("ERROR WHILE TRYING TO AUTH WITH " + clientUserName);
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

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName") || !dataFromClient.containsKey("publicKey")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientUserName = (String) dataFromClient.get("userName");
                    String clientPublicKey = (String) dataFromClient.get("publicKey");

                    boolean operationResult = handleJoinUs(output, clientUserName, clientPublicKey, sharedKey);

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

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName") || !dataFromClient.containsKey("nickName") || !dataFromClient.containsKey("session")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

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
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("to") || !dataFromClient.containsKey("from") ||
                            !dataFromClient.containsKey("type") || !dataFromClient.containsKey("data") ||
                            !dataFromClient.containsKey("session")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientSession = (String)dataFromClient.get("session");
                    String clientTo = (String)dataFromClient.get("to");
                    String clientFrom = (String)dataFromClient.get("from");
                    String clientType = (String)dataFromClient.get("type");
                    String clientData = (String)dataFromClient.get("data"); // sanitized BLOB

                    boolean operationResult = handleSend(output, clientSession, clientTo, clientFrom, clientType, clientData, sharedKey);

                    if(operationResult)
                        Log.write("SENT MESSAGE FROM " + clientFrom + " TO " + clientTo);
                    else
                        Log.write("FAILED TO SEND MESSAGE FROM " + clientFrom + " TO " + clientTo);
                    break;
                }
                case("check_mail"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName") || !dataFromClient.containsKey("session")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientUserName = (String)dataFromClient.get("userName");
                    String clientSession = (String)dataFromClient.get("session");

                    boolean operationResult = handleCheckMail(output, clientUserName, clientSession, sharedKey);

                    /*
                    if(operationResult)
                        Log.write("CHECKED MAIL FOR " + clientUserName);
                    else
                        Log.write("ERROR WHILE CHECKIG MAIL FOR " + clientUserName);
                    */
                    break;
                }
                case("get_username"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("nickName")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientNickName = (String) dataFromClient.get("nickName");

                    boolean operationResult = handleGetUsername(output, clientNickName, sharedKey);

                    if(operationResult)
                        Log.write("FOUND USERNAME FOR NICKNAME " + clientNickName);
                    else
                        Log.write("ERROR WHILE TRYING TO FIND USERNAME FOR NICKNAME " + clientNickName);
                    break;
                }
                case("get_message"): {
                    DatabaseHandler database = new DatabaseHandler();
                    String sharedKey = database.getSharedKey(clientIp);

                    if(sharedKey == Constants.CONNECTION_NOT_FOUND_MESSAGE){
                        handleNotConnected(output);
                        break;
                    }

                    JSONObject dataFromClient = getDataFromClient(requestData, sharedKey);

                    if(dataFromClient == null){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    if(!dataFromClient.containsKey("userName") || !dataFromClient.containsKey("session") || !dataFromClient.containsKey("id")){
                        handleBadRequest(output);
                        Log.write("ERROR WHILE PARSING REQUEST INFO");
                        return;
                    }

                    String clientUserName = (String)dataFromClient.get("userName");
                    String clientSession = (String)dataFromClient.get("session");
                    String clientId = (String)dataFromClient.get("id");

                    boolean operationResult = handleGetMessage(output, clientUserName, clientSession, clientId, sharedKey);

                    if(operationResult)
                        Log.write("GOT MESSAGE WITH ID " + clientId + " FOR USER " + clientUserName);
                    else
                        Log.write("ERROR WHILE GETTING MESSAGE WITH ID " + clientId + " FOR USER " + clientUserName);
                    break;
                }
                default: { // request with unknown type is bad request
                    handleBadRequest(output);
                    Log.write("SUCESSFULLY HANDLED BAD REQUEST");
                    break;
                }
            }

        }catch(IOException e){ // stream is closed or something???
            System.out.println("INPUT-OUTPUT EXCEPTION WHILE READING REQUEST");
            e.printStackTrace();
        }catch (ParseException e) { // wrong request
            System.out.println("EXCEPTION WHILE PARSING JSON FROM REQUEST");
            e.printStackTrace();
            Log.write("SUCESSFULLY HANDLED BAD REQUEST");
            return;
        }
    }
}
