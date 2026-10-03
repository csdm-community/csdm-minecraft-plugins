package tv.csdm.minecraft.admin.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;
import static org.mockito.Mockito.*;

class ParkourRescueTest {
    @Test void activeParkourOwnsRescueInsteadOfLobbySpawn() {
        var plugin = mock(CSDMAdminPlugin.class);
        var player = mock(Player.class);
        var move = mock(PlayerMoveEvent.class);
        when(move.getPlayer()).thenReturn(player);
        when(player.hasMetadata("csdm_parkour_active")).thenReturn(true);
        new AdminPlayerListener(plugin).onMove(move);
        verifyNoInteractions(plugin);
        verify(move, never()).setTo(any());
    }
}
