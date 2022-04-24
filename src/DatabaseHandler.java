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
        long aliveUntil = (currentTimeMillis() / 1000) + Constants.ADDITIONAL_UPDATE_TIME;

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
}
