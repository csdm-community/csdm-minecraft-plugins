package tv.csdm.minecraft.parkour;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Emitted synchronously only after a complete route and successful inventory restoration. */
public final class ParkourCompletedEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String courseId;
    private final long elapsedMillis;
    public ParkourCompletedEvent(Player player, String courseId, long elapsedMillis) {
        super(player); this.courseId = courseId; this.elapsedMillis = elapsedMillis;
    }
    public String getCourseId() { return courseId; }
    public long getElapsedMillis() { return elapsedMillis; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
