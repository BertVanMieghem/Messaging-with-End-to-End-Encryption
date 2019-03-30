import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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
    public static void main(String[] args) throws IOException {
        System.out.println("Hello World!");

        // Port should not be in this list: https://svn.nmap.org/nmap/nmap-services
        // Port number is 'Secure Chanel Chat Interface' written in bad leet
        HttpServer server = HttpServer.create(new InetSocketAddress(5661), 0);
        server.createContext("/", new StaticHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        MainDialog dialog = new MainDialog();
        dialog.pack();
        dialog.setVisible(true);

        //System.exit(0);
    }
}
