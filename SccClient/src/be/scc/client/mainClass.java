package be.scc.client;

import be.scc.common.*;
import com.sun.net.httpserver.*;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Map;

import java.security.*;
import java.security.cert.Certificate; // Solves a java version incompatibility error
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

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

                try {
                    KeyPair pair = SccEncryption.GenerateKeypair();
                    // TODO: Post request
                    URL url = new URL("http://localhost:5665/registerUser?access_token=" + access_token);//+ "&public_key=" + SccEncryption.serializeKey(pair.getPublic()));
                    String ret = Util.SyncRequest(url);
                    JSONObject obj = new JSONObject(ret);
                    int facebook_id = obj.getInt("facebook_id");

                    ClientSingleton.inst().db.setFacebookId(facebook_id);
                    ClientSingleton.inst().db.setSecretPublicKeys(pair);

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
    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("Hello World!");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Interface' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5661), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();


        ClientSingleton.inst().ShowLoginDialog();

    }
}
