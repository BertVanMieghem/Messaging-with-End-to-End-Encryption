package be.scc.common;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

class UtilTest {

    @org.junit.jupiter.api.Test
    void splitQuery() throws URISyntaxException, UnsupportedEncodingException {
        URI uri = new URI("http://localhost:5661/login.html?testKey=testValue");

        Map<String, String> map = Util.decodeQueryString(uri);

        assert (map.get("testKey").equals("testValue"));
    }

    @org.junit.jupiter.api.Test
    void postBody() {

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