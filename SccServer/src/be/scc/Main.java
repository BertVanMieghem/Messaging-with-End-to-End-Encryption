package be.scc;

import be.scc.common.Util;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.sql.*;
import java.util.Map;
import java.util.Random;

import org.json.*;


public class Main {

    static class StaticHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            String path = httpExchange.getRequestURI().getPath();
            System.out.println("StaticHandler handle: " + path);

            int statusCode = 200;
            byte[] data;
            try {
                switch (path) {
                    case "/insertUser":

                        URI uri = httpExchange.getRequestURI();
                        Map<String, String> queryParameters = Util.splitQuery(uri);
                        String accessToken = queryParameters.get("accessToken");
                        data = "callback accepted".getBytes();

                        URL url = new URL("https://graph.facebook.com/v3.2/me?access_token=" + accessToken + "&method=get&pretty=0&sdk=joey&suppress_http_code=1");
                        String ret = Util.SyncRequest(url);
                        JSONObject obj = new JSONObject(ret);
                        String username = obj.getString("userName");
                        int userid = obj.getInt("id");

                        Random r = new Random();
                        DbSingleton.inst().InsertUser(userid, r.nextInt(1000));
                        data = "insertUser done".getBytes();
                        break;
                    default:
                        String response = "StaticHandler default handle: " + path;

                        data = response.getBytes();
                        break;
                }
            } catch (SQLException e) {
                statusCode = 500;
                data = e.getMessage().getBytes();
                e.printStackTrace();
            }

            httpExchange.sendResponseHeaders(statusCode, data.length);
            httpExchange.getResponseBody().write(data);
            httpExchange.getResponseBody().close();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Setver started");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Server' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5665), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();


    }
}
