package tv.csdm.minecraft.verify.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class FixedWindowRateLimiter<K> {
    private final int limit;
    private final Duration windowSize;
    private final Clock clock;
    private final Map<K, Window> windows = new HashMap<>();

    public FixedWindowRateLimiter(int limit, Duration windowSize) {
        this(limit, windowSize, Clock.systemUTC());
    }

    FixedWindowRateLimiter(int limit, Duration windowSize, Clock clock) {
        if (limit <= 0 || windowSize.isNegative() || windowSize.isZero()) {
            throw new IllegalArgumentException("El limite y la ventana deben ser positivos");
        }
        this.limit = limit;
        this.windowSize = windowSize;
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(K key) {
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current == null || !now.isBefore(current.startedAt.plus(windowSize))) {
            windows.put(key, new Window(now, 1));
            prune(now);
            return true;
        }
        if (current.count >= limit) {
            return false;
        }
        windows.put(key, new Window(current.startedAt, current.count + 1));
        return true;
    }

    private void prune(Instant now) {
        if (windows.size() < 128) {
            return;
        }
        windows.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().startedAt.plus(windowSize)));
    }

    private record Window(Instant startedAt, int count) {}
}

