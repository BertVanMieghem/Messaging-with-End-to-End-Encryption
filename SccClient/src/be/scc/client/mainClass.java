package be.scc.client;

import be.scc.common.*;
import com.sun.net.httpserver.*;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

import java.security.*;

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
                Map<String, String> queryParameters = Util.decodeQueryString(uri);

                String access_token = queryParameters.get("access_token");
                data = "callback accepted".getBytes();

                try {
                    KeyPair pair = SccEncryption.GenerateKeypair();

                    URL url = new URL("http://localhost:5665/register_user");
                    var params = new HashMap<String, String>();
                    params.put("access_token", access_token);
                    params.put("public_key", SccEncryption.serializeKey(pair.getPublic()));

                    String ret = Util.SyncRequestPost(url, params);
                    JSONObject obj = new JSONObject(ret);
                    var facebook_id = obj.getLong("facebook_id");

                    ClientSingleton.inst().db.facebook_id = (facebook_id);
                    ClientSingleton.inst().db.keyPair = (pair);
                    ClientSingleton.inst().db.saveToDb();

                    Runnable runner = new Runnable() {
                        public void run() {
                            ClientSingleton.inst().FromLoginToChatDialog();
                        }
                    };
                    EventQueue.invokeLater(runner);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");
        ClientSingleton.inst().Initilise();

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Interface' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5661), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        ClientSingleton.inst().ShowLoginDialog();

    }
}
