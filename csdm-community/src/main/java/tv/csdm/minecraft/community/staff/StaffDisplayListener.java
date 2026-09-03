package tv.csdm.minecraft.community.staff;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffDisplayListener implements Listener {
    private final JavaPlugin plugin;
    private final StaffRankService service;

    public StaffDisplayListener(JavaPlugin plugin, StaffRankService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.ensureDefaultFunctionalRank(event.getPlayer()).whenComplete((changed, error) ->
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                service.syncDisplay(event.getPlayer());
            }
        }, 5L));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.removeDisplay(event.getPlayer());
    }
}
