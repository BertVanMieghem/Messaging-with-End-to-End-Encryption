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
        PreparedStatement pstmt = conn.prepareStatement("UPDATE single_row SET facebook_id=?");
        pstmt.setInt(1, facebook_id);
        pstmt.executeUpdate();
    }

    public void setKeyPair(KeyPair pair) throws SQLException {
        PrivateKey priv = pair.getPrivate();
        PublicKey publ = pair.getPublic();

        var private_key = SccEncryption.serializeKey(priv);
        var public_key = SccEncryption.serializeKey(publ);

        PreparedStatement pstmt = conn.prepareStatement("UPDATE single_row SET private_key=? , public_key=?");
        pstmt.setString(1, private_key);
        pstmt.setString(2, public_key);
        pstmt.executeUpdate();
    }

    public KeyPair getKeyPair() throws SQLException, GeneralSecurityException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from single_row");

        String privStr = result.getNString("private_key");
        String publStr = result.getNString("public_key");

        KeyPair pair = new KeyPair(SccEncryption.deserialisePublicKey(publStr), SccEncryption.deserialisePrivateKey(privStr));
        return pair;
    }
}
