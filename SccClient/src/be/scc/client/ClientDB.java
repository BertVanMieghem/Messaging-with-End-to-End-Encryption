package be.scc.client;

import be.scc.common.FacebookId;
import be.scc.common.SccException;
import be.scc.common.Util;
import be.scc.common.SccEncryption;
import org.json.JSONObject;

import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class ClientDB {

    private Connection conn = null;
    private Collection<Channel> buildedChannels;
    // Don't forget to save to database when changing these properties:
    public long facebook_id_long = 0;
    public FacebookId facebook_id = null;
    public KeyPair keyPair;
    public long last_handshake_buffer_index = 0;
    public long last_message_buffer_index = 0;
    public SccDispatcher dispatcher = new SccDispatcher();


    // private constructor restricted to this class itself
    public ClientDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            var popup = new SelectDatabase();
            var files = Util.getFilesFromDirectory(Path.of("db"));
            popup.initialise(files);
            popup.pack();
            popup.setVisible(true);
            var result = popup.getSelected();
            if (result == null || result.equals("")) throw new SccException("No db path was selected!");
            conn = DriverManager.getConnection("jdbc:sqlite:" + result); // db/SccClient.sqlite
            System.out.println("Opened database successfully: " + result);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }


    public int getLargestUserId() throws SQLException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("select MAX(id) from local_users;");
        return result.getInt(1);

    }

    public void addUser(int id, FacebookId facebook_id, String facebook_name, RSAPublicKey public_key) throws Exception {
        var user = getUserWithFacebookId(facebook_id);
        if (user == null)
            user = new Local_user();

        user.id = id;
        user.facebook_id = facebook_id;
        user.facebook_name = facebook_name;
        user.public_key = public_key;

        PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO local_users VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        var i = 0;
        pstmt.setLong(++i, user.id);
        pstmt.setString(++i, user.facebook_id == null ? null : user.facebook_id.toString());
        pstmt.setString(++i, user.facebook_name);
        pstmt.setString(++i, SccEncryption.serializeKey(user.public_key));
        pstmt.setString(++i, SccEncryption.serializeKey(user.ephemeral_key_outgoing));
        pstmt.setString(++i, SccEncryption.serializeKey(user.ephemeral_key_ingoing));
        String user_ephemeral_id_outgoing = null;
        if (user.ephemeral_id_outgoing != null)
            user_ephemeral_id_outgoing = user.ephemeral_id_outgoing.toString();
        String user_ephemeral_id_ingoing = null;
        if (user.ephemeral_id_ingoing != null)
            user_ephemeral_id_ingoing = user.ephemeral_id_ingoing.toString();
        pstmt.setString(++i, user_ephemeral_id_outgoing);
        pstmt.setString(++i, user_ephemeral_id_ingoing);
        pstmt.executeUpdate();

        dispatcher.sccDispatchModelChanged();
    }

    public void updateUserInDb(Local_user user) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO local_users VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        var i = 0;
        pstmt.setLong(++i, user.id);
        pstmt.setString(++i, user.facebook_id == null ? null : user.facebook_id.toString());
        pstmt.setString(++i, user.facebook_name);
        pstmt.setString(++i, SccEncryption.serializeKey(user.public_key));
        pstmt.setString(++i, SccEncryption.serializeKey(user.ephemeral_key_outgoing));
        pstmt.setString(++i, SccEncryption.serializeKey(user.ephemeral_key_ingoing));
        String user_ephemeral_id_outgoing = null;
        if (user.ephemeral_id_outgoing != null)
            user_ephemeral_id_outgoing = user.ephemeral_id_outgoing.toString();
        String user_ephemeral_id_ingoing = null;
        if (user.ephemeral_id_ingoing != null)
            user_ephemeral_id_ingoing = user.ephemeral_id_ingoing.toString();
        pstmt.setString(++i, user_ephemeral_id_outgoing);
        pstmt.setString(++i, user_ephemeral_id_ingoing);
        pstmt.executeUpdate();
        dispatcher.sccDispatchModelChanged();
    }

    public void insertFacebookFriends(List<FacebookFriendRow> friends) throws SQLException {
        for (var friend : friends) {
            PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO facebook_friends VALUES (?, ?)");
            var i = 0;
            pstmt.setLong(++i, friend.facebook_id_long);
            pstmt.setString(++i, friend.facebook_name);
            pstmt.executeUpdate();
        }
        dispatcher.sccDispatchModelChanged();
    }

    public List<FacebookFriendRow> getFacebookFriends() {

        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT * from facebook_friends");

            var aggregate = new ArrayList<FacebookFriendRow>();
            while (result.next()) {
                aggregate.add(FacebookFriendRow.parse(result));
            }
            return aggregate;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveToDb() throws SQLException {
        String private_key = null;
        String public_key = null;

        if (this.keyPair != null) {
            private_key = SccEncryption.serializeKey(this.keyPair.getPrivate());
            public_key = SccEncryption.serializeKey(this.keyPair.getPublic());
        }

        PreparedStatement pstmt = conn.prepareStatement("UPDATE single_row SET " +
                "facebook_id_long=?," +
                "facebook_id=?," +
                "private_key=?," +
                "public_key=?," +
                "last_handshake_buffer_index=?," +
                "last_message_buffer_index=?" +
                "");
        var i = 0;
        pstmt.setLong(++i, facebook_id_long);
        pstmt.setString(++i, facebook_id == null ? null : facebook_id.toString());
        pstmt.setString(++i, private_key);
        pstmt.setString(++i, public_key);
        pstmt.setLong(++i, last_handshake_buffer_index);
        pstmt.setLong(++i, last_message_buffer_index);
        pstmt.executeUpdate();

    }

    public void loadFromDb() throws SQLException, GeneralSecurityException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from single_row");
        if (result.isClosed()) {
            Statement statement2 = conn.createStatement();
            statement2.execute("INSERT INTO single_row VALUES (0, NULL, NULL, NULL, 0, 0)");
            saveToDb();
            return;
        }
        var facebook_id_long = result.getLong("facebook_id_long");
        var facebook_id = FacebookId.fromString(result.getString("facebook_id"));
        assert Objects.equals(facebook_id, FacebookId.doSlowHash(facebook_id_long));

        var private_key = result.getString("private_key");
        var public_key = result.getString("public_key");
        KeyPair pair = null;
        if (public_key != null && private_key != null)
            pair = new KeyPair(SccEncryption.deserialisePublicKey(public_key), SccEncryption.deserialisePrivateKey(private_key));

        var last_handshake_buffer_index = result.getLong("last_handshake_buffer_index");
        var last_message_buffer_index = result.getLong("last_message_buffer_index");

        this.facebook_id_long = facebook_id_long;
        this.facebook_id = facebook_id;
        this.keyPair = pair;
        this.last_handshake_buffer_index = last_handshake_buffer_index;
        this.last_message_buffer_index = last_message_buffer_index;
    }

    public List<Local_user> getUsers() {

        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT * from local_users");

            var aggregate = new ArrayList<Local_user>();
            while (result.next()) {
                aggregate.add(getUserFromResultRow(result));
            }
            return aggregate;
        } catch (SQLException | GeneralSecurityException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public List<String> getIncomingEphemeralIds() throws SQLException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT ephemeral_id_ingoing FROM local_users WHERE ephemeral_id_ingoing IS NOT NULL");
        var strlist = new ArrayList<String>();
        if (result.isClosed()) return strlist;
        while (result.next()) {
            var temp = result.getString("ephemeral_id_ingoing");
            strlist.add(temp);
        }
        return strlist;
    }
//SELECT ephemeral_id_outgoing FROM local_users WHERE ephemeral_id_outgoing IS NOT NULL

    public Local_user getUserWithFacebookId(FacebookId facebook_id) throws SQLException, GeneralSecurityException {
        if (facebook_id == null) throw new SccException("facebook_id is null!");
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from local_users WHERE facebook_id=\"" + facebook_id + "\"");
        if (result.isClosed()) return null;
        return getUserFromResultRow(result);
    }

    public Local_user getUserFromResultRow(ResultSet result) throws SQLException, GeneralSecurityException {
        var row = new Local_user();
        row.id = result.getInt("id");
        row.facebook_id = FacebookId.fromString(result.getString("facebook_id"));
        row.facebook_name = result.getString("facebook_name");

        var public_key = result.getString("public_key");
        if (public_key != null) row.public_key = SccEncryption.deserialisePublicKey(public_key);

        var ephemeral_key_outgoing = result.getString("ephemeral_key_outgoing");
        if (ephemeral_key_outgoing != null)
            row.ephemeral_key_outgoing = SccEncryption.deserialiseSymetricKey(ephemeral_key_outgoing);

        var ephemeral_key_ingoing = result.getString("ephemeral_key_ingoing");
        if (ephemeral_key_ingoing != null)
            row.ephemeral_key_ingoing = SccEncryption.deserialiseSymetricKey(ephemeral_key_ingoing);

        var ephemeral_id_outgoing = result.getString("ephemeral_id_outgoing");
        if (ephemeral_id_outgoing != null) row.ephemeral_id_outgoing = UUID.fromString(ephemeral_id_outgoing);

        var ephemeral_id_ingoing = result.getString("ephemeral_id_ingoing");
        if (ephemeral_id_ingoing != null) row.ephemeral_id_ingoing = UUID.fromString(ephemeral_id_ingoing);
        return row;
    }

    public void insertHandshake(Handshake_row row) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO handshake_buffer VALUES (?, ?, ?)");
        var i = 0;
        pstmt.setLong(++i, row.id);
        pstmt.setString(++i, row.message);
        pstmt.setString(++i, row.client_can_decode);
        pstmt.executeUpdate();

        dispatcher.sccDispatchModelChanged();
    }

    public List<Local_user> getUsersThatShookOurHands() {
        var lst = getUsers().stream().filter(u -> u.ephemeral_key_ingoing != null).collect(Collectors.toList());
        return lst;
    }
/*
    public List<Tuple<Long, SecretKeySpec>> getIncomingSymetricalKeys() throws Exception {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT facebook_id, ephemeral_key_ingoing FROM local_users;");
        var lst = new ArrayList<Tuple<Long, SecretKeySpec>>();
        while (result.next()) {
            var keyStr = result.getString("ephemeral_key_ingoing");
            SecretKeySpec key = SccEncryption.deserialiseSymetricKey(keyStr);
            lst.add(new Tuple<>(result.getLong("facebook_id"), key));
        }
        return lst;
    }
    */

    public void insertMessage(Message_row row) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO message_buffer VALUES (?, ?, ?)");
        var i = 0;
        pstmt.setLong(++i, row.id);
        pstmt.setString(++i, row.message);
        pstmt.setString(++i, row.client_can_decode);
        pstmt.executeUpdate();

        dispatcher.sccDispatchModelChanged();
    }

    public void insertCachedMessage(Cached_message_row row) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO cached_messages VALUES (?, ?, ?)");
        var i = 0;
        pstmt.setLong(++i, row.id);
        pstmt.setString(++i, row.from_facebook_id == null ? null : row.from_facebook_id.toString());
        pstmt.setString(++i, row.message.toString());
        pstmt.executeUpdate();

        dispatcher.sccDispatchModelChanged();
        dispatcher.sccDispatchModelChanged();
    }

    public List<Cached_message_row> getMessagesForFacebookId(long facebook_id) {
        var aggregate = new ArrayList<Cached_message_row>();
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT * from cached_messages WHERE from_facebook_id=" + facebook_id);

            while (result.next()) {
                var row = new Cached_message_row();
                row.fillInFromSqlResult(result);
                aggregate.add(row);
            }
        } catch (Exception ex) {
            System.exit(1); // fail
        }
        return aggregate;
    }

    public List<Cached_message_row> getAllCachedMessages() {
        var aggregate = new ArrayList<Cached_message_row>();
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT * from cached_messages");

            while (result.next()) {
                var row = new Cached_message_row();
                row.fillInFromSqlResult(result);
                aggregate.add(row);
            }
        } catch (Exception ex) {
            System.exit(1); // fail
        }
        return aggregate;
    }

    public Collection<Channel> getBuildedChannels() {
        if (buildedChannels == null)
            rebuildChannelsFromMessageCache();
        return buildedChannels;
    }


    public void rebuildChannelsFromMessageCache() {
        var tmpBuildedChannels = buildChannelsFromMessageCache();
        if (!Util.listEqualsIgnoreOrder(tmpBuildedChannels, buildedChannels)) {
            buildedChannels = tmpBuildedChannels;
            dispatcher.sccDispatchModelChanged();
        }
    }

    public Channel getChannelByUuid(UUID uuid) {
        for (var ch : getBuildedChannels()) {
            if (ch.uuid.equals(uuid))
                return ch;
        }
        return null;
    }

    private Collection<Channel> buildChannelsFromMessageCache() {
        var messages = getAllCachedMessages();

        var channels = new HashMap<UUID, Channel>();

        for (Cached_message_row messageRow : messages) {

            String message_type = messageRow.message.getString("message_type");
            JSONObject content = messageRow.message.getJSONObject("content");
            ZonedDateTime sent_time = ZonedDateTime.parse(messageRow.message.getString("sent_time"));

            switch (message_type) {

                // Is also used to create initial channel
                case "invite_to_channel": {
                    var invited_facebook_id = FacebookId.fromString(content.getString("invited_facebook_id"));
                    var remoteChannel = Channel.fromJson(content.getJSONObject("channel_content"));
                    channels.putIfAbsent(remoteChannel.uuid, remoteChannel);

                    // Assure the the member's status is INVITE_PENDING
                    // - if initial request, the sender could already have added the invited to the channel. Else it will be fixed here
                    // - If we were already member, we don't use the received channel representation, but we only update our local representattion
                    var ch = channels.get(remoteChannel.uuid);
                    if (ch.hasOwner(messageRow.from_facebook_id)) {
                        var mem = ch.getOrCreateMember(invited_facebook_id);
                        if (!ch.hasOwner(invited_facebook_id)) // Ignore the owner inviting himself. This is for initial creation.
                            mem.status = MemberStatus.INVITE_PENDING;
                    } else
                        System.err.println("User may not invite to channel! facebook_id:" + messageRow.from_facebook_id);
                    break;
                }

                case "remove_person_from_channel": {
                    var removed_facebook_id = FacebookId.fromString(content.getString("removed_facebook_id"));
                    var channel_uuid = UUID.fromString(content.getString("channel_uuid"));
                    var ch = channels.get(channel_uuid);

                    if (ch.hasOwner(messageRow.from_facebook_id) || messageRow.from_facebook_id.equals(removed_facebook_id)) {
                        // If the last OWNER leaves the channel, no one can invite again.
                        if (removed_facebook_id.equals(facebook_id)) {
                            ch.status = ChannelStatus.ARCHIEVED;
                        }
                        // No one will send use messages.
                        // ATM, we keep the stored messages, as it is difficult to remove them from event sourcing.
                        ch.getMember(removed_facebook_id).status = MemberStatus.REMOVED;
                    } else
                        System.err.println("User may remove this person from the channel! facebook_id:" + messageRow.from_facebook_id + " removed_facebook_id:" + removed_facebook_id);
                    break;
                }
                case "accept_invite_to_channel": {
                    var channel_uuid = UUID.fromString(content.getString("channel_uuid"));
                    var ch = channels.get(channel_uuid);
                    ch.getMember(messageRow.from_facebook_id).status = MemberStatus.MEMBER;
                    break;
                }
                case "rename_channel": {
                    var new_channel_name = content.getString("new_channel_name");
                    var channel_uuid = UUID.fromString(content.getString("channel_uuid"));
                    var ch = channels.get(channel_uuid);
                    if (ch.hasOwner(messageRow.from_facebook_id)) {
                        ch.name = new_channel_name;
                    } else
                        System.err.println("User may not rename this channel! facebook_id:" + messageRow.from_facebook_id);
                    break;
                }
                case "chat_message_to_channel": {
                    try {
                        var chat_message = content.getString("chat_message");
                        UUID uuid = UUID.fromString(content.getString("channel_uuid"));
                        var ch = channels.get(uuid);
                        if (ch.hasMember(messageRow.from_facebook_id)) {
                            var cm = new ChatMessage();
                            cm.message = chat_message;
                            cm.from_facebook_id = messageRow.from_facebook_id;
                            cm.date = sent_time;
                            ch.chatMessages.add(cm);
                        } else
                            System.err.println("User not in channel! facebook_id:" + messageRow.from_facebook_id);
                    } catch (Exception e) {
                        System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    }
                    break;
                }
                case "file_message_to_channel": {
                    var file_content = content.get("file_content").toString();
                    var file_name = content.get("file_name").toString();
                    UUID uuid = UUID.fromString(content.getString("channel_uuid"));
                    var ch = channels.get(uuid);
                    if (ch.hasMember(messageRow.from_facebook_id)) {
                        var fm = new FileMessage();
                        fm.file_content = file_content;
                        fm.file_name = file_name;
                        fm.from_facebook_id = messageRow.from_facebook_id;
                        fm.date = sent_time;
                        ch.fileMessages.add(fm);
                    } else
                        System.err.println("User not in channel! facebook_id:" + messageRow.from_facebook_id);
                    break;
                }
                default:
                    System.err.println("[buildChannelsFromMessageCache] Switch cases exhausted");
                    break;
            }
        }

        return channels.values();
    }
}

/*
class Tuple<X, Y> {
    public final X x;
    public final Y y;

    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}
*/
