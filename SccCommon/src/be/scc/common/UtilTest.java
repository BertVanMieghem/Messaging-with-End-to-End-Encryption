package be.scc.common;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class UtilTest {

    @Test
    protected void splitQuery() throws URISyntaxException, UnsupportedEncodingException {
        URI uri = new URI("http://localhost:5661/login.html?testKey=testValue");

        Map<String, String> map = Util.decodeQueryString(uri);

        assert (map.get("testKey").equals("testValue"));
    }

    @Test
    protected void postBody() {

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

    @Test
    protected void hashMap() {
        HashMap<UUID, String> map1 = new HashMap<>();
        var g1 = UUID.randomUUID();
        var g2 = UUID.randomUUID();
        var g3 = UUID.randomUUID();

        map1.put(g1, "A");
        map1.put(g2, "B");
        map1.put(g3, "C");

        //Same as map1
        HashMap<UUID, String> map2 = new HashMap<>();

        map2.put(g3, "C");
        map2.put(g1, "A");
        map2.put(g2, "B");

        //Different from map1
        HashMap<UUID, String> map3 = new HashMap<>();

        map3.put(g1, "A");
        map3.put(g2, "B");
        map3.put(g3, "C");
        map3.put(g3, "D"); // Duplicate key

        assert map1.equals(map2);
        assert !map1.equals(map3);

        assert Util.listEqualsIgnoreOrder(map1.values(), map2.values());
        assert !Util.listEqualsIgnoreOrder(map1.values(), map3.values());
    }
}