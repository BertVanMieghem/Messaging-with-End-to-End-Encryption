package be.scc.client;

import be.scc.common.SccEncryption;
import be.scc.common.Util;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;


public class ClientSingleton {
    private static ClientSingleton single_instance = null;

    // private constructor restricted to this class itself
    private ClientSingleton() {
    }

    public void Initilise() throws Exception {
        db.loadFromDb();
        loginDialog = new LoginDialog();
        chatDialog = new ChatDialog();

        loginDialog.Initialisation();
    }

    /**
     * static method to create instance of Singleton class
     */
    public static ClientSingleton inst() {
        if (single_instance == null)
            single_instance = new ClientSingleton();

        return single_instance;
    }

    public ClientDB db = new ClientDB();

    LoginDialog loginDialog;
    ChatDialog chatDialog;

    public void ShowLoginDialog() {
        loginDialog.pack();
        loginDialog.setVisible(true);
    }

    public void FromLoginToChatDialog() {
        loginDialog.setVisible(false);

        chatDialog.pack();
        chatDialog.setVisible(true);
    }

    public void PullUsers() throws Exception {
        URL url = new URL("http://localhost:5665/get_users");

        var jsonObj = Util.SyncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("users")) {
            var obj = (JSONObject) row;
            db.addUser(obj.getInt("id"), obj.getLong("facebook_id"), obj.getString("facebook_name"), SccEncryption.deserialisePublicKey(obj.getString("public_key")));
        }
        db.saveToDb();
    }

    public void PullServerEvents() throws Exception {

        var idx = ClientSingleton.inst().db.last_handshake_buffer_index;
        URL url = new URL("http://localhost:5665/get_handshake_buffer?last_handshake_buffer_index=" + idx);

        var jsonObj = Util.SyncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("handshake_buffer")) {
            var obj = (JSONObject) row;
            var h = new handshake_row();
            h.fillInFromJson(obj);
            try {
                var incomming_symetric_key = SccEncryption.Decript(db.keyPair.getPrivate(), h.getMessageAsBytes());
                h.client_can_decode = incomming_symetric_key;
            } catch (GeneralSecurityException ex) {
                h.client_can_decode = "NO";
            }
            db.insertHandshake(h);
        }
        db.saveToDb();
    }


    public void handshakeWithFacebookId(long facebook_id) throws Exception {
        var user = db.getUserWithFacebookId(facebook_id);
        user.ephemeral_key_outgoing = SccEncryption.GenerateSymetricKey();
        var params = new HashMap<String, String>();
        var key = SccEncryption.serializeKey(user.ephemeral_key_outgoing);
        params.put("message", Base64.getEncoder().encodeToString(SccEncryption.Encript(user.public_key, key)));

        var result = Util.SyncRequestPost(new URL("http://localhost:5665/add_handshake"), params);
        db.updateUserInDb(user); // only set when webrequest succeeded
    }
}
