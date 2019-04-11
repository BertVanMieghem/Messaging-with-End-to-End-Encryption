package be.scc.client;

import be.scc.common.SccEncryption;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

interface SccListener {
    /**
     * Should be idempotent.
     * This function translates the model information to GUI and should not change any external state.
     */
    void SccModelChanged();
}

class SccDispatcher {
    private List<SccListener> listeners = new ArrayList<SccListener>();

    public void addListener(SccListener toAdd) {
        toAdd.SccModelChanged(); // Could be unhandy in some cases
        listeners.add(toAdd);
    }

    public void SccDispatchModelChanged() {
        for (SccListener hl : listeners)
            hl.SccModelChanged();
    }
}

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

    public SccDispatcher dispatcher = new SccDispatcher();

    // Don't forget to save to database when changing these properties:
    public long facebook_id;
    public KeyPair keyPair;
    public int last_handshake_buffer_index = 0;
    public int last_message_buffer_index = 0;


    public void addUser(int id, long facebook_id, String facebook_name, String public_key) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO local_users VALUES (?, ?, ?, ?, ?, ?)");
        var i = 0;
        pstmt.setInt(++i, id);
        pstmt.setLong(++i, facebook_id);
        pstmt.setString(++i, facebook_name);
        pstmt.setString(++i, public_key);
        pstmt.setNull(++i, Types.VARCHAR); // ephemeral_key_outgoing
        pstmt.setNull(++i, Types.VARCHAR); // ephemeral_key_ingoing
        pstmt.executeUpdate();
    }

    public void saveToDb() throws SQLException {
        String private_key = null;
        String public_key = null;

        if (this.keyPair != null) {
            private_key = SccEncryption.serializeKey(this.keyPair.getPrivate());
            public_key = SccEncryption.serializeKey(this.keyPair.getPublic());
        }

        PreparedStatement pstmt = conn.prepareStatement("UPDATE single_row SET " +
                "facebook_id=?," +
                "private_key=?," +
                "public_key=?," +
                "last_handshake_buffer_index=?," +
                "last_message_buffer_index=?" +
                "");
        var i = 0;
        pstmt.setLong(++i, facebook_id);
        pstmt.setString(++i, private_key);
        pstmt.setString(++i, public_key);
        pstmt.setInt(++i, last_handshake_buffer_index);
        pstmt.setInt(++i, last_message_buffer_index);
        pstmt.executeUpdate();

        dispatcher.SccDispatchModelChanged();
    }

    public void loadFromDb() throws SQLException, GeneralSecurityException {
        Statement statement = conn.createStatement();
        ResultSet result = statement.executeQuery("SELECT * from single_row");
        if (result.isClosed()) {
            Statement statement2 = conn.createStatement();
            statement.execute("INSERT INTO single_row VALUES (0, NULL, NULL, 0, 0)");
            saveToDb();
            return;
        }
        var facebook_id = result.getLong("facebook_id");

        var private_key = result.getString("private_key");
        var public_key = result.getString("public_key");
        KeyPair pair = null;
        if (public_key != null && private_key != null)
            pair = new KeyPair(SccEncryption.deserialisePublicKey(public_key), SccEncryption.deserialisePrivateKey(private_key));

        var last_handshake_buffer_index = result.getInt("last_handshake_buffer_index");
        var last_message_buffer_index = result.getInt("last_message_buffer_index");

        this.facebook_id = facebook_id;
        this.keyPair = pair;
        this.last_handshake_buffer_index = last_handshake_buffer_index;
        this.last_message_buffer_index = last_message_buffer_index;
    }

    public List<local_user> getUsers() {

        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT * from local_users");

            var aggregate = new ArrayList<local_user>();
            while (result.next()) {
                var row = new local_user();
                row.id = result.getInt("id");
                row.facebook_id = result.getLong("facebook_id");
                row.facebook_name = result.getString("facebook_name");

                var public_key = result.getString("public_key");
                if (public_key != null) row.public_key = SccEncryption.deserialisePublicKey(public_key);

                var ephemeral_key_outgoing = result.getString("ephemeral_key_outgoing");
                if (ephemeral_key_outgoing != null)
                    row.ephemeral_key_outgoing = SccEncryption.deserialisePublicKey(ephemeral_key_outgoing);

                var ephemeral_key_ingoing = result.getString("ephemeral_key_ingoing");
                if (ephemeral_key_ingoing != null)
                    row.ephemeral_key_ingoing = SccEncryption.deserialisePublicKey(ephemeral_key_ingoing);

                aggregate.add(row);
            }
            return aggregate;
        } catch (SQLException | GeneralSecurityException e) {
            e.printStackTrace();
            return new ArrayList<local_user>();

        }
    }

}

class local_user {
    public int id;
    public long facebook_id;
    public String facebook_name;
    public PublicKey public_key;
    public Key ephemeral_key_outgoing;
    public Key ephemeral_key_ingoing;

    public String[] toStringList() {
        Object[] tmp = {id, facebook_id, facebook_name, public_key, ephemeral_key_outgoing, ephemeral_key_ingoing};
        return Stream.of(tmp).map(o -> "" + o).toArray(String[]::new);
    }

    public final static String[] columnNames = {"id", "facebook_id", "facebook_name", "public_key", "ephemeral_key_outgoing", "ephemeral_key_ingoing"};
}