package be.scc.client;

import be.scc.common.FacebookId;
import be.scc.common.SccEncryption;
import be.scc.common.SccException;
import be.scc.common.Util;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

                try {
                    var bodyParams = Util.getBodyParams(httpExchange);
                    var step0 = bodyParams.get("friendlist").get(0);
                    var step1 = Util.base64(step0);
                    var step2 = new String(step1, StandardCharsets.UTF_8);
                    var step3 = new JSONObject(step2);
                    var list = new ArrayList<FacebookFriendRow>();
                    for (var row : step3.getJSONArray("data")) {
                        list.add(FacebookFriendRow.parse((JSONObject) row));
                    }
                    ClientSingleton.inst().db.insertFacebookFriends(list);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                String access_token = queryParameters.get("access_token");
                var facebook_id_long = Long.parseLong(queryParameters.get("facebook_id_long"));
                data = "callback accepted".getBytes();

                try {
                    KeyPair pair = SccEncryption.generateKeypair();

                    URL url = new URL("http://localhost:5665/register_user");
                    var params = new HashMap<String, String>();
                    params.put("access_token", access_token);
                    params.put("public_key", SccEncryption.serializeKey(pair.getPublic()));

                    String ret = Util.syncRequestPost(url, params);
                    JSONObject obj = new JSONObject(ret);
                    var facebook_id = FacebookId.fromString(obj.getString("facebook_id"));

                    if (!Objects.equals(FacebookId.doSlowHash(facebook_id_long), facebook_id))
                        throw new SccException("facebook_id received from server is not correct!"); // Man in the middle?

                    ClientSingleton.inst().db.facebook_id_long = facebook_id_long;
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
