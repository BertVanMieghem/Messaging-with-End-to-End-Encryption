package be.scc.server;

import be.scc.common.SccEncryption;
import be.scc.common.Util;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.UUID;

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
                    case "/register_user": {

                        var bodyParams = Util.getBodyParams(httpExchange);
                        var access_token = bodyParams.get("access_token").get(0);
                        var public_key = bodyParams.get("public_key").get(0);

                        URL url = new URL("https://graph.facebook.com/v3.2/me?access_token=" + access_token + "&method=get&pretty=0&sdk=joey&suppress_http_code=1");
                        var obj = Util.SyncJsonRequest(url);
                        long facebook_id = Long.parseLong(obj.getString("id"));
                        String facebook_name = obj.getString("name");


                        DbSingleton.inst().insertUser(facebook_id, facebook_name, SccEncryption.deserialisePublicKey(public_key));
                        JSONObject jsonRet = new JSONObject();
                        jsonRet.put("message", "insertUser done");
                        jsonRet.put("facebook_id", facebook_id);
                        jsonRet.put("facebook_name", facebook_name);
                        data = jsonRet.toString().getBytes();
                        break;
                    }
                    case "/get_users": {
                        // Todo: user needs session token before accesing this
                        var users = DbSingleton.inst().getAllUsers();
                        data = users.toString().getBytes();
                        break;
                    }

                    case "/add_handshake": {
                        var bodyParams = Util.getBodyParams(httpExchange);
                        var message = bodyParams.get("message").get(0);

                        DbSingleton.inst().addHandshake(message);
                        JSONObject jsonRet = new JSONObject();
                        jsonRet.put("message", path + " done");
                        //jsonRet.put("handshake_id", id);
                        data = jsonRet.toString().getBytes();
                        break;
                    }
                    case "/get_handshake_buffer": {
                        URI uri = httpExchange.getRequestURI();
                        var qs = Util.decodeQueryString(uri);
                        var last_handshake_buffer_index = Integer.parseInt(qs.get("last_handshake_buffer_index"));
                        var json = DbSingleton.inst().getHandshakes(last_handshake_buffer_index);
                        data = json.toString().getBytes();
                        break;
                    }

                    case "/add_message": {
                        var bodyParams = Util.getBodyParams(httpExchange);
                        var message = bodyParams.get("message").get(0);
                        var target_ephemeral_id = bodyParams.get("target_ephemeral_id").get(0);

                        DbSingleton.inst().addMessage(message, UUID.fromString(target_ephemeral_id));
                        JSONObject jsonRet = new JSONObject();
                        jsonRet.put("message", path + " done");
                        //jsonRet.put("message_id", id);
                        data = jsonRet.toString().getBytes();
                        break;
                    }
                    case "/get_message_buffer": {
                        URI uri = httpExchange.getRequestURI();
                        var qs = Util.decodeQueryString(uri);
                        var last_message_buffer_index = Integer.parseInt(qs.get("last_message_buffer_index"));
                        var temp = qs.get("ephemeral_ids");
                        String[] ephemeral_ids = temp.split("\\|");
                        var json = DbSingleton.inst().getMessages(last_message_buffer_index, ephemeral_ids);
                        data = json.toString().getBytes();
                        break;
                    }

                    default: {
                        String response = "StaticHandler default handle: " + path;
                        statusCode = 404;
                        data = response.getBytes();
                        break;
                    }
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
