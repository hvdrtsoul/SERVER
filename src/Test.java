import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) throws IOException{

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(9674);
        } catch (IOException e) {
            System.out.println("CANT OPEN A CONNECTION");
            e.printStackTrace();
        }
		System.out.println(serverSocket.getInetAddress());
		
		Socket clientSocket = serverSocket.accept();
		clientSocket.getOutputStream().write("ANANAS WORKING".getBytes(StandardCharsets.UTF_8));
		
		System.out.println(((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress());
		
		clientSocket.close();
		serverSocket.close();

    }
}