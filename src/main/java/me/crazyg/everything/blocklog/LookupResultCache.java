package me.crazyg.everything.blocklog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches lookup results per player for paginated display.
 * Entries expire after 5 minutes.
 */
public class LookupResultCache {

    private static final long TTL_SECONDS = 300;

    private final Map<UUID, CachedQuery> cache = new ConcurrentHashMap<>();

    public void store(UUID playerUuid, List<BlockChange> results, String scope) {
        cache.put(playerUuid, new CachedQuery(results, scope));
    }

    public List<BlockChange> get(UUID playerUuid) {
        CachedQuery q = cache.get(playerUuid);
        if (q == null) return null;
        if (System.currentTimeMillis() - q.createdMs > TTL_SECONDS * 1000) {
            cache.remove(playerUuid);
            return null;
        }
        return q.results;
    }

    public String getScope(UUID playerUuid) {
        CachedQuery q = cache.get(playerUuid);
        if (q == null) return "";
        return q.scope;
    }

    public void clear(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    private static final class CachedQuery {
        final List<BlockChange> results;
        final String scope;
        final long createdMs;

        CachedQuery(List<BlockChange> results, String scope) {
            this.results = new ArrayList<>(results);
            this.scope = scope;
            this.createdMs = System.currentTimeMillis();
        }
    }
}
