package be.scc.client;

import be.scc.common.SccEncryption;
import be.scc.common.Util;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class FacebookLoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String path = httpExchange.getRequestURI().getPath();
        System.out.println("StaticHandler handle: " + path);

        byte[] data;
        switch (path) {
            case "/login.html":
                data = Util.readFileLineByLine("res" + path).getBytes();
                break;

            case "/callback":
                URI uri = httpExchange.getRequestURI();
                Map<String, String> queryParameters = Util.decodeQueryString(uri);

                String access_token = queryParameters.get("access_token");
                data = "callback accepted".getBytes();

                try {
                    KeyPair pair = SccEncryption.generateKeypair();

                    URL url = new URL("http://localhost:5665/register_user");
                    var params = new HashMap<String, String>();
                    params.put("access_token", access_token);
                    params.put("public_key", SccEncryption.serializeKey(pair.getPublic()));

                    String ret = Util.syncRequestPost(url, params);
                    JSONObject obj = new JSONObject(ret);
                    var facebook_id = new FacebookId(obj.getLong("facebook_id"));

                    ClientSingleton.inst().db.facebook_id = facebook_id;
                    ClientSingleton.inst().db.keyPair = (pair);
                    ClientSingleton.inst().db.saveToDb();

                    //ClientSingleton.inst().fromLoginToChatDialog();
                    Runnable runner = () -> ClientSingleton.inst().fromLoginToChatDialog();
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
