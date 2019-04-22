package be.scc.common;

import org.junit.jupiter.api.Test;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

class UtilTest {

    @Test
    public void splitQuery() throws URISyntaxException, UnsupportedEncodingException {
        URI uri = new URI("http://localhost:5661/login.html?testKey=testValue");

        Map<String, String> map = Util.decodeQueryString(uri);

        assert (map.get("testKey").equals("testValue"));
    }

    @Test
    public void postBody() {

        var postParameters = new HashMap<String, String>();
        postParameters.put("testKey", "testValue");
        postParameters.put("testKey2", "1#!@#$%^&*()_)(");
        postParameters.put("1#!@#$%^&*()_)(", "testValue");

        var encoded = Util.encodePostParams(postParameters);
        var decoded = Util.decodePostParams(encoded);

        assert (decoded.get("testKey").get(0).equals(postParameters.get("testKey")));
        assert (decoded.get("testKey2").get(0).equals(postParameters.get("testKey2")));
        assert (decoded.get("1#!@#$%^&*()_)(").get(0).equals(postParameters.get("1#!@#$%^&*()_)(")));
    }
}