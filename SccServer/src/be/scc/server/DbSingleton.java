package be.scc.server;

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

    public void InsertUser(long facebookId, int publicKey) throws SQLException {

        Statement statement = conn.createStatement();
        statement.executeUpdate("INSERT INTO users VALUES (NULL, " + facebookId + ", " + publicKey + ")");
        //conn.commit();
    }
}
