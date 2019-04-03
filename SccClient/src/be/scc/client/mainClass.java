package be.scc.client;

import be.scc.common.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Map;

class StaticHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String path = httpExchange.getRequestURI().getPath();
        System.out.println("StaticHandler handle: " + path);

        byte[] data;
        switch (path) {
            case "/login.html":
                data = Util.ReadFileLineByLine("res" + path).getBytes();
                break;

            case "/callback":
                URI uri = httpExchange.getRequestURI();
                Map<String, String> queryParameters = Util.splitQuery(uri);

                String access_token = queryParameters.get("access_token");
                data = "callback accepted".getBytes();

                URL url = new URL("http://localhost:5665/registerUser?access_token=" + access_token);
                String ret = Util.SyncRequest(url);

                Runnable runner = new Runnable() {
                    public void run() {
                        SccSingleton.inst().FromLoginToChatDialog();
                    }
                };
                EventQueue.invokeLater(runner);
                break;
            default:
                String response = "StaticHandler handle: " + path;

                data = response.getBytes();
                break;
        }
        httpExchange.sendResponseHeaders(200, data.length);
        httpExchange.getResponseBody().write(data);
        httpExchange.getResponseBody().close();
    }
}

public class mainClass {
    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("Hello World!");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Interface' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5661), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();


        SccSingleton.inst().ShowLoginDialog();

    }
}
