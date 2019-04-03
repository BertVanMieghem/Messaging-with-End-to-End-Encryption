package be.scc.server;

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
                    case "/registerUser":

                        URI uri = httpExchange.getRequestURI();
                        Map<String, String> queryParameters = Util.splitQuery(uri);
                        String access_token = queryParameters.get("access_token");

                        URL url = new URL("https://graph.facebook.com/v3.2/me?access_token=" + access_token + "&method=get&pretty=0&sdk=joey&suppress_http_code=1");
                        String ret = Util.SyncRequest(url);
                        JSONObject obj = new JSONObject(ret);
                        String username = obj.getString("name");
                        long userid = Long.parseLong(obj.getString("id"));

                        Random r = new Random();
                        DbSingleton.inst().InsertUser(userid, r.nextInt(1000));
                        JSONObject jsonRet = new JSONObject();
                        jsonRet.put("message", "insertUser done");
                        jsonRet.put("facebook_id", userid);
                        data = jsonRet.toString().getBytes();
                        break;
                    default:
                        String response = "StaticHandler default handle: " + path;

                        data = response.getBytes();
                        break;
                }
            } catch (java.lang.Exception e) {
                statusCode = 500;
                String msg = e.getMessage();
                if (msg == null)
                    msg = e.getClass().getName();
                data = msg.getBytes(); // TODO disable in release mode
                e.printStackTrace();
            }

            httpExchange.sendResponseHeaders(statusCode, data.length);
            httpExchange.getResponseBody().write(data);
            httpExchange.getResponseBody().close();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Server started");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Server' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5665), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();


    }
}
