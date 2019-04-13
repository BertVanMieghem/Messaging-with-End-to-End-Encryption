package be.scc.client;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.sql.*;
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
    public long facebook_id;

    public MemberStatus status = MemberStatus.NOT_SET;

    public JSONObject toJson() {
        var json = new JSONObject();
        json.put("facebook_id", facebook_id);
        json.put("status", status);
        return json;
    }
}

enum ChannelStatus {
    NOT_SET,
    //MADE_ON_THIS_COMPUTER,
    //GOT_FROM_REMOTE,
    ARCHIEVED,
}

class Channel {

    public ChannelStatus status = ChannelStatus.NOT_SET;

    public ChannelMember getMember(long facebook_id) {
        for (var m : members) {
            if (m.facebook_id == facebook_id)
                return m;
        }
        return null;
    }

    public ChannelMember getOrCreateMember(long facebook_id) {
        var mem = getMember(facebook_id);
        if (mem == null) {
            mem = new ChannelMember();
            mem.facebook_id = facebook_id;
            this.members.add(mem);
        }
        return mem;
    }

    public boolean hasMember(long facebook_id) {
        for (var m : members) {
            if (m.facebook_id == facebook_id) {
                if (m.status != MemberStatus.INVITE_PENDING
                        && m.status != MemberStatus.REMOVED)
                    return true;
            }
        }
        return false;
    }

    public boolean hasOwner(long facebook_id) {
        for (var m : members) {
            if (m.facebook_id == facebook_id) {
                if (m.status == MemberStatus.OWNER)
                    return true;
            }
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
            chatMessages.put(cm);
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
            m.facebook_id = jm.getLong("facebook_id");
            m.status = MemberStatus.valueOf(jm.getString("status"));
            ch.members.add(m);
        }

        ch.chatMessages = new ArrayList<>();
        var chatMessages = json.getJSONArray("chatMessages");
        for (var ocm : chatMessages) {
            var message = (String) ocm;
            ch.chatMessages.add(message);
        }

        return ch;
    }

    public UUID uuid;
    public String name;
    public List<ChannelMember> members = new ArrayList<>();
    public List<String> chatMessages = new ArrayList<>();
}

/**
 * CREATE TABLE `cached_messages` (
 * `id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * `from_facebook_id`	INTEGER NOT NULL,
 * `message`	TEXT NOT NULL
 * );
 */
class cached_message_row {
    public long id;
    public long from_facebook_id;
    public String message;

    public void fillInFromSqlResult(ResultSet result) throws SQLException {
        id = result.getLong("id");
        from_facebook_id = result.getLong("from_facebook_id");
        message = result.getString("message");
    }

    public String[] toStringList() {
        Object[] tmp = {id, from_facebook_id, message};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

    public final static String[] columnNames = {"id", "from_facebook_id", "message"};
}

class handshake_row {
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

class message_row extends handshake_row {

}

class local_user {
    public local_user() {
    }

    public int id;
    public long facebook_id;
    public String facebook_name;
    public PublicKey public_key;
    public SecretKey ephemeral_key_outgoing;
    public SecretKey ephemeral_key_ingoing;

    public String[] toStringList() {
        Object[] tmp = {id, facebook_id, facebook_name, public_key, ephemeral_key_outgoing, ephemeral_key_ingoing};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

    public final static String[] columnNames = {"id", "facebook_id", "facebook_name", "public_key", "ephemeral_key_outgoing", "ephemeral_key_ingoing"};
}