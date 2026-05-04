package tn.esprit.tools;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Google OAuth 2.0 (OpenID Connect) for desktop apps.
 * <p>
 * Setup: drop client id + secret into ~/.skillora/api.properties as
 *   google.client.id=...
 *   google.client.secret=...
 */
public class GoogleOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPE = "openid email profile";
    private static final Path KEY_FILE =
            Paths.get(System.getProperty("user.home"), ".skillora", "api.properties");

    public static class GoogleUser {
        public String email;
        public boolean emailVerified;
        public String firstName;
        public String lastName;
        public String pictureUrl;
        public String googleId;     // sub
    }

    public GoogleUser signIn() throws Exception {
        Properties cfg = readConfig();
        String clientId = firstNonBlank(System.getenv("GOOGLE_CLIENT_ID"), cfg.getProperty("google.client.id"));
        String clientSecret = firstNonBlank(System.getenv("GOOGLE_CLIENT_SECRET"), cfg.getProperty("google.client.secret"));
        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException(
                    "Google OAuth not configured. Add google.client.id and google.client.secret to "
                            + KEY_FILE);
        }

        // 1. Spin up a local server to catch the redirect
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port + "/callback";
        String state = randomString(24);
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String code = params.get("code");
            String returnedState = params.get("state");
            String error = params.get("error");

            String html;
            if (error != null) {
                codeFuture.completeExceptionally(new RuntimeException("Google returned error: " + error));
                html = htmlPage("Sign-in cancelled", "You can close this tab and try again.");
            } else if (code == null || !state.equals(returnedState)) {
                codeFuture.completeExceptionally(new RuntimeException("Invalid OAuth callback."));
                html = htmlPage("Sign-in failed", "Invalid response. Close this tab and retry.");
            } else {
                codeFuture.complete(code);
                html = htmlPage("Signed in to Google", "You can close this tab and return to SkillORA.");
            }
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        try {
            // 2. Open the browser to Google's consent page
            String authUrl = AUTH_URL
                    + "?response_type=code"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8)
                    + "&state=" + state
                    + "&access_type=online"
                    + "&prompt=select_account";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                throw new IllegalStateException("Cannot open a browser on this system.");
            }

            // 3. Wait up to 3 minutes for the user to finish on the browser
            String code;
            try {
                code = codeFuture.get(3, TimeUnit.MINUTES);
            } catch (TimeoutException te) {
                throw new IllegalStateException("Timed out waiting for Google sign-in.");
            }

            // 4. Exchange the code for an access token
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)).build();
            String tokenBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";
            HttpRequest tokenReq = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .build();
            HttpResponse<String> tokenResp = client.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            if (tokenResp.statusCode() < 200 || tokenResp.statusCode() >= 300) {
                throw new RuntimeException("Token exchange failed: HTTP " + tokenResp.statusCode()
                        + " " + tokenResp.body());
            }
            String accessToken = new JSONObject(tokenResp.body()).getString("access_token");

            // 5. Call /userinfo
            HttpRequest infoReq = HttpRequest.newBuilder(URI.create(USERINFO_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();
            HttpResponse<String> infoResp = client.send(infoReq, HttpResponse.BodyHandlers.ofString());
            if (infoResp.statusCode() < 200 || infoResp.statusCode() >= 300) {
                throw new RuntimeException("UserInfo failed: HTTP " + infoResp.statusCode());
            }
            JSONObject o = new JSONObject(infoResp.body());

            GoogleUser u = new GoogleUser();
            u.googleId = o.optString("sub", null);
            u.email = o.optString("email", null);
            u.emailVerified = o.optBoolean("email_verified", false);
            u.firstName = o.optString("given_name", null);
            u.lastName = o.optString("family_name", null);
            u.pictureUrl = o.optString("picture", null);
            return u;
        } finally {
            server.stop(0);
        }
    }

    private Properties readConfig() {
        Properties p = new Properties();
        try {
            if (Files.exists(KEY_FILE)) {
                try (var in = Files.newInputStream(KEY_FILE)) { p.load(in); }
            }
        } catch (IOException ignored) {}
        return p;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> out = new HashMap<>();
        if (query == null) return out;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    private String randomString(int len) {
        SecureRandom r = new SecureRandom();
        byte[] buf = new byte[len];
        r.nextBytes(buf);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private String htmlPage(String title, String body) {
        return "<!doctype html><meta charset=utf-8><title>" + title + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;display:flex;flex-direction:column;"
                + "align-items:center;justify-content:center;height:100vh;background:#0f172a;color:#e2e8f0}"
                + "h1{font-weight:600;margin:0 0 12px}p{opacity:.7}</style>"
                + "<h1>" + title + "</h1><p>" + body + "</p>";
    }
}
