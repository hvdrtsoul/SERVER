import java.sql.*;
import java.sql.Connection;

import static java.lang.System.currentTimeMillis;

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

    public boolean updateLastActive(String userName){
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



}
