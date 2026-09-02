package tv.csdm.minecraft.verify.listener;

import java.time.Duration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import tv.csdm.minecraft.verify.CSDMVerifyPlugin;
import tv.csdm.minecraft.verify.backend.BackendClient;
import tv.csdm.minecraft.verify.config.Messages;
import tv.csdm.minecraft.verify.config.VerifySettings;
import tv.csdm.minecraft.verify.model.IdentityStatusResponse;
import tv.csdm.minecraft.verify.world.WorldRoutingService;

public final class JoinListener implements Listener {
    private final CSDMVerifyPlugin plugin;
    private final VerifySettings settings;
    private final Messages messages;
    private final BackendClient backend;
    private final WorldRoutingService routing;

    public JoinListener(
            CSDMVerifyPlugin plugin,
            VerifySettings settings,
            Messages messages,
            BackendClient backend,
            WorldRoutingService routing) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.backend = backend;
        this.routing = routing;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(false);

        routing.sendToVerification(player);
        player.showTitle(Title.title(
                messages.plain("status-title"),
                messages.plain("status-subtitle"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(3), Duration.ofMillis(300))));

        if (!settings.enabled()) {
            showPending(player, new IdentityStatusResponse(false, false));
            return;
        }

        backend.identityStatus(player.getUniqueId()).thenAccept(status ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (status.linked()) {
                        routing.sendToMuseum(player);
                        showMuseum(player);
                    } else {
                        showPending(player, status);
                    }
                }));
    }

    private void showPending(org.bukkit.entity.Player player, IdentityStatusResponse status) {
        routing.sendToVerification(player);
        player.showTitle(Title.title(
                messages.plain("join-title"),
                messages.plain("join-subtitle"),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
        player.sendMessage(messages.prefixed("join-line-1"));
        player.sendMessage(messages.prefixed("join-line-2"));
        if (!status.reachable() && settings.enabled()) {
            player.sendMessage(messages.prefixed("status-unavailable"));
        }
    }

    private void showMuseum(org.bukkit.entity.Player player) {
        player.showTitle(Title.title(
                messages.plain("museum-title"),
                messages.plain("museum-subtitle"),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
        player.sendMessage(messages.prefixed("museum-welcome"));
    }
}
