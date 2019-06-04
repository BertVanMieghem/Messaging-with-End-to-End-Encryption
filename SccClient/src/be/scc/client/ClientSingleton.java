package be.scc.client;

import be.scc.common.FacebookId;
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
import java.util.UUID;


public class ClientSingleton {

    public LoginDialog loginDialog;
    public ChatDialog chatDialog;
    public ClientDB db = new ClientDB();
    public static final String serverUrl = "http://localhost:5665";

    private static ClientSingleton single_instance = null;

    // private constructor restricted to this class itself
    private ClientSingleton() {
    }

    public void initialise() throws Exception {
        db.loadFromDb();
        loginDialog = new LoginDialog();

    }

    /**
     * static method to create instance of Singleton class
     */
    public static ClientSingleton inst() {
        if (single_instance == null)
            single_instance = new ClientSingleton();

        return single_instance;
    }


    public void openLoginOrSkip() throws IOException {
        if (ClientSingleton.inst().db.facebook_id != null) {
            ClientSingleton.inst().fromLoginToChatDialog();
        } else {
            loginDialog.pack();
            loginDialog.initialise();
            loginDialog.setVisible(true);
        }
    }

    public void fromLoginToChatDialog() {
        loginDialog.dispatchEvent(new WindowEvent(loginDialog, WindowEvent.WINDOW_CLOSING));

        chatDialog = new ChatDialog();
        chatDialog.pack();
        chatDialog.setVisible(true);
    }

    public void pullUsers() throws Exception {
        var last_user_index = db.getLargestUserId();
        URL url = new URL(serverUrl + "/get_users?last_user_index=" + last_user_index);

        var jsonObj = Util.syncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("users")) {
            var obj = (JSONObject) row;
            db.addUser(obj.getInt("id"),
                    FacebookId.fromString(obj.getString("facebook_id")),
                    obj.getString("facebook_name"),
                    SccEncryption.deserialisePublicKey(obj.getString("public_key")));
        }
        db.saveToDb();
    }

    public void pullServerEvents() throws Exception {
        pullUsers();
        pullHandshakes();
        pullMessages();
        db.rebuildChannelsFromMessageCache();
        db.saveToDb();
    }

    public void pullHandshakes() throws Exception {
        if (db.keyPair == null)
            return;
        URL url = new URL(serverUrl + "/get_handshake_buffer?last_handshake_buffer_index="
                + ClientSingleton.inst().db.last_handshake_buffer_index);

        var jsonObj = Util.syncJsonRequest(url);
        for (Object row : jsonObj.getJSONArray("handshake_buffer")) {
            var obj = (JSONObject) row;
            var h = new Handshake_row();
            h.fillInFromJson(obj);
            try {
                var parts = h.message.split("\\|");
                var encryptesSymetricalKeyString = parts[0];
                var encryptesSymetricalKey = Util.base64(encryptesSymetricalKeyString);
                var symetricalKeySerialised = SccEncryption.decrypt(db.keyPair.getPrivate(), encryptesSymetricalKey);
                var symetricalKey = SccEncryption.deserialiseSymetricKey(symetricalKeySerialised);

                byte[] secondPartEncrypted = Util.base64(parts[1]);
                var secondPart = SccEncryption.decrypt(symetricalKey, secondPartEncrypted);

                var idx = secondPart.lastIndexOf("|");
                String payload = secondPart.substring(0, idx);
                var json = new JSONObject(payload);
                var from_facebook_id = FacebookId.fromString(json.getString("handshake_initiator_facebook_id"));

                var sig = Util.base64(secondPart.substring(idx + 1));

                var from_user = db.getUserWithFacebookId(from_facebook_id);
                if (!SccEncryption.verifySign(from_user.public_key, payload, sig))
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

    public void pullMessages() throws Exception {
        URL url = new URL(serverUrl + "/get_message_buffer?last_message_buffer_index="
                + ClientSingleton.inst().db.last_message_buffer_index);

        var params = new HashMap<String, String>();
        params.put("ephemeral_ids", StringUtils.join(db.getIncomingEphemeralIds(), "|"));
        var jsonObj = new JSONObject(Util.syncRequestPost(url, params));
        for (Object row : jsonObj.getJSONArray("message_buffer")) {
            var obj = (JSONObject) row;
            var h = new Message_row();
            h.fillInFromJson(obj);
            try {

                var users = db.getUsersThatShookOurHands();
                for (var user : users) {
                    try {
                        var message = Util.base64(h.message);
                        String payload = decryptPayloadAndVerify(message, user);

                        var cm = new Cached_message_row();
                        cm.id = h.id;
                        cm.from_facebook_id = user.facebook_id;
                        cm.message = new JSONObject(payload);
                        db.insertCachedMessage(cm);

                        //var json = new JSONObject(payload);
                        //var message_type = json.getString("message_type");
                        //var content = json.getJSONObject("content");
                        //System.out.println("Got message:");
                        //System.out.println("  id: " + h.id);
                        //System.out.println("  message_type: " + message_type);
                        //System.out.println("  content: " + content);

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
            ClientSingleton.inst().db.last_message_buffer_index = Math.max(h.id, ClientSingleton.inst().db.last_message_buffer_index);
        }
        db.saveToDb();
    }

    /**
     * Can not be used when decrypting handshake, because we don't know what user to verify with then.
     */
    private String decryptPayloadAndVerify(byte[] secondPartEncrypted, Local_user user) throws Exception {
        var secondPart = SccEncryption.decrypt(user.ephemeral_key_ingoing, secondPartEncrypted);

        var idx = secondPart.lastIndexOf("|");
        var payload = secondPart.substring(0, idx);

        var sig = Util.base64(secondPart.substring(idx + 1));

        if (!SccEncryption.verifySign(user.public_key, payload, sig))
            throw new SccException("Signature from handshake was not valid!");
        return payload;
    }

    private byte[] signAndEncryptPayload(String payload, Local_user user) throws Exception {
        var sig = SccEncryption.sign(db.keyPair.getPrivate(), payload);
        var sigStr = Util.base64(sig);
        var secondPartPlainText = payload + "|" + sigStr;
        return (SccEncryption.encrypt(user.ephemeral_key_outgoing, secondPartPlainText));
    }

    public void handshakeWithFacebookId(FacebookId facebook_id) throws Exception {
        var user = db.getUserWithFacebookId(facebook_id);
        user.ephemeral_key_outgoing = SccEncryption.generateSymetricKey();
        user.ephemeral_id_outgoing = UUID.randomUUID();

        var key = SccEncryption.serializeKey(user.ephemeral_key_outgoing);
        var encryptesSymetricalKey = Util.base64(SccEncryption.encrypt(user.public_key, key));
        var json = new JSONObject();
        json.put("handshake_initiator_facebook_id", db.facebook_id);
        json.put("ephemeral_id", user.ephemeral_id_outgoing);
        json.put("datetime", ZonedDateTime.now(ZoneOffset.UTC));
        var payload = json.toString();
        var secondPart = Util.base64(signAndEncryptPayload(payload, user));

        var message = encryptesSymetricalKey + "|" + secondPart;
        var params = new HashMap<String, String>();
        params.put("message", message);

        Util.syncRequestPost(new URL(serverUrl + "/add_handshake"), params);
        db.updateUserInDb(user); // only set when webrequest succeeded
    }

    public void sendMessageToFacebookId(FacebookId facebook_id, JSONObject jsonPayload) throws Exception {
        assert jsonPayload.get("message_type") != null;
        assert jsonPayload.get("content") != null;
        jsonPayload.put("sent_time", ZonedDateTime.now(ZoneOffset.UTC).toString());
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
        var result = Util.syncRequestPost(new URL(serverUrl + "/add_message"), params);
        System.out.println("[sendMessageToFacebookId]" + result);
    }

    public void sendMessageToChannel(Channel ch, JSONObject json) throws Exception {
        // Send a copy to each member of the channel
        for (var member : ch.members) {
            sendMessageToFacebookId(member.facebook_id, json);
        }
        pullServerEvents();
    }

    public void sendMessageToChannelMembers(Channel ch, JSONObject json) throws Exception {
        // Send a copy to each member of the channel
        for (var member : ch.members) {
            if (ch.hasMember(member.facebook_id)) // chat messages are only sent to joined members
                sendMessageToFacebookId(member.facebook_id, json);
        }
        pullServerEvents();
    }

    // File Transfer
    public void sendFileToFacebookId(FacebookId facebook_id, JSONObject jsonPayload) throws Exception {
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
        params.put("file", secondPart);
        params.put("target_ephemeral_id", user.ephemeral_id_outgoing.toString());
        var result = Util.syncRequestPost(new URL(serverUrl + "/add_file"), params);
        System.out.println("[sendFileToFacebookId]" + result);
    }

    public void sendFileToChannelMembers(Channel ch, JSONObject json) throws Exception {
        // Send a copy to each member of the channel
        for (var member : ch.members) {
            if (ch.hasMember(member.facebook_id)) // chat messages are only sent to joined members
                sendFileToFacebookId(member.facebook_id, json);
        }
        pullServerEvents();
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
        pullServerEvents();
    }

    public void inviteUserToChannel(Channel ch, FacebookId invited_facebook_id) throws Exception {
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

    public void removeUserToChannel(Channel ch, FacebookId removed_facebook_id) throws Exception {
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
