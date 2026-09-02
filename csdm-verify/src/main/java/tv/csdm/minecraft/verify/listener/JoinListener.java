package tv.csdm.minecraft.verify.listener;

import java.time.Duration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.verify.config.Messages;

public final class JoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Messages messages;

    public JoinListener(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.showTitle(Title.title(
                    messages.plain("join-title"),
                    messages.plain("join-subtitle"),
                    Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
            player.sendMessage(messages.prefixed("join-line-1"));
            player.sendMessage(messages.prefixed("join-line-2"));
        }, 10L);
    }
}
