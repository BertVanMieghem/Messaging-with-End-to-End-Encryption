package be.scc.server;

import be.scc.common.FacebookId;
import be.scc.common.SccEncryption;
import be.scc.common.Util;
import org.json.*;
import org.sqlite.util.StringUtils;

import java.security.PublicKey;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

class DbSingleton {
    private static DbSingleton single_instance = null;

    private Connection conn = null;

    // private constructor restricted to this class itself
    private DbSingleton() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:SccServer.sqlite");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    // static method to create instance of Singleton class
    public static DbSingleton inst() {
        if (single_instance == null)
            single_instance = new DbSingleton();

        return single_instance;
    }


    public void insertUser(FacebookId facebook_id, String facebook_name, PublicKey public_key) throws SQLException {
        var public_key_str = SccEncryption.serializeKey(public_key);

        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?, ?)");
        var i = 0;
        pstmt.setString(++i, facebook_id.toString());
        pstmt.setString(++i, facebook_name);
        pstmt.setString(++i, public_key_str);
        pstmt.executeUpdate();
    }

    public JSONObject getAllUsers(int last_user_index) throws SQLException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from users WHERE id>" + last_user_index);
        //var collumnNames = List.of("id", "facebook_id", "public_key");
        var jsonArr = Util.sqlResultsToJson(result); // Information on the server is considered public, so just get all properties

        var rootJson = new JSONObject();
        rootJson.put("users", jsonArr);
        return rootJson;
    }


    public void addHandshake(String message) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO handshake_buffer VALUES (NULL, ?)");
        pstmt.setString(1, message);
        pstmt.executeUpdate();
    }

    public JSONObject getHandshakes(int last_handshake_buffer_index) throws SQLException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * FROM handshake_buffer WHERE id>" + last_handshake_buffer_index);

        var jsonArr = Util.sqlResultsToJson(result); //, List.of("id", "message"));

        var rootJson = new JSONObject();
        rootJson.put("handshake_buffer", jsonArr);
        return rootJson;
    }


    public void addMessage(String message, UUID target_ephemeral_id) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO message_buffer VALUES (NULL, ?, ?)");
        pstmt.setString(1, message);
        pstmt.setString(2, target_ephemeral_id.toString());
        pstmt.executeUpdate();
    }

    public JSONObject getMessages(int last_message_buffer_index, String[] ephemeral_ids) throws SQLException {
        Statement statement = conn.createStatement();
        var temp = StringUtils.join((Arrays.stream(ephemeral_ids).map(e -> "\"" + e + "\"").collect(Collectors.toList())), ", ");
        ResultSet result = statement.executeQuery("SELECT * FROM message_buffer WHERE id>" + last_message_buffer_index + " AND target_ephemeral_id IN (" + temp + ")");

        var jsonArr = Util.sqlResultsToJson(result); //, List.of("id", "message"));

        var rootJson = new JSONObject();
        rootJson.put("message_buffer", jsonArr);
        return rootJson;
    }
}
