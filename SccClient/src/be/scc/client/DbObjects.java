package be.scc.client;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.PublicKey;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

enum MemberStatus {
    NOT_SET,
    OWNER,
    INVITE_PENDING,
    MEMBER,
    REMOVED
}

class ChannelMember {
    public FacebookId facebook_id;

    public MemberStatus status = MemberStatus.NOT_SET;

    public JSONObject toJson() {
        var json = new JSONObject();
        json.put("facebook_id", facebook_id);
        json.put("status", status);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelMember that = (ChannelMember) o;
        return Objects.equals(facebook_id, that.facebook_id) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(facebook_id, status);
    }
}

enum ChannelStatus {
    NOT_SET,
    //MADE_ON_THIS_COMPUTER,
    //GOT_FROM_REMOTE,
    ARCHIEVED,
}

class ChatMessage { // superclass
    public String message;
    public ZonedDateTime date;
    public FacebookId from_facebook_id;
    private boolean isTrusted = true;
    public final static String[] columnNames = {"message", "date", "from_facebook_id"};


    public JSONObject toJson() {
        var json = new JSONObject();
        json.put("message", message);
        json.put("date", date.toString());
        json.put("from_facebook_id", from_facebook_id);
        return json;
    }

    public static ChatMessage fromJson(JSONObject json) {
        var cm = new ChatMessage();
        cm.message = json.getString("message");
        cm.date = ZonedDateTime.parse(json.getString("date"));
        cm.from_facebook_id = new FacebookId(json.getLong("from_facebook_id"));
        cm.isTrusted = false;
        return cm;
    }


    public String[] toStringList() {
        Object[] tmp = {message, date, from_facebook_id};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return isTrusted == that.isTrusted &&
                Objects.equals(message, that.message) &&
                Objects.equals(date, that.date) &&
                Objects.equals(from_facebook_id, that.from_facebook_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, date, from_facebook_id, isTrusted);
    }
}

class FileMessage {
    public String file;
    public String fileName;
    public String extension;
    public ZonedDateTime date;
    public FacebookId from_facebook_id;
    private boolean isTrusted = true;
    public final static String[] columnNames = {"fileName", "date", "from_facebook_id"};


    public JSONObject toJson() {
        var json = new JSONObject();
        json.put("file", file);
        json.put("date", date.toString());
        json.put("from_facebook_id", from_facebook_id);
        return json;
    }

    public static FileMessage fromJson(JSONObject json) {
        var fm = new FileMessage();
        fm.file = json.getString("file");
        fm.date = ZonedDateTime.parse(json.getString("date"));
        fm.from_facebook_id = new FacebookId(json.getLong("from_facebook_id"));
        fm.isTrusted = false;
        return fm;
    }


    public String[] toStringList() {
        Object[] tmp = {file, date, from_facebook_id};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMessage that = (FileMessage) o;
        return isTrusted == that.isTrusted &&
                Objects.equals(file, that.file) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(extension, that.extension) &&
                Objects.equals(date, that.date) &&
                Objects.equals(from_facebook_id, that.from_facebook_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, fileName, extension, date, from_facebook_id, isTrusted);
    }
}

class Channel {

    public UUID uuid;
    public String name;
    public List<ChannelMember> members = new ArrayList<>();
    public List<ChatMessage> chatMessages = new ArrayList<>();
    public List<FileMessage> fileMessages = new ArrayList<>();
    public ChannelStatus status = ChannelStatus.NOT_SET;

    public ChannelMember getMember(FacebookId facebook_id) {
        for (var m : members) {
            if (m.facebook_id.equals(facebook_id))
                return m;
        }
        return null;
    }

    public ChannelMember getOrCreateMember(FacebookId facebook_id) {
        var mem = getMember(facebook_id);
        if (mem == null) {
            mem = new ChannelMember();
            mem.facebook_id = facebook_id;
            this.members.add(mem);
        }
        return mem;
    }

    public boolean hasMember(FacebookId facebook_id) {
        for (var m : members) {
            if (m.facebook_id.equals(facebook_id) &&
                    (m.status != MemberStatus.INVITE_PENDING && m.status != MemberStatus.REMOVED))
                return true;
        }
        return false;
    }

    public boolean hasOwner(FacebookId facebook_id) {
        for (var m : members) {
            if ((m.facebook_id.equals(facebook_id)) && (m.status == MemberStatus.OWNER))
                return true;
        }
        return false;
    }

    public JSONObject toJson() {
        var json = new JSONObject();

        json.put("uuid", uuid.toString());
        json.put("name", name);

        var members = new JSONArray();
        for (var mem : this.members) {
            members.put(mem.toJson());
        }
        json.put("members", members);

        var chatMessages = new JSONArray();
        for (var cm : this.chatMessages) {
            chatMessages.put(cm.toJson());
        }
        json.put("chatMessages", chatMessages);

        return json;
    }

    /**
     * I read that java serialisation is dangerous.
     * So instead of figuring it out, serialisation is done explicitly.
     */
    public static Channel fromJson(JSONObject json) {
        var ch = new Channel();
        ch.uuid = UUID.fromString(json.getString("uuid"));
        ch.name = json.getString("name");

        JSONArray members = json.getJSONArray("members");
        ch.members = new ArrayList<>();
        for (Object om : members) {
            var jm = (JSONObject) om;
            var m = new ChannelMember();
            m.facebook_id = new FacebookId(jm.getLong("facebook_id"));
            m.status = MemberStatus.valueOf(jm.getString("status"));
            ch.members.add(m);
        }

        ch.chatMessages = new ArrayList<>();
        var chatMessages = json.getJSONArray("chatMessages");
        for (var ocm : chatMessages) {
            var jcm = (JSONObject) ocm;
            var cm = ChatMessage.fromJson(jcm);
            ch.chatMessages.add(cm);
        }

        return ch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equals(uuid, channel.uuid) &&
                Objects.equals(name, channel.name) &&
                Objects.equals(members, channel.members) &&
                Objects.equals(chatMessages, channel.chatMessages) &&
                Objects.equals(fileMessages, channel.fileMessages) &&
                status == channel.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, members, chatMessages, fileMessages, status);
    }
}

/**
 * CREATE TABLE `cached_messages` (
 * `id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * `from_facebook_id`	INTEGER NOT NULL,
 * `message`	TEXT NOT NULL
 * );
 */
class Cached_message_row {
    public long id;
    public FacebookId from_facebook_id;
    public JSONObject message;
    public final static String[] columnNames = {"id", "from_facebook_id", "message"};

    public void fillInFromSqlResult(ResultSet result) throws SQLException {
        id = result.getLong("id");
        from_facebook_id = new FacebookId(result.getLong("from_facebook_id"));
        message = new JSONObject(result.getString("message"));
    }

    public String[] toStringList() {
        Object[] tmp = {id, from_facebook_id, message.toString()};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

}

class Handshake_row {
    public long id;
    public String message;
    public String client_can_decode;

    public void fillInFromJson(JSONObject row) {
        id = row.getInt("id");
        message = row.getString("message");
        if (row.has("client_can_decode"))
            client_can_decode = row.getString("client_can_decode");
    }
}

class Message_row extends Handshake_row {

}

class Local_user {

    public long id;
    public FacebookId facebook_id;
    public String facebook_name;
    public PublicKey public_key;
    public SecretKey ephemeral_key_outgoing;
    public SecretKey ephemeral_key_ingoing;
    public UUID ephemeral_id_outgoing;
    public UUID ephemeral_id_ingoing;
    public final static String[] columnNames = {"id", "facebook_id", "facebook_name", "public_key", "ephemeral_key_outgoing", "ephemeral_key_ingoing",
            "ephemeral_id_outgoing", "ephemeral_id_ingoing"};

    public String[] toStringList() {
        Object[] tmp = {id, facebook_id, facebook_name, public_key, ephemeral_key_outgoing, ephemeral_key_ingoing, ephemeral_id_outgoing, ephemeral_id_ingoing};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }
}

class FacebookId {
    public FacebookId(long facebook_id) {
        assert facebook_id > 0;
        this.facebook_id = facebook_id;
    }

    private long facebook_id;

    @Override
    public String toString() {
        return "" + facebook_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacebookId that = (FacebookId) o;
        return facebook_id == that.facebook_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(facebook_id);
    }
}