package be.scc.server;

import be.scc.common.SccEncryption;
import org.json.*;

import java.security.PublicKey;
import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DbSingleton {
    private static DbSingleton single_instance = null;

    Connection conn = null;

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

    public void InsertUser(long facebook_id, PublicKey public_key) throws SQLException {
        var public_key_str = SccEncryption.serializeKey(public_key);

        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        pstmt.setLong(1, facebook_id);
        pstmt.setString(2, public_key_str);
        pstmt.executeUpdate();
    }

    public int insertHandshake(String message) throws SQLException {

        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO handshake_buffer VALUES (NULL, ?)");
        pstmt.setString(1, message);
        pstmt.executeUpdate();
        return 1;
    }

    public JSONObject GetHandshakes(int last_handshake_buffer_index) throws SQLException {

        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * FROM handshake_buffer WHERE id>" + last_handshake_buffer_index);
        var collumnNames = List.of("id", "message");

        var jsonArr = new JSONArray();
        while (result.next()) {
            var jsonRow = new JSONObject();
            for (var colName : collumnNames) {
                jsonRow.put(colName, result.getString(colName));
            }
            jsonArr.put(jsonRow);
        }

        var rootJson = new JSONObject();
        rootJson.put("handshake_buffer", jsonArr);
        return rootJson;
    }

    public JSONObject GetAllUsers() throws SQLException {

        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from users");
        var collumnNames = List.of("id", "facebook_id", "public_key");

        var jsonArr = new JSONArray();
        while (result.next()) {
            var jsonRow = new JSONObject();
            for (var colName : collumnNames) {
                jsonRow.put(colName, result.getString(colName));
            }
            jsonArr.put(jsonRow);
        }

        var rootJson = new JSONObject();
        rootJson.put("users", jsonArr);
        return rootJson;
    }
}
