package tv.csdm.minecraft.admin.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;
import tv.csdm.minecraft.admin.moderation.Sanction;
import tv.csdm.minecraft.admin.moderation.SanctionService;
import tv.csdm.minecraft.admin.service.AdminSettings;

class MaintenanceMessageTest {
    private static final String TEXT = "<gold>ARCHIVO CSDM</gold>\n<gray>Estamos en mantenimiento. Vuelve pronto.</gray>";
    private static final Component MESSAGE = MiniMessage.miniMessage().deserialize(TEXT);
    private static final Component ORIGINAL = Component.text("Original rejection");

    @Test
    void whitelistUsesConfiguredMaintenanceMessageWithoutGrantingAccess() {
        var plugin = plugin(true);
        var listener = new AdminPlayerListener(plugin);
        Player player = mock(Player.class);
        var event = login(player, PlayerLoginEvent.Result.KICK_WHITELIST);
        listener.onLogin(event);
        assertEquals(MESSAGE, event.kickMessage());
        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        // A maintenance bypass never grants a whitelist bypass.
        when(player.hasPermission("csdm.maintenance.bypass")).thenReturn(true);
        listener.onLogin(event);
        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        verify(player, never()).setWhitelisted(anyBoolean());
    }

    @Test
    void switchingMaintenanceOffRestoresNormalWhitelistRejections() {
        var plugin = plugin(true);
        var listener = new AdminPlayerListener(plugin);
        listener.onLogin(login(mock(Player.class), PlayerLoginEvent.Result.KICK_WHITELIST));
        when(plugin.settings()).thenReturn(settings(false));
        var nextAttempt = login(mock(Player.class), PlayerLoginEvent.Result.KICK_WHITELIST);
        listener.onLogin(nextAttempt);
        assertEquals(ORIGINAL, nextAttempt.kickMessage());
        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, nextAttempt.getResult());
    }

    @Test
    void bansAndOtherRejectionReasonsArePreserved() {
        var listener = new AdminPlayerListener(plugin(true));
        for (var result : PlayerLoginEvent.Result.values()) {
            if (result == PlayerLoginEvent.Result.KICK_WHITELIST) continue;
            var event = login(mock(Player.class), result);
            listener.onLogin(event);
            assertEquals(result, event.getResult());
            assertEquals(ORIGINAL, event.kickMessage());
        }
    }

    @Test
    void localSanctionsStillTakePrecedenceOverMaintenanceText() {
        Player player = mock(Player.class);
        var event = login(player, PlayerLoginEvent.Result.KICK_WHITELIST);
        new AdminPlayerListener(plugin(true)).onLogin(event);
        SanctionService sanctions = mock(SanctionService.class);
        Sanction restriction = mock(Sanction.class);
        when(sanctions.activeRestriction(player.getUniqueId())).thenReturn(restriction);
        Component banned = Component.text("Acceso suspendido por sanción");
        when(sanctions.restrictionMessage(restriction)).thenReturn(banned);
        new SanctionListener(sanctions).onLogin(event);
        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, event.getResult());
        assertEquals(banned, event.kickMessage());
    }

    @Test
    void whitelistedPlayerWithoutBypassStillGetsMaintenanceAtJoin() {
        var listener = new AdminPlayerListener(plugin(true));
        Player player = mock(Player.class);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onJoin(event);
        verify(event).joinMessage(null);
        verify(player).kick(MESSAGE);
    }

    private static CSDMAdminPlugin plugin(boolean enabled) {
        var plugin = mock(CSDMAdminPlugin.class);
        when(plugin.settings()).thenReturn(settings(enabled));
        return plugin;
    }

    private static AdminSettings settings(boolean enabled) {
        var config = new YamlConfiguration();
        config.set("maintenance.enabled", enabled);
        config.set("maintenance.kick-message", TEXT);
        return AdminSettings.load(config);
    }

    private static PlayerLoginEvent login(Player player, PlayerLoginEvent.Result result) {
        return new PlayerLoginEvent(player, "verify.csdm.tv", InetAddress.getLoopbackAddress(),
                result, ORIGINAL, InetAddress.getLoopbackAddress());
    }
}
