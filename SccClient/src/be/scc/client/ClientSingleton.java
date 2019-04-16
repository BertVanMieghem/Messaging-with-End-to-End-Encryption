package be.scc.client;

import be.scc.common.SccEncryption;
import be.scc.common.SccException;
import be.scc.common.Util;
import org.json.JSONObject;
import org.sqlite.util.StringUtils;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


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
        PullUsers();
        PullHandshakes();
        PullMessages();
        db.rebuildChannelsFromMessageCache();
        db.saveToDb();
    }

    public void PullHandshakes() throws Exception {
        URL url = new URL("http://localhost:5665/get_handshake_buffer?last_handshake_buffer_index="
                + ClientSingleton.inst().db.last_handshake_buffer_index);

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
                String payload = secondPart.substring(0, idx);
                var json = new JSONObject(payload);
                var from_facebook_id = json.getLong("handshake_initiator_facebook_id");
                var datetime = json.getString("datetime");

                var sig = Util.base64(secondPart.substring(idx + 1));

                var from_user = db.getUserWithFacebookId(from_facebook_id);
                if (!SccEncryption.VerifySign(from_user.public_key, payload, sig))
                    throw new SccException("Signature from handshake was not valid!");

                from_user.ephemeral_key_ingoing = symetricalKey;
                from_user.ephemeral_id_ingoing = UUID.fromString(json.getString("ephemeral_id"));
                db.updateUserInDb(from_user);


                h.client_can_decode = "YES";
            } catch (GeneralSecurityException ex) {
                h.client_can_decode = "NO";
            }
            db.insertHandshake(h);
            ClientSingleton.inst().db.last_handshake_buffer_index = h.id;
        }
        db.saveToDb();
    }

    public void PullMessages() throws Exception {
        URL url = new URL("http://localhost:5665/get_message_buffer?last_message_buffer_index="
                + ClientSingleton.inst().db.last_message_buffer_index);

        var params = new HashMap<String, String>();
        params.put("ephemeral_ids", StringUtils.join(db.GetIncomingEphemeralIds(), "|"));
        var jsonObj = new JSONObject(Util.SyncRequestPost(url, params));
        for (Object row : jsonObj.getJSONArray("message_buffer")) {
            var obj = (JSONObject) row;
            var h = new message_row();
            h.fillInFromJson(obj);
            try {

                var users = db.getUsersThatShookOurHands();
                for (var user : users) {
                    try {
                        var message = Util.base64(h.message);
                        String payload = decryptPayloadAndVerify(message, user);
                        var json = new JSONObject(payload);
                        var message_type = json.getString("message_type");
                        var content = json.getJSONObject("content");

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
            ClientSingleton.inst().db.last_message_buffer_index = h.id;
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
        user.ephemeral_id_outgoing = UUID.randomUUID();

        var key = SccEncryption.serializeKey(user.ephemeral_key_outgoing);
        var encryptesSymetricalKey = Util.base64(SccEncryption.Encript(user.public_key, key));
        var json = new JSONObject();
        json.put("handshake_initiator_facebook_id", db.facebook_id);
        json.put("ephemeral_id", user.ephemeral_id_outgoing);
        json.put("datetime", ZonedDateTime.now(ZoneOffset.UTC));
        var payload = json.toString();
        var secondPart = Util.base64(signAndEncryptPayload(payload, user));

        var message = encryptesSymetricalKey + "|" + secondPart;
        var params = new HashMap<String, String>();
        params.put("message", message);

        var result = Util.SyncRequestPost(new URL("http://localhost:5665/add_handshake"), params);
        db.updateUserInDb(user); // only set when webrequest succeeded
    }

    void sendMessageToFacebookId(long facebook_id, JSONObject jsonPayload) throws Exception {
        assert jsonPayload.get("message_type") != null;
        assert jsonPayload.get("content") != null;
        var payload = jsonPayload.toString();

        var user = db.getUserWithFacebookId(facebook_id);
        if (user.ephemeral_key_outgoing == null) {
            handshakeWithFacebookId(facebook_id);
            user = db.getUserWithFacebookId(facebook_id);
        }

        var secondPart = Util.base64(signAndEncryptPayload(payload, user));
        var params = new HashMap<String, String>();
        params.put("message", secondPart);
        params.put("target_ephemeral_id", user.ephemeral_id_outgoing.toString());
        var result = Util.SyncRequestPost(new URL("http://localhost:5665/add_message"), params);
        System.out.println(result);
    }

    void sendMessageToChannel(Channel ch, JSONObject json) throws Exception {
        // Send a copy to each member of the channel
        for (var member : ch.members) {
            sendMessageToFacebookId(member.facebook_id, json);
        }
        PullServerEvents();
    }

    void sendMessageToChannelMembers(Channel ch, JSONObject json) throws Exception {
        // Send a copy to each member of the channel
        for (var member : ch.members) {
            if (ch.hasMember(member.facebook_id)) // chat messages are only sent to joined members
                sendMessageToFacebookId(member.facebook_id, json);
        }
        PullServerEvents();
    }

    public void createNewChannel() throws Exception {
        var json = new JSONObject();
        json.put("message_type", "invite_to_channel");
        var jsonContent = new JSONObject();
        jsonContent.put("invited_facebook_id", db.facebook_id);

        var ch = new Channel();
        var mem = new ChannelMember();
        mem.status = MemberStatus.OWNER;
        mem.facebook_id = db.facebook_id;
        ch.members.add(mem);
        ch.uuid = UUID.randomUUID();
        ch.name = "Untitled Channel (" + ch.uuid + ")";
        jsonContent.put("channel_content", ch.toJson());

        json.put("content", jsonContent);

        sendMessageToFacebookId(db.facebook_id, json);
        PullServerEvents();
    }

    public void inviteUserToChannel(Channel ch, long invited_facebook_id) throws Exception {
        var json = new JSONObject();
        json.put("message_type", "invite_to_channel");
        var jsonContent = new JSONObject();
        jsonContent.put("invited_facebook_id", invited_facebook_id);

        var mem = new ChannelMember();
        mem.status = MemberStatus.INVITE_PENDING;
        mem.facebook_id = invited_facebook_id;
        ch.members.add(mem);
        // We don't show the history of the chat conversation.
        // It would be difficult to trust anyway.
        ch.chatMessages = new ArrayList<>();
        jsonContent.put("channel_content", ch.toJson());

        json.put("content", jsonContent);

        sendMessageToChannel(ch, json);
    }

    public void removeUserToChannel(Channel ch, long removed_facebook_id) throws Exception {
        var json = new JSONObject();
        json.put("message_type", "remove_person_from_channel");
        var jsonContent = new JSONObject();
        jsonContent.put("removed_facebook_id", removed_facebook_id);
        jsonContent.put("channel_uuid", ch.uuid);

        json.put("content", jsonContent);

        sendMessageToChannel(ch, json);
    }

    public void acceptInvite(Channel ch) throws Exception {
        var json = new JSONObject();
        json.put("message_type", "accept_invite_to_channel");
        var jsonContent = new JSONObject();
        jsonContent.put("channel_uuid", ch.uuid);
        // The accepting user is the owne that sends this message
        json.put("content", jsonContent);

        sendMessageToChannel(ch, json);
    }

    public void renameChannel(Channel ch, String new_channel_name) throws Exception {
        var json = new JSONObject();
        json.put("message_type", "rename_channel");
        var jsonContent = new JSONObject();
        jsonContent.put("channel_uuid", ch.uuid);
        jsonContent.put("new_channel_name", new_channel_name);
        json.put("content", jsonContent);

        sendMessageToChannel(ch, json);
    }
}
