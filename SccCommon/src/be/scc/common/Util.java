package be.scc.common;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;


public class Util {

    public static String encodePostParams(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    public static Map<String, List<String>> decodePostParams(String postBody) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        String defs[] = postBody.split("[&]");
        for (String def : defs) {
            int ix = def.indexOf('=');
            String name;
            String value;
            if (ix < 0) {
                name = URLDecoder.decode(def, StandardCharsets.UTF_8);
                value = "";
            } else {
                name = URLDecoder.decode(def.substring(0, ix), StandardCharsets.UTF_8);
                value = URLDecoder.decode(def.substring(ix + 1), StandardCharsets.UTF_8);
            }
            List<String> list = parms.get(name);
            if (list == null) {
                list = new ArrayList<String>();
                parms.put(name, list);
            }
            list.add(value);
        }
        return parms;
    }

    public static Map<String, List<String>> getBodyParams(HttpExchange httpExchange) throws IOException {
        var body = httpExchange.getRequestBody();
        var bodyStr = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        return decodePostParams(bodyStr);
    }

    public static String SyncRequestPost(URL url, Map<String, String> postParameters) throws IOException {
        // https://www.baeldung.com/java-http-request
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(encodePostParams(postParameters));
        out.flush();
        out.close();

        var in = con.getInputStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static JSONObject SyncJsonRequest(URL url) throws IOException {
        var ret = SyncRequest(url);
        JSONObject obj = new JSONObject(ret);
        return obj;
    }

    public static String SyncRequest(URL url) throws IOException {
        // https://www.baeldung.com/java-http-request
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET"); // Is default

        var in = con.getInputStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
    public static String ReadFileLineByLine(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static Map<String, String> decodeQueryString(URI url) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return query_pairs;
    }


    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }


    /**
     * Will guess the column names out of the ResultSet
     * Sligtly less secure, but more flexible
     */
    public static JSONArray SqlResultsToJson(ResultSet result) throws SQLException {
        ResultSetMetaData rsmd = result.getMetaData();
        var collumnNames = new ArrayList<String>();
        var count = rsmd.getColumnCount();
        for (int i = 1; i <= count; i++) {
            collumnNames.add(rsmd.getColumnName(i));
        }
        return SqlResultsToJson(result, collumnNames);
    }

    public static JSONArray SqlResultsToJson(ResultSet result, List<String> collumnNames) throws SQLException {
        var jsonArr = new JSONArray();
        while (result.next()) {
            var jsonRow = new JSONObject();
            for (var colName : collumnNames) {
                jsonRow.put(colName, result.getString(colName));
            }
            jsonArr.put(jsonRow);
        }
        return jsonArr;
    }
}
