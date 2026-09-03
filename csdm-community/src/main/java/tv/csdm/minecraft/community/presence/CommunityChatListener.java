package tv.csdm.minecraft.community.presence;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class CommunityChatListener implements Listener {
    private static final Component SEPARATOR = Component.text(": ", NamedTextColor.GRAY);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) ->
                sourceDisplayName.append(SEPARATOR).append(message));
    }
}
