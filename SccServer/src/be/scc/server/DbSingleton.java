package be.scc.server;

import be.scc.common.SccEncryption;

import java.security.PublicKey;
import java.sql.*;

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
}
