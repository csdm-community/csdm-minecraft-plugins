package tv.csdm.minecraft.admin.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;
import tv.csdm.minecraft.admin.service.AdminSettings;
import tv.csdm.minecraft.admin.service.WorldPolicyService;

class LobbyRoutingTest {
    @Test void onlyLobbyDeathsAndFallsUseMuseumSpawn() {
        var plugin = mock(CSDMAdminPlugin.class);
        var settings = mock(AdminSettings.class);
        var policy = mock(WorldPolicyService.class);
        when(plugin.settings()).thenReturn(settings);
        when(plugin.worldPolicyService()).thenReturn(policy);
        when(settings.worldName()).thenReturn("lobby");
        when(settings.rescueBelowY()).thenReturn(50);
        var lobby = mock(World.class);
        when(lobby.getName()).thenReturn("lobby");
        var spawn = new Location(lobby, 0.5, 101, 0.5);
        when(policy.configuredSpawn()).thenReturn(Optional.of(spawn));
        var listener = new AdminPlayerListener(plugin);
        for (String name : new String[] {"verify_void", "other_game", "lobby"}) {
            var world = mock(World.class);
            when(world.getName()).thenReturn(name);
            var player = mock(Player.class);
            when(player.getWorld()).thenReturn(world);
            var respawn = mock(PlayerRespawnEvent.class);
            when(respawn.getPlayer()).thenReturn(player);
            listener.onRespawn(respawn);
            var below = new Location(world, 10, -100, 10);
            var move = new PlayerMoveEvent(player, below, below);
            listener.onMove(move);
            if (name.equals("lobby")) {
                verify(respawn).setRespawnLocation(spawn);
                assertEquals(spawn, move.getTo());
                verify(player).setFallDistance(0);
            } else {
                verify(respawn, never()).setRespawnLocation(any());
                assertSame(below, move.getTo());
            }
        }
    }
}
