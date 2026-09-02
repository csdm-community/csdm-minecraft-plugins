package tv.csdm.minecraft.admin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import tv.csdm.minecraft.admin.moderation.Sanction;
import tv.csdm.minecraft.admin.moderation.SanctionService;

public final class SanctionListener implements Listener {
    private final SanctionService sanctions;

    public SanctionListener(SanctionService sanctions) {
        this.sanctions = sanctions;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        Sanction restriction = sanctions.activeRestriction(event.getPlayer().getUniqueId());
        if (restriction != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, sanctions.restrictionMessage(restriction));
        }
    }
}
