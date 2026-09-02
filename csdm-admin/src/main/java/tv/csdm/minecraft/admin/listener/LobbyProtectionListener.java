package tv.csdm.minecraft.admin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class LobbyProtectionListener implements Listener {
    private static final String BYPASS = "csdm.admin.bypass";
    private final CSDMAdminPlugin plugin;

    public LobbyProtectionListener(CSDMAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (protectedWorld(event.getBlock().getWorld().getName())
                && plugin.settings().blockBreaking()
                && !event.getPlayer().hasPermission(BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (protectedWorld(event.getBlock().getWorld().getName())
                && plugin.settings().blockPlacing()
                && !event.getPlayer().hasPermission(BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (protectedWorld(event.getPlayer().getWorld().getName())
                && plugin.settings().blockInteractions()
                && !event.getPlayer().hasPermission(BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (protectedWorld(event.getPlayer().getWorld().getName())
                && plugin.settings().blockItemDrops()
                && !event.getPlayer().hasPermission(BYPASS)) {
            event.setCancelled(true);
        }
    }

    private boolean protectedWorld(String worldName) {
        return plugin.settings().protectionEnabled() && plugin.settings().worldName().equals(worldName);
    }
}

