package be.scc.common;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @org.junit.jupiter.api.Test
    void splitQuery() throws URISyntaxException, UnsupportedEncodingException {
        URI uri = new URI("http://localhost:5661/login.html?testKey=testValue");

        Map<String, String> map = Util.splitQuery(uri);

        assert (map.get("testKey").equals("testValue"));
    }

    //@org.junit.jupiter.api.Test
    //void javaTest()  {
    //}
}