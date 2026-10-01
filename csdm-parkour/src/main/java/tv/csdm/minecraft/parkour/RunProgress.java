package tv.csdm.minecraft.parkour;

/** Pure ordered-checkpoint state; time comes from the monotonic clock. */
final class RunProgress {
    private final int total;
    private int checkpoint;
    private long started;

    RunProgress(int total, long now) {
        if (total < 0) throw new IllegalArgumentException("Negative checkpoint count");
        this.total = total;
        restart(now);
    }

    boolean touch(int index) {
        if (index != checkpoint + 1 || index > total) return false;
        checkpoint = index;
        return true;
    }

    void restart(long now) { checkpoint = 0; started = now; }
    int checkpoint() { return checkpoint; }
    boolean canFinish() { return checkpoint == total; }
    long elapsedMillis(long now) { return Math.max(0, (now - started) / 1_000_000); }

    static String format(long millis) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d.%03d",
                millis / 60_000, (millis / 1000) % 60, millis % 1000);
    }
}
