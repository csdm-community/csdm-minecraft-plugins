package tv.csdm.minecraft.admin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class WorldPolicyListener implements Listener {
    private final CSDMAdminPlugin plugin;

    public WorldPolicyListener(CSDMAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.worldPolicyService().apply(event.getWorld());
    }
}
