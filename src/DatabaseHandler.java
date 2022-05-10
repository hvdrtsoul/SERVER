import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.in;

public class DatabaseHandler extends DatabaseConfig {
    Connection databaseConnection;

    public Connection getDatabaseConnection() throws ClassNotFoundException, SQLException{
        String connectionString = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;

        Class.forName("com.mysql.cj.jdbc.Driver");

        databaseConnection = DriverManager.getConnection(connectionString, dbUser, dbPass);

        return databaseConnection;
    }

    public boolean connectionExists(String adress){
        String select = "SELECT COUNT(*) FROM " + Constants.CONNECTIONS_TABLE +
                " WHERE " + Constants.CONNECTIONS_ADRESS + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, adress);

            ResultSet result = preparedStatement.executeQuery();
            result.next();

            if(result.getInt(1) == 0)
                return false;
            else
                return true;
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE CHECKING IF CONNECTION EXISTS");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING IF CONNECTION EXISTS");
            e.printStackTrace();
            return false;
        }
    }

    public void removeConnection(String adress){
        String delete = "DELETE FROM " + Constants.CONNECTIONS_TABLE + " WHERE " + Constants.CONNECTIONS_ADRESS +
                " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, adress);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE REMOVING A CONNECTION");
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE REMOVING A CONNECTION");
        }
    }

    public String getSharedKey(String adress){

        if(!connectionExists(adress)){
            return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        }

        String select = "SELECT " + Constants.CONNECTIONS_SHARED_KEY +" FROM " + Constants.CONNECTIONS_TABLE + " WHERE "
                + Constants.CONNECTIONS_ADRESS + " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, adress);

            ResultSet result = preparedStatement.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING SHARED KEY FOR CONNECTION FROM " + adress);
            return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING SHARED KEY FOR CONNECTION FROM " + adress);
            return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        }
    }

    public boolean addConnection(String adress, String sharedKey){

        if(connectionExists(adress)){
            removeConnection(adress);
        }

        String insert = "INSERT INTO " + Constants.CONNECTIONS_TABLE + " (" +
                Constants.CONNECTIONS_ADRESS + "," + Constants.CONNECTIONS_SHARED_KEY + "," +
                Constants.CONNECTIONS_ALIVE_UNTIL + ")" + "VALUES(?,?,?)";
        long aliveUntil = (currentTimeMillis() / 1000L) + Constants.ADDITIONAL_UPDATE_TIME;

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insert)){
            preparedStatement.setString(1, adress);
            preparedStatement.setString(2, sharedKey);
            preparedStatement.setLong(3, aliveUntil);

            preparedStatement.executeUpdate();
            return true;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE ADDING CONNECTION");
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE ADDING CONNECTION");
            e.printStackTrace();
            return false;
        }

    }

    public boolean updateConnection(String adress){
        if(!connectionExists(adress)){
            return false;
        }

        String update = "UPDATE " + Constants.CONNECTIONS_TABLE + " SET " +
                Constants.CONNECTIONS_ALIVE_UNTIL + " = " + Constants.CONNECTIONS_ALIVE_UNTIL +
                " + " + Constants.ADDITIONAL_UPDATE_TIME + " WHERE " + Constants.CONNECTIONS_ADRESS +
                " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(update)){
            preparedStatement.setString(1, adress);

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE UPDATING A CONNECTION");
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE UPDATING A CONNECTION");
            return false;
        }
    }

    public void cleanConnections(){
        String delete = "DELETE FROM " + Constants.CONNECTIONS_TABLE + " WHERE "
                + Constants.CONNECTIONS_ALIVE_UNTIL + " < ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setLong(1, currentTimeMillis() / 1000L);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE CLEANING CONNECTIONS");
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CLEANING CONNECTIONS");
        }
    }

    public void cleanAuth(){
        String delete = "DELETE FROM " + Constants.CREATING_SESSIONS_TABLE + " WHERE "
                + Constants.CREATING_SESSIONS_ALIVE_UNTIL + " < ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setLong(1, currentTimeMillis() / 1000L);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE CLEANING AUTH");
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CLEANING AUTH");
        }
    }

    public void deleteUser(String userName){
        String delete = "DELETE FROM " + Constants.USERS_TABLE + " WHERE "
                + Constants.USERS_USER + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, userName);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE DELETING USER " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE DELETING USER " + userName);
        }
    }

    public void deleteMessagesToAndFromUser(String userName){
        String delete = "DELETE FROM " + Constants.MESSAGES_TABLE + " WHERE `"
                + Constants.MESSAGES_TO + "` = ? OR `" + Constants.MESSAGES_FROM + "` = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, userName);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE DELETING MESSAGES OF USER " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE DELETING MESSAGES OF USER " + userName);
        }
    }

    public void deleteUserNickname(String userName){
        String delete = "DELETE FROM " + Constants.NICKNAMES_TABLE + " WHERE "
                + Constants.NICKNAMES_USER + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, userName);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE DELETING NICKNAME OF " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE DELETING NICKNAME OF " + userName);
        }
    }

    public ArrayList<String> getLongInactiveUsers(){
        ArrayList<String> result = new ArrayList<>();

        String select = "SELECT " + Constants.LAST_ACTIVE_USERNAME + "FROM " + Constants.LAST_ACTIVE_TABLE + " WHERE "
                + Constants.LAST_ACTIVE_LAST_ACTIVE + " < ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setLong(1, (currentTimeMillis() / 1000L) - Constants.CONSIDER_INACTIVE_TIME);

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                result.add(resultSet.getString(1));
            }

            return result;
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE CLEANING AUTH");
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CLEANING AUTH");
            return null;
        }
    }

    public String addUser(String userName, String publicKey){
        String insertToUsers = "INSERT INTO " + Constants.USERS_TABLE + " (" +
                Constants.USERS_USER + "," + Constants.USERS_PUBLIC_KEY + "," +
                Constants.USERS_SESSION + "," + Constants.USERS_LOGGED_UNTIL + ")" + "VALUES(?,?,?,?)";
        long loggedUntil = (currentTimeMillis() / 1000) + Constants.ADDITIONAL_LOGGED_IN_TIME;
        SessionHandler sessionHandler = new SessionHandler();
        String newSession = sessionHandler.generateSession();

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insertToUsers)){
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, publicKey);
            preparedStatement.setString(3, newSession);
            preparedStatement.setLong(4, loggedUntil);
            preparedStatement.executeUpdate();

            return newSession;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE ADDING USER");
            e.printStackTrace();
            return Constants.JOIN_US_ERROR;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE ADDING USER");
            return Constants.JOIN_US_ERROR;
        }


    }

    public boolean insertLastActive(String userName){
        String insertToLastActive = "INSERT INTO " + Constants.LAST_ACTIVE_TABLE + " (" +
                Constants.LAST_ACTIVE_USERNAME + "," + Constants.LAST_ACTIVE_LAST_ACTIVE + ")" + "VALUES(?,?)";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insertToLastActive)){
            preparedStatement.setString(1, userName);
            preparedStatement.setLong(2, currentTimeMillis() / 1000L);
            preparedStatement.executeUpdate();

            return true;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE UPDATING LAST ACTIVE");
            return false;
        } catch (ClassNotFoundException e){
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE UPDATING LAST ACTIVE");
            return false;
        }
    }

    public String getUserSession(String userName, long currentTime){
        String select = "SELECT * FROM " + Constants.USERS_TABLE + " WHERE "
                + Constants.USERS_USER + " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);
            ResultSet result = preparedStatement.executeQuery();
            // user DEFINETELY EXISTS
            result.next();
            long loggedUntil = result.getLong(5);

            if(loggedUntil >= currentTime)
                return result.getString(4);
            else
                return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE GETTING USER'S SESSION");
            e.printStackTrace();
            return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING USER'S SESSION");
            return Constants.CONNECTION_NOT_FOUND_MESSAGE;
        }
    }

    public boolean userExists(String userName){
        String select = "SELECT COUNT(*) FROM " + Constants.USERS_TABLE +
                " WHERE " + Constants.USERS_USER+ " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);
            ResultSet result = preparedStatement.executeQuery();
            result.next();

            if(result.getInt(1) == 0)
                return false;
            else
                return true;
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE CHECKING IF USER EXISTS");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING IF USER EXISTS");
            e.printStackTrace();
            return false;
        }
    }

    public boolean nicknameExists(String nickName){
        String select = "SELECT COUNT(*) FROM " + Constants.NICKNAMES_TABLE +
                " WHERE " + Constants.NICKNAMES_NICKNAME + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, nickName);
            ResultSet result = preparedStatement.executeQuery();
            result.next();

            if(result.getInt(1) == 0)
                return false;
            else
                return true;
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE CHECKING IF NICKNAME EXISTS");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING IF NICKNAME EXISTS");
            e.printStackTrace();
            return false;
        }
    }

    public boolean userClaimedNickname(String userName){
        String select = "SELECT COUNT(*) FROM " + Constants.NICKNAMES_TABLE +
                " WHERE " + Constants.NICKNAMES_USER + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);
            ResultSet result = preparedStatement.executeQuery();
            result.next();

            if(result.getInt(1) == 0)
                return false;
            else
                return true;
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE CHECKING IF USER CLAIMED A NICKNAME");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING IF USER CLAIMED A NICKNAME");
            e.printStackTrace();
            return false;
        }
    }

    public boolean addNickName(String nickName, String userName){
        String insertToNickNames = "INSERT INTO " + Constants.NICKNAMES_TABLE+ " (" +
                Constants.NICKNAMES_NICKNAME + "," + Constants.NICKNAMES_USER + ") " + "VALUES(?,?)";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insertToNickNames)){
            preparedStatement.setString(1, nickName);
            preparedStatement.setString(2, userName);
            preparedStatement.executeUpdate();

            return true;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE ADDING NICKNAME");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE ADDING NICKNAME");
            return false;
        }
    }

    public String getUserNameByNickName(String nickName){

        // nickname definetely exists

        String select = "SELECT " + Constants.NICKNAMES_USER +" FROM " + Constants.NICKNAMES_TABLE + " WHERE "
                + Constants.NICKNAMES_NICKNAME + " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, nickName);

            ResultSet result = preparedStatement.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING USER FOR NICKNAME " + nickName);
            return Constants.NICKNAME_NOT_FOUND;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING USER FOR NICKNAME " + nickName);
            return Constants.NICKNAME_NOT_FOUND;
        }
    }

    public boolean authExists(String userName){
        String select = "SELECT COUNT(*) FROM " + Constants.CREATING_SESSIONS_TABLE +
                " WHERE " + Constants.CREATING_SESSIONS_USER + " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);

            ResultSet result = preparedStatement.executeQuery();
            result.next();

            if(result.getInt(1) == 0)
                return false;
            else
                return true;
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE CHECKING IF AUTH EXISTS");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING IF AUTH EXISTS");
            e.printStackTrace();
            return false;
        }
    }

    public void removeAuth(String userName){
        String delete = "DELETE FROM " + Constants.CREATING_SESSIONS_TABLE + " WHERE " + Constants.CREATING_SESSIONS_USER +
                " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, userName);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE REMOVING AN AUTH");
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE REMOVING AN AUTH");
        }
    }

    public boolean addAuth(String userName, String secret){

        if(authExists(userName)){
            removeAuth(userName);
        }

        String insert = "INSERT INTO " + Constants.CREATING_SESSIONS_TABLE + " (" +
                Constants.CREATING_SESSIONS_USER + "," + Constants.CREATING_SESSIONS_SECRET + "," +
                Constants.CREATING_SESSIONS_ALIVE_UNTIL + ")" + "VALUES(?,?,?)";
        long aliveUntil = (currentTimeMillis() / 1000L) + Constants.ADDITIONAL_UPDATE_TIME;

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insert)){
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, secret);
            preparedStatement.setLong(3, aliveUntil);

            preparedStatement.executeUpdate();
            return true;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE ADDING AN AUTH");
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE ADDING AN AUTH");
            e.printStackTrace();
            return false;
        }
    }

    public String getPublicKey(String userName){

        // user definetely exists

        String select = "SELECT " + Constants.USERS_PUBLIC_KEY +" FROM " + Constants.USERS_TABLE + " WHERE "
                + Constants.USERS_USER + " = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);

            ResultSet result = preparedStatement.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING SHARED KEY FOR CONNECTION FROM " + userName);
            return Constants.AUTH_PUBLIC_KEY_NOT_FOUND;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING SHARED KEY FOR CONNECTION FROM " + userName);
            return Constants.AUTH_PUBLIC_KEY_NOT_FOUND;
        }
    }

    public String getSecret(String userName, long currentTime){

        if(!authExists(userName)){
            return Constants.TWISTED_SECRET_NOT_FOUND;
        }

        String select = "SELECT * FROM " + Constants.CREATING_SESSIONS_TABLE + " WHERE "
                + Constants.CREATING_SESSIONS_USER + " = ?" ;

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);
            ResultSet result = preparedStatement.executeQuery();
            result.next();

            long aliveUntil = result.getLong(3);

            if(aliveUntil >= currentTime){ // auth is still active
                return result.getString(2);
            }
            else{
                return Constants.TWISTED_SECRET_NOT_FOUND;
            }
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING SECRET FOR " + userName);
            return Constants.TWISTED_SECRET_NOT_FOUND;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING SECRET FOR " + userName);
            return Constants.TWISTED_SECRET_NOT_FOUND;
        }
    }

    public boolean setSession(String userName, String newSession){

        // user definetely exists

        String update = "UPDATE " + Constants.USERS_TABLE + " SET " +
                Constants.USERS_SESSION + " = ?, " + Constants.USERS_LOGGED_UNTIL + " = ?" +
                " WHERE " + Constants.USERS_USER +
                " = ?";

        long loggedUntil = (currentTimeMillis() / 1000) + Constants.ADDITIONAL_LOGGED_IN_TIME;

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(update)){
            preparedStatement.setString(1, newSession);
            preparedStatement.setLong(2, loggedUntil);
            preparedStatement.setString(3, userName);

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE SETTING NEW SESSION FOR USER " + userName);
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE SETTING NEW SESSION FOR USER " + userName);
            return false;
        }
    }

    public long saveMessage(String to, String from, String type, String data){
        String insert = "INSERT INTO " + Constants.MESSAGES_TABLE + " (`" +
                Constants.MESSAGES_TO + "`,`" + Constants.MESSAGES_FROM + "`,`" +
                Constants.MESSAGES_TYPE + "`,`" + Constants.MESSAGES_TIMESTAMP + "`,`" + Constants.MESSAGES_DATA + "`)" +
                " VALUES(?, ?, ?, ?, ?)";

        long sentTime = (currentTimeMillis() / 1000L);

        Sanitizer sanitizer = new Sanitizer();
        byte[] unSanitizedData = sanitizer.unSanitize(data);

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(insert)){
            preparedStatement.setString(1, to);
            preparedStatement.setString(2, from);
            preparedStatement.setString(3, type);
            preparedStatement.setLong(4, sentTime);
            preparedStatement.setBinaryStream(5, new ByteArrayInputStream(unSanitizedData), unSanitizedData.length);

            preparedStatement.executeUpdate();
            return sentTime;
        }catch (SQLException e){
            System.out.println("SQL EXCEPTION WHILE SAVING MESSAGE");
            e.printStackTrace();
            return -1;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE SAVING MESSAGE");
            e.printStackTrace();
            return -1;
        }
    }

    public String checkMail(String userName){
        String select = "SELECT " + Constants.MESSAGES_ID + " FROM " + Constants.MESSAGES_TABLE +
                " WHERE `" + Constants.MESSAGES_TO + "` = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, userName);
            ResultSet result = preparedStatement.executeQuery();

            StringBuilder resultBuilder = new StringBuilder();

            while(result.next()){
                resultBuilder.append(result.getLong(1));
                resultBuilder.append(Constants.CHECK_MAIL_SEPARATOR);
            }

            if(resultBuilder.isEmpty()){ // if everything's okay but there's no messages
                return Constants.CHECK_MAIL_NO_MESSAGES;
            }else{
                resultBuilder.deleteCharAt(resultBuilder.length()-1);
                return resultBuilder.toString();
            }
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE CHECKING MAIL FOR " + userName);
            return Constants.SOMETHING_WENT_WRONG_MESSAGE;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE CHECKING MAIL FOR " + userName);
            return Constants.SOMETHING_WENT_WRONG_MESSAGE;
        }
    }

    public boolean messageExistsAndMine(String userName, String messageId){
        String select = "SELECT `" + Constants.MESSAGES_TO + "` FROM " + Constants.MESSAGES_TABLE +
                " WHERE `" + Constants.MESSAGES_ID + "` = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, messageId);
            ResultSet result = preparedStatement.executeQuery();

            StringBuilder resultBuilder = new StringBuilder();

            while(result.next()){
                resultBuilder.append(result.getString(1));
            } // there's maximum ONE message with this id

            if(resultBuilder.isEmpty()) // if there's no message with this id resultBuilder will be empty
                return false; // message does not exist

            if(resultBuilder.toString().equals(userName)) // if this message belongs to me
                return true;
            else // it belongs to someone else
                return false;
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING MESSAGE WITH ID " + messageId + " FOR USER " + userName);
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING MESSAGE WITH ID " + messageId + " FOR USER " + userName);
            return false;
        }
    }

    public JSONObject getMessageById(String messageId){

        JSONObject jsonResponse = new JSONObject();
        // this message DEFINETELY exists
        String select = "SELECT * FROM " + Constants.MESSAGES_TABLE +
                " WHERE `" + Constants.MESSAGES_ID + "` = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(select)){
            preparedStatement.setString(1, messageId);
            ResultSet messageInfo = preparedStatement.executeQuery();

            messageInfo.next();

            try {
                jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_OKAY);
                jsonResponse.put(Constants.GET_MESSAGE_FROM_HEADER, messageInfo.getString(Constants.MESSAGES_FROM));
                jsonResponse.put(Constants.GET_MESSAGE_TYPE_HEADER, messageInfo.getString(Constants.MESSAGES_TYPE));
                jsonResponse.put(Constants.GET_MESSAGE_TIMESTAMP_HEADER, String.valueOf(messageInfo.getLong(Constants.MESSAGES_TIMESTAMP)));

                Blob byteData = messageInfo.getBlob(Constants.MESSAGES_DATA);
                Sanitizer sanitizer = new Sanitizer();

                jsonResponse.put(Constants.GET_MESSAGE_DATA_HEADER, sanitizer.sanitize(byteData.getBytes(1, (int)byteData.length())));

                return jsonResponse;
            }catch (SQLException e){
                e.printStackTrace();
                // something went wrong while obtaining message info
                jsonResponse.clear();
                jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
                jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);

                return jsonResponse;
            }

        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE GETTING MESSAGE WITH ID " + messageId);
            jsonResponse.clear();
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);

            return jsonResponse;
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE GETTING MESSAGE WITH ID " + messageId);
            jsonResponse.clear();
            jsonResponse.put(Constants.RESPONSE_HEADER_NAME, Constants.RESPONSE_HEADER_ERROR);
            jsonResponse.put(Constants.ADDITIONAL_INFO_HEADER, Constants.SOMETHING_WENT_WRONG_MESSAGE);

            return jsonResponse;
        }
    }

    public void removeMessageById(String id){
        String delete = "DELETE FROM " + Constants.MESSAGES_TABLE+ " WHERE `" + Constants.MESSAGES_ID +
                "` = ?";

        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(delete)){
            preparedStatement.setString(1, id);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE REMOVING A MESSAGE WITH ID " + id);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE REMOVING A MESSAGE WITH ID " + id);
        }
    }

    public void updateLastActive(String userName){

        String update = "UPDATE " + Constants.LAST_ACTIVE_TABLE + " SET " +
                Constants.LAST_ACTIVE_LAST_ACTIVE + " = " +  String.valueOf(currentTimeMillis() / 1000L) +
                " WHERE " + Constants.LAST_ACTIVE_USERNAME +
                " = ?";
        try(PreparedStatement preparedStatement = getDatabaseConnection().prepareStatement(update)){
            preparedStatement.setString(1, userName);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION WHILE UPDATING LAST ACTIVE FOR USER " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASS NOT FOUND EXCEPTION WHILE UPDATING LAST ACTIVE FOR USER " + userName);
        }
    }
}
