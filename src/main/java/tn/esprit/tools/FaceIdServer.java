package tn.esprit.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class FaceIdServer {

    public enum SessionType { REGISTER, VERIFY }
    public enum SessionStatus { PENDING, CAPTURED, FAILED }

    public static class Session {
        public final String token;
        public final SessionType type;
        public final String email;
        public volatile SessionStatus status = SessionStatus.PENDING;
        public volatile double[] descriptor;
        public volatile String error;
        public volatile long createdAt = System.currentTimeMillis();

        Session(String token, SessionType type, String email) {
            this.token = token;
            this.type = type;
            this.email = email;
        }
    }

    private static FaceIdServer INSTANCE;
    public static synchronized FaceIdServer get() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new FaceIdServer();
            INSTANCE.start();
        }
        return INSTANCE;
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private HttpsServer server;
    private String lanIp;
    private int port;

    private void start() throws Exception {
        lanIp = LanAddress.pick();
        KeyStore ks = SelfSignedCert.loadOrCreate(lanIp);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, SelfSignedCert.PASSWORD);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());

        port = findFreePort();
        server = HttpsServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx));

        server.createContext("/face/register", new PageHandler("/face/register.html"));
        server.createContext("/face/verify",   new PageHandler("/face/verify.html"));
        server.createContext("/face/submit",   new SubmitHandler());
        server.createContext("/face/status",   new StatusHandler());

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("[FaceIdServer] https://" + lanIp + ":" + port);
    }

    private int findFreePort() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    public Session createSession(SessionType type, String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Session s = new Session(token, type, email);
        sessions.put(token, s);
        return s;
    }

    public Session getSession(String token) {
        return sessions.get(token);
    }

    public void removeSession(String token) {
        sessions.remove(token);
    }

    public static Session get_unsafe(String token) {
        return INSTANCE == null ? null : INSTANCE.sessions.get(token);
    }

    public static void get_unsafe_remove(String token) {
        if (INSTANCE != null) INSTANCE.sessions.remove(token);
    }

    public String urlFor(Session s) {
        String path = s.type == SessionType.REGISTER ? "/face/register" : "/face/verify";
        return "https://" + lanIp + ":" + port + path + "?token=" + s.token;
    }

    // ==================== HANDLERS ====================

    private class PageHandler implements HttpHandler {
        private final String resource;
        PageHandler(String resource) { this.resource = resource; }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String token = extractToken(query);
            Session s = token == null ? null : sessions.get(token);
            if (s == null) {
                sendText(ex, 404, "Invalid or expired session.");
                return;
            }
            byte[] html = loadResource(resource);
            if (html == null) {
                sendText(ex, 500, "Page not found.");
                return;
            }
            String body = new String(html, StandardCharsets.UTF_8)
                    .replace("{{TOKEN}}", s.token);
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        }
    }

    private class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"ok\":false,\"error\":\"method\"}");
                return;
            }
            try {
                byte[] body = readAll(ex.getRequestBody());
                String text = new String(body, StandardCharsets.UTF_8);
                Map<String, String> obj = parseJsonFlat(text);
                String token = obj.get("token");
                String descStr = obj.get("descriptor");
                if (token == null || descStr == null) {
                    sendJson(ex, 400, "{\"ok\":false,\"error\":\"missing\"}");
                    return;
                }
                Session s = sessions.get(token);
                if (s == null) {
                    sendJson(ex, 404, "{\"ok\":false,\"error\":\"session\"}");
                    return;
                }
                String[] parts = descStr.split(",");
                if (parts.length != 128) {
                    sendJson(ex, 400, "{\"ok\":false,\"error\":\"descriptor length\"}");
                    return;
                }
                double[] d = new double[128];
                for (int i = 0; i < 128; i++) d[i] = Double.parseDouble(parts[i].trim());
                s.descriptor = d;
                s.status = SessionStatus.CAPTURED;
                sendJson(ex, 200, "{\"ok\":true}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"ok\":false,\"error\":\"server\"}");
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String token = extractToken(ex.getRequestURI().getQuery());
            Session s = token == null ? null : sessions.get(token);
            if (s == null) {
                sendJson(ex, 404, "{\"status\":\"unknown\"}");
                return;
            }
            sendJson(ex, 200, "{\"status\":\"" + s.status.name().toLowerCase() + "\"}");
        }
    }

    // ==================== HELPERS ====================

    private static String extractToken(String query) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && "token".equals(p.substring(0, i))) return p.substring(i + 1);
        }
        return null;
    }

    private static byte[] loadResource(String path) throws IOException {
        try (InputStream is = FaceIdServer.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return readAll(is);
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    // Minimal JSON parser for {"token":"...","descriptor":"..."}
    private static Map<String, String> parseJsonFlat(String s) {
        Map<String, String> out = new HashMap<>();
        int i = 0;
        while (i < s.length()) {
            int keyStart = s.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = s.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = s.substring(keyStart + 1, keyEnd);
            int colon = s.indexOf(':', keyEnd + 1);
            if (colon < 0) break;
            int valStart = s.indexOf('"', colon + 1);
            if (valStart < 0) break;
            int valEnd = s.indexOf('"', valStart + 1);
            if (valEnd < 0) break;
            String val = s.substring(valStart + 1, valEnd);
            out.put(key, val);
            i = valEnd + 1;
        }
        return out;
    }
}
