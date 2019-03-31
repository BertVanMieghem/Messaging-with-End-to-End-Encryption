import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Map;

class StaticHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String path = httpExchange.getRequestURI().getPath();
        System.out.println("StaticHandler handle: " + path);

        byte[] data;
        switch (path) {
            case "/login.html":
                data = Util.ReadFileLineByLine("res" + path).getBytes();
                break;
            case "/callback":
                URI uri = httpExchange.getRequestURI();
                Map<String, String> queryParameters = Util.splitQuery(uri);
                String accessToken = queryParameters.get("accessToken");
                data = "callback accepted".getBytes();

                // https://www.baeldung.com/java-http-request
                URL url = new URL("https://graph.facebook.com/v3.2/me?access_token=" + accessToken + "&method=get&pretty=0&sdk=joey&suppress_http_code=1");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                con.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                //out.writeBytes(ParameterStringBuilder.getParamsString(parameters));

                out.flush();
                out.close();

                Runnable runner = new Runnable() {
                    public void run() {
                        SccSingleton.inst().FromLoginToMessageDialog();
                    }
                };
                EventQueue.invokeLater(runner);

                break;
            default:
                String response = "StaticHandler handle: " + path;

                data = response.getBytes();
                break;
        }
        httpExchange.sendResponseHeaders(200, data.length);
        httpExchange.getResponseBody().write(data);
        httpExchange.getResponseBody().close();
    }
}

public class mainClass {
    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("Hello World!");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Interface' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5661), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();


        SccSingleton.inst().ShowLoginDialog();

    }
}
