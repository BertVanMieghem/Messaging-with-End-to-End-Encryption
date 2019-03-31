package be.scc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class Main {

    class StaticHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            String path = httpExchange.getRequestURI().getPath();
            System.out.println("StaticHandler handle: " + path);

            byte[] data;
            switch (path) {
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

    public static void main(String[] args) {
        System.out.println("Setver started");

    }
}
