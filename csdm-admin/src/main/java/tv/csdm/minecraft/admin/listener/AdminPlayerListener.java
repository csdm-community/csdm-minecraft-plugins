package tv.csdm.minecraft.admin.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        // Whitelist rejection happens before PlayerJoinEvent. Change only its text;
        // never allow access or overwrite a ban, full-server or other rejection.
        if (plugin.settings().maintenanceEnabled()
                && event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
            event.kickMessage(miniMessage.deserialize(plugin.settings().maintenanceKickMessage()));
        }
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
                    {
                        if (player.isOnline() && isLobby(player.getWorld())) {
                            plugin.worldPolicyService().configuredSpawn().ifPresent(player::teleportAsync);
                        }
                    });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !isLobby(event.getTo().getWorld())
                || event.getTo().getY() >= plugin.settings().rescueBelowY()) {
            return;
        }
        plugin.worldPolicyService().configuredSpawn().ifPresent(spawn -> {
            event.getPlayer().setFallDistance(0);
            event.getPlayer().setVelocity(new org.bukkit.util.Vector());
            event.setTo(spawn);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isLobby(event.getPlayer().getWorld())) {
            plugin.worldPolicyService().configuredSpawn().ifPresent(event::setRespawnLocation);
        }
    }

    private boolean isLobby(org.bukkit.World world) {
        return world != null && world.getName().equals(plugin.settings().worldName());
    }
}
