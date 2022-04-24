

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class OUTSiDE {

    private int port;

    public OUTSiDE(int port){
        this.port = port;
    }

    void start(){
        Log.write("STARTING SERVER UP...");
        //CleanerDaemon cleanerDaemon = new CleanerDaemon();
        //cleanerDaemon.start();
        //Log.write("CLEANER DAEMON STARTED");

        Log.write("SERVER STARTED");
        try(ServerSocket server = new ServerSocket(this.port)){
            while(true){
                Socket socket = server.accept();
                Handler thread = new Handler(socket);
                thread.start();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
