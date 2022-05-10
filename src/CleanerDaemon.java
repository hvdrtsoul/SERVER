import java.util.ArrayList;

import static java.lang.System.currentTimeMillis;

public class CleanerDaemon extends Thread{

    public void cleanLongInactiveUsers(){
        DatabaseHandler database = new DatabaseHandler();
        ArrayList<String> longInactiveUsers = database.getLongInactiveUsers();

        for(String inactiveUser : longInactiveUsers){
            database.deleteUser(inactiveUser);
            database.deleteMessagesToAndFromUser(inactiveUser);
            database.deleteUserNickname(inactiveUser);
            Log.write("CLEANER DAEMON DELETED USER " + inactiveUser + " FOR INACTIVITY");
        }

    }

    @Override
    public void run() {

        DatabaseHandler database = new DatabaseHandler();
        long willCleanInactiveUsersAt = currentTimeMillis() / 1000L + Constants.CLEAN_INACTIVE_USER_TIME;

        while(true){
            database.cleanConnections();
            database.cleanAuth();

            if(currentTimeMillis() / 1000L >= willCleanInactiveUsersAt){
                cleanLongInactiveUsers();
                willCleanInactiveUsersAt += Constants.CLEAN_INACTIVE_USER_TIME;
            }

            Log.write("CLEANER DAEMON DONE WORK");
            try {
                Thread.sleep(Constants.CLEAN_CONNECTIONS_TIME);
            } catch (InterruptedException e) {
                System.out.println("EXCEPTION WHILE CLEANER DAEMON TRIED TO GO TO SLEEP");
            }
        }

    }
}
