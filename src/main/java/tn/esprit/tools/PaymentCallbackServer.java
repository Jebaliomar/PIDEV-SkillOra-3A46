package tn.esprit.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class PaymentCallbackServer implements AutoCloseable {

    public enum Status {
        SUCCESS,
        CANCELLED
    }

    public record Result(Status status, String sessionId) {
    }

    private final HttpServer server;
    private final CompletableFuture<Result> resultFuture = new CompletableFuture<>();

    public PaymentCallbackServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/success", exchange -> handle(exchange, Status.SUCCESS));
        server.createContext("/cancel", exchange -> handle(exchange, Status.CANCELLED));
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "skillora-payment-callback");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
    }

    public String successUrl() {
        return baseUrl() + "/success?session_id={CHECKOUT_SESSION_ID}";
    }

    public String cancelUrl() {
        return baseUrl() + "/cancel";
    }

    public CompletableFuture<Result> resultFuture() {
        return resultFuture;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange, Status status) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String sessionId = query.get("session_id");
        resultFuture.complete(new Result(status, sessionId));
        String title = status == Status.SUCCESS ? "Payment completed" : "Payment cancelled";
        String message = status == Status.SUCCESS
                ? "You can return to SkillORA. Your course will unlock in a moment."
                : "You can return to SkillORA and try again when ready.";
        byte[] response = buildHtml(title, message).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return values;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            values.put(key, value);
        }
        return values;
    }

    private String buildHtml(String title, String message) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: grid;
                            place-items: center;
                            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
                            background: #eef6ff;
                            color: #0f172a;
                        }
                        main {
                            width: min(560px, calc(100vw - 40px));
                            padding: 42px;
                            border: 1px solid #d8e4f2;
                            border-radius: 28px;
                            background: white;
                            box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
                        }
                        h1 { margin: 0 0 12px; font-size: 34px; }
                        p { margin: 0; font-size: 18px; line-height: 1.6; color: #53657d; }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>%s</h1>
                        <p>%s</p>
                    </main>
                </body>
                </html>
                """.formatted(escape(title), escape(title), escape(message));
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
