package tn.esprit.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalModelServer {

    private static final AtomicInteger MODEL_ID_SEQUENCE = new AtomicInteger(1);
    private static final Map<String, ServedModel> MODELS = new ConcurrentHashMap<>();
    private static HttpServer server;
    private static int port = -1;

    private LocalModelServer() {
    }

    public static synchronized String registerModel(Path path) throws IOException {
        ensureServer();
        String modelId = String.valueOf(MODEL_ID_SEQUENCE.getAndIncrement());
        String fileName = path.getFileName().toString();
        MODELS.put(modelId, new ServedModel(path, detectContentType(fileName), fileName));
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        return "http://127.0.0.1:" + port + "/model/" + modelId + "/" + encodedName;
    }

    private static void ensureServer() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/model/", new ModelHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private static String detectContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".gltf")) {
            return "model/gltf+json";
        }
        return "model/gltf-binary";
    }

    private record ServedModel(Path path, String contentType, String fileName) {
    }

    private static final class ModelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                URI requestUri = exchange.getRequestURI();
                String[] segments = requestUri.getPath().split("/");
                if (segments.length < 3) {
                    sendStatus(exchange, 404);
                    return;
                }

                ServedModel servedModel = MODELS.get(segments[2]);
                if (servedModel == null || !Files.exists(servedModel.path())) {
                    sendStatus(exchange, 404);
                    return;
                }

                byte[] bytes = Files.readAllBytes(servedModel.path());
                exchange.getResponseHeaders().set("Content-Type", servedModel.contentType());
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bytes);
                }
            } finally {
                exchange.close();
            }
        }

        private void sendStatus(HttpExchange exchange, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, -1);
        }
    }
}
