package tn.esprit.services;

import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.Jedis;

public class SpamGuardService {

    private final String host;
    private final int port;
    private final String password;
    private final boolean enabled;

    public SpamGuardService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.host = dotenv.get("UPSTASH_REDIS_HOST");
        this.port = parsePort(dotenv.get("UPSTASH_REDIS_PORT"));
        this.password = dotenv.get("UPSTASH_REDIS_PASSWORD");
        this.enabled = host != null && !host.isBlank() && port > 0 && password != null && !password.isBlank();
    }

    public boolean allowPost(int userId) {
        return allow("spam:post:" + userId, 3, 60);
    }

    public boolean allowReply(int userId) {
        return allow("spam:reply:" + userId, 8, 60);
    }

    public boolean allowReport(int userId) {
        return allow("spam:report:" + userId, 5, 300);
    }

    private boolean allow(String key, int maxRequests, int windowSeconds) {
        if (!enabled) {
            return true;
        }

        try (Jedis jedis = new Jedis(host, port, true)) {
            jedis.auth(password);
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, windowSeconds);
            }
            return count <= maxRequests;
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private int parsePort(String rawPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(rawPort.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
