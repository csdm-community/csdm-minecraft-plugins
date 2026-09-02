package tv.csdm.minecraft.admin.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class AdminPlayerListener implements Listener {
    private static final String ADMIN_BYPASS = "csdm.admin.bypass";
    private static final String MAINTENANCE_BYPASS = "csdm.maintenance.bypass";
    private final CSDMAdminPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public AdminPlayerListener(CSDMAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (plugin.settings().maintenanceEnabled() && !player.hasPermission(MAINTENANCE_BYPASS)) {
            event.joinMessage(null);
            player.kick(miniMessage.deserialize(plugin.settings().maintenanceKickMessage()));
            return;
        }
        if (plugin.settings().forceAdventure()
                && !(plugin.settings().staffBypassKeepsGamemode() && player.hasPermission(ADMIN_BYPASS))) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        player.setAllowFlight(plugin.settings().allowFlight());
        if (plugin.settings().teleportToSpawnOnJoin()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.worldPolicyService().configuredSpawn().ifPresent(player::teleportAsync));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getY() >= plugin.settings().rescueBelowY()) {
            return;
        }
        plugin.worldPolicyService().configuredSpawn().ifPresent(spawn -> event.getPlayer().teleportAsync(spawn));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.worldPolicyService().configuredSpawn().ifPresent(event::setRespawnLocation);
    }
}
