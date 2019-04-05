package be.scc.client;

import be.scc.common.Util;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

public class ClientSingleton {
    private static ClientSingleton single_instance = null;

    // private constructor restricted to this class itself
    private ClientSingleton() {
    }

    // static method to create instance of Singleton class
    public static ClientSingleton inst() {
        if (single_instance == null)
            single_instance = new ClientSingleton();

        return single_instance;
    }

    public ClientDB db = new ClientDB();

    LoginDialog loginDialog = new LoginDialog();
    ChatDialog chatDialog = new ChatDialog();

    public void ShowLoginDialog() {
        loginDialog.pack();
        loginDialog.setVisible(true);
    }

    public void FromLoginToChatDialog() {
        loginDialog.setVisible(false);

        chatDialog.pack();
        chatDialog.setVisible(true);
    }

    public void PullUsers() throws IOException, SQLException {
        URL url = new URL("http://localhost:5665/get_users");

        var jsonObj = Util.SyncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("users")) {
            var obj = (JSONObject) row;
            db.addUser(obj.getInt("id"), obj.getLong("facebook_id"), obj.getString("public_key"));
        }
    }

    public void PullKpi() throws IOException {

        var idx = ClientSingleton.inst().db.last_handshake_buffer_index;
        URL url = new URL("http://localhost:5665/get_handshake_buffer?last_handshake_buffer_index=" + idx);

        var jsonObj = Util.SyncJsonRequest(url);
        jsonObj.keySet().forEach(keyStr ->
        {
            Object keyvalue = jsonObj.get(keyStr);
            System.out.println("key: " + keyStr + " value: " + keyvalue);
            //for nested objects iteration if required
            //if (keyvalue instanceof JSONObject)
            //    printJsonObject((JSONObject)keyvalue);
        });
    }
}
