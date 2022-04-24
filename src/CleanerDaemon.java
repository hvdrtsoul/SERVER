public class CleanerDaemon extends Thread{
    @Override
    public void run() {

        DatabaseHandler database = new DatabaseHandler();

        while(true){
            database.cleanConnections();
            Log.write("CLEANER DAEMON DONE WORK");
            try {
                Thread.sleep(Constants.CLEAN_CONNECTIONS_TIME);
            } catch (InterruptedException e) {
                System.out.println("EXCEPTION WHILE CLEANER DAEMON TRIED TO GO TO SLEEP");
            }
        }

    }
}
