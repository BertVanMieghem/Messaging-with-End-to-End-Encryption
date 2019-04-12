package be.scc.client;

import be.scc.common.SccEncryption;
import be.scc.common.SccException;
import be.scc.common.Util;
import org.json.JSONObject;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;


public class ClientSingleton {
    private static ClientSingleton single_instance = null;

    // private constructor restricted to this class itself
    private ClientSingleton() {
    }

    public void Initilise() throws Exception {
        db.loadFromDb();
        loginDialog = new LoginDialog();
        chatDialog = new ChatDialog();

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

    public void OpenLoginOrSkip() throws IOException {
        if (ClientSingleton.inst().db.facebook_id != 0) {
            ClientSingleton.inst().fromLoginToChatDialog();
        } else {
            loginDialog.pack();
            loginDialog.Initialise();
            loginDialog.setVisible(true);
        }
    }

    public void fromLoginToChatDialog() {
        loginDialog.dispatchEvent(new WindowEvent(loginDialog, WindowEvent.WINDOW_CLOSING));

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
        PullHandshakes();
        PullMessages();
    }

    public void PullHandshakes() throws Exception {
        var last_handshake_buffer_index = ClientSingleton.inst().db.last_handshake_buffer_index;
        URL url = new URL("http://localhost:5665/get_handshake_buffer?last_handshake_buffer_index=" + last_handshake_buffer_index);

        var jsonObj = Util.SyncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("handshake_buffer")) {
            var obj = (JSONObject) row;
            var h = new handshake_row();
            h.fillInFromJson(obj);
            try {
                var parts = h.message.split("\\|");
                var encryptesSymetricalKeyString = parts[0];
                var encryptesSymetricalKey = Util.base64(encryptesSymetricalKeyString);
                var symetricalKeySerialised = SccEncryption.Decript(db.keyPair.getPrivate(), encryptesSymetricalKey);
                var symetricalKey = SccEncryption.deserialiseSymetricKey(symetricalKeySerialised);

                byte[] secondPartEncrypted = Util.base64(parts[1]);
                var secondPart = SccEncryption.Decript(symetricalKey, secondPartEncrypted);

                var idx = secondPart.lastIndexOf("|");
                var payload = secondPart.substring(0, idx);
                var json = new JSONObject(payload);
                var from_facebook_id = json.getLong("handshake_initiator_facebook_id");
                var datetime = json.getString("datetime");

                var sig = Util.base64(secondPart.substring(idx + 1));

                var from_user = db.getUserWithFacebookId(from_facebook_id);
                if (!SccEncryption.VerifySign(from_user.public_key, payload, sig))
                    throw new SccException("Signature from handshake was not valid!");

                from_user.ephemeral_key_ingoing = symetricalKey;
                db.updateUserInDb(from_user);

                h.client_can_decode = "YES";
            } catch (GeneralSecurityException ex) {
                h.client_can_decode = "NO";
            }
            db.insertHandshake(h);
        }
        db.saveToDb();
    }

    public void PullMessages() throws Exception {
        var last_message_buffer_index = ClientSingleton.inst().db.last_message_buffer_index;
        URL url = new URL("http://localhost:5665/get_message_buffer?last_message_buffer_index=" + last_message_buffer_index);

        var jsonObj = Util.SyncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("message_buffer")) {
            var obj = (JSONObject) row;
            var h = new message_row();
            h.fillInFromJson(obj);
            try {

                var users = db.getUsersThatShookOurHands();
                for (var user : users) {
                    try {
                        var message = Util.base64(h.message);
                        var payload = decryptPayloadAndVerify(message, user);
                        var json = new JSONObject(payload);
                        var message_type = json.getString("message_type");
                        var content = json.getString("content");

                        var cm = new cached_message_row();
                        cm.id = h.id;
                        cm.from_facebook_id = user.facebook_id;
                        cm.message = payload;
                        db.insertCachedMessage(cm);

                        System.out.println("message_type: " + message_type);
                        System.out.println("content: " + content);

                        h.client_can_decode = "YES";
                        break;
                    } catch (GeneralSecurityException ex) {
                        h.client_can_decode = "STILL TRYING";
                    }
                }

            } catch (GeneralSecurityException ex) {
                h.client_can_decode = "NO";
            }
            db.insertMessage(h);
        }
        db.saveToDb();
    }

    /**
     * Can not be used when decripting handshake, becouse we don't know what user to verify with then.
     */
    private String decryptPayloadAndVerify(byte[] secondPartEncrypted, local_user user) throws Exception {
        var secondPart = SccEncryption.Decript(user.ephemeral_key_ingoing, secondPartEncrypted);

        var idx = secondPart.lastIndexOf("|");
        var payload = secondPart.substring(0, idx);

        var sig = Util.base64(secondPart.substring(idx + 1));

        if (!SccEncryption.VerifySign(user.public_key, payload, sig))
            throw new SccException("Signature from handshake was not valid!");
        return payload;
    }

    private byte[] signAndEncryptPayload(String payload, local_user user) throws Exception {
        var sig = SccEncryption.Sign(db.keyPair.getPrivate(), payload);
        var sigStr = Util.base64(sig);
        var secondPartPlainText = payload + "|" + sigStr;
        return (SccEncryption.Encript(user.ephemeral_key_outgoing, secondPartPlainText));
    }

    public void handshakeWithFacebookId(long facebook_id) throws Exception {
        var user = db.getUserWithFacebookId(facebook_id);
        user.ephemeral_key_outgoing = SccEncryption.GenerateSymetricKey();
        var key = SccEncryption.serializeKey(user.ephemeral_key_outgoing);
        var encryptesSymetricalKey = Util.base64(SccEncryption.Encript(user.public_key, key));
        var json = new JSONObject();
        json.put("handshake_initiator_facebook_id", db.facebook_id);
        json.put("datetime", ZonedDateTime.now(ZoneOffset.UTC));
        var payload = json.toString();
        var secondPart = Util.base64(signAndEncryptPayload(payload, user));

        var message = encryptesSymetricalKey + "|" + secondPart;
        var params = new HashMap<String, String>();
        params.put("message", message);

        var result = Util.SyncRequestPost(new URL("http://localhost:5665/add_handshake"), params);
        db.updateUserInDb(user); // only set when webrequest succeeded
    }

    public void sendMessageToFacebookId(long facebook_id, String payload) throws Exception {
        var user = db.getUserWithFacebookId(facebook_id);

        var secondPart = Util.base64(signAndEncryptPayload(payload, user));
        var params = new HashMap<String, String>();
        params.put("message", secondPart);
        var result = Util.SyncRequestPost(new URL("http://localhost:5665/add_message"), params);
        System.out.println(result);
    }
}
