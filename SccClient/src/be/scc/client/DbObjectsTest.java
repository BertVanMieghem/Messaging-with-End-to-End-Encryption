package be.scc.client;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

class DbObjectsTest {

    @org.junit.jupiter.api.Test
    void channel() {
        var ch = new Channel();
        ch.name = "test channel name";
        ch.uuid = UUID.randomUUID();
        ch.members = new ArrayList<>();

        var mOwner = new ChannelMember();
        mOwner.facebook_id = 654354351;
        mOwner.status = MemberStatus.OWNER;
        ch.members.add(mOwner);

        var mMemeber = new ChannelMember();
        mMemeber.facebook_id = 879789456;
        mMemeber.status = MemberStatus.MEMBER;
        ch.members.add(mMemeber);

        var mPending = new ChannelMember();
        mPending.facebook_id = 23147844;
        mPending.status = MemberStatus.INVITE_PENDING;
        ch.members.add(mPending);

        ch.chatMessages = new ArrayList<>();
        ch.chatMessages.add("First message");
        ch.chatMessages.add("Second message");

        assert ch.hasOwner(654354351);
        assert ch.hasMember(654354351);
        assert ch.hasMember(879789456);
        assert !ch.hasOwner(0);
        assert !ch.hasMember(0);


        var json = ch.toJson().toString();
        var ch2 = Channel.fromJson(new JSONObject(json));
        var json2 = ch2.toJson();

        assert json.equals(json2.toString());
    }
}