
import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) throws IOException {
        String targetUrl = "http://192.168.1.4:3389/";
        String bindIp = "::";
        int listenTcpPort = 8080;

        startServer(bindIp, listenTcpPort, targetUrl);
    }

    private static void startServer(String bindIp, int listenTcpPort, String targetUrl) throws IOException {

        ServerSocket serverSocket = new ServerSocket(listenTcpPort);
        System.out.println("Listening on " + bindIp + ":" + listenTcpPort);

        while (true) {
            new Handler(serverSocket.accept(), targetUrl).start();
        }
    }
}
