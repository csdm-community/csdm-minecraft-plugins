package tv.csdm.minecraft.admin.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class ServerListListener implements Listener {
    private final CSDMAdminPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ServerListListener(CSDMAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ServerListPingEvent event) {
        if (!plugin.getConfig().getBoolean("motd.enabled", true)) {
            return;
        }
        boolean maintenance = plugin.settings().maintenanceEnabled();
        String prefix = maintenance ? "maintenance" : "online";
        String lineOne = plugin.getConfig().getString(
                "motd." + prefix + "-line-1", "<aqua><bold>ARCHIVO CSDM</bold></aqua>");
        String lineTwo = plugin.getConfig().getString(
                "motd." + prefix + "-line-2", "<gray>Verificacion de identidad</gray>");
        event.motd(miniMessage.deserialize(lineOne + "\n" + lineTwo));
    }
}

