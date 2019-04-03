package be.scc.client;

import be.scc.common.SccEncryption;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.*;

public class ClientDB {

    Connection conn = null;

    // private constructor restricted to this class itself
    public ClientDB() {


        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:SccClient.sqlite");


        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    public void setFacebookId(int facebook_id) throws SQLException {
        Statement statement = conn.createStatement();
        statement.executeUpdate("UPDATE single_row SET facebook_id=\"" + facebook_id + "\"");
    }

    public void setSecretPublicKeys(KeyPair pair) throws SQLException {
        PrivateKey priv = pair.getPrivate();
        PublicKey publ = pair.getPublic();

        String privStr = SccEncryption.serializeKey(priv);
        String publStr = SccEncryption.serializeKey(publ);

        Statement statement = conn.createStatement();
        statement.executeUpdate("UPDATE single_row SET private_key=\"" + privStr + "\", public_key=\"" + publStr + "\"");
    }

    public KeyPair getSecretPublicKeys() throws SQLException, GeneralSecurityException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from single_row");

        String privStr = result.getNString("private_key");
        String publStr = result.getNString("public_key");

        KeyPair pair = new KeyPair(SccEncryption.deserialisePublicKey(publStr), SccEncryption.deserialisePrivateKey(privStr));
        return pair;
    }
}
