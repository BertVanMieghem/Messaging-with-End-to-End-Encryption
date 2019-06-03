package be.scc.common;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class FacebookId extends SccHash {

    public static FacebookId doSlowHash(long facebook_id) throws NoSuchAlgorithmException {
        var id = "" + facebook_id;
        var tmp = SccEncryption.SlowHash(id.getBytes(StandardCharsets.UTF_8));
        return new FacebookId(tmp.bytes);
    }

    public static FacebookId fromString(String base64Str) {
        if (base64Str == null) return null;
        return new FacebookId(Util.base64(base64Str));
    }

    public FacebookId(byte[] bytes) {
        super(bytes);
    }

}

/*class FacebookId {
    public FacebookId(long facebook_id) {
        assert facebook_id > 0;
        this.facebook_id = facebook_id;
    }

    private long facebook_id;

    @Override
    public String toString() {
        return "" + facebook_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacebookId that = (FacebookId) o;
        return facebook_id == that.facebook_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(facebook_id);
    }
}
*/