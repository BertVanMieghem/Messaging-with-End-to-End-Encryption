package be.scc.client;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DbObjectsTest {

    @org.junit.jupiter.api.Test
    void channel() {
        var ch = new Channel();
        ch.name = "test channel name";
        ch.uuid = UUID.randomUUID();
        ch.members = new ArrayList<>();
        var mem = new ChannelMember();
        mem.facebook_id = 654354351;
        mem.status = "OWNER";
        ch.members.add(mem);
        ch.chatMessages = new ArrayList<>();
        ch.chatMessages.add("First message");
        ch.chatMessages.add("Second message");

        var json = ch.toJson().toString();
        var ch2 = Channel.fromJson(new JSONObject(json));
        var json2 = ch2.toJson();

        assert json.equals(json2.toString());
    }
}