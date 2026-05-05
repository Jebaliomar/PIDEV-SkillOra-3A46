package tn.esprit.services;

import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.Jedis;

public class SpamGuardService {

    private final String host;
    private final int port;
    private final String password;

    public SpamGuardService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.host = dotenv.get("UPSTASH_REDIS_HOST");
        this.port = Integer.parseInt(dotenv.get("UPSTASH_REDIS_PORT"));
        this.password = dotenv.get("UPSTASH_REDIS_PASSWORD");
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
        try (Jedis jedis = new Jedis(host, port, true)) {
            jedis.auth(password);
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, windowSeconds);
            }
            return count <= maxRequests;
        }
    }
}