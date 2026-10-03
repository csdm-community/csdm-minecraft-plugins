package tv.csdm.minecraft.verify.service;

import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerIdentityResolverTest {
    @Test void javaWorksWithoutFloodgateInstalled() {
        var plugin = mock(JavaPlugin.class);
        var server = mock(Server.class);
        var plugins = mock(PluginManager.class);
        var player = mock(Player.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(plugins);
        when(plugins.isPluginEnabled("floodgate")).thenReturn(false);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("JavaPlayer");
        var identity = new PlayerIdentityResolver(plugin).resolve(player);
        assertEquals(player.getUniqueId(), identity.uuid());
        assertFalse(identity.bedrock());
    }
}
