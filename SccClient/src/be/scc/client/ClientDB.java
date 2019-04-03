package be.scc.client;

import java.security.KeyPair;
import java.sql.*;

public class ClientDB {

    Connection conn = null;

    // private constructor restricted to this class itself
    private ClientDB() {


        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:SccClient.sqlite");


        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    // Todo
    public void setSecretPublicKeys(KeyPair pair){

        //Statement statement = conn.createStatement();
        //statement.executeUpdate("INSERT INTO users VALUES (NULL, " + facebookId + ", " + publicKey + ")");
    }
}
