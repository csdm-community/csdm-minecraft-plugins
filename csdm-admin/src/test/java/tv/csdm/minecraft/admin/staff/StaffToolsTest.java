package tv.csdm.minecraft.admin.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tv.csdm.minecraft.admin.listener.StaffModeListener;

class StaffToolsTest {
    @TempDir Path directory;

    @Test
    void inspectionRemainsReadOnlyEvenWhenViewerIsNoLongerInStaffMode() {
        StaffModeService service = mock(StaffModeService.class);
        StaffModeListener listener = new StaffModeListener(service);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(mock(StaffInspection.class));
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getView()).thenReturn(view);
        listener.onInventoryClick(click);
        verify(click).setCancelled(true);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        listener.onInventoryDrag(drag);
        verify(drag).setCancelled(true);
        verifyNoInteractions(service);
    }

    @Test
    void randomTeleportExcludesSelfHiddenPlayersAndOtherWorlds() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("CSDMAdmin");
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource("staff-messages.yml"))
                .thenAnswer(invocation -> getClass().getResourceAsStream("/staff-messages.yml"));
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        StaffModeService service = spy(new StaffModeService(plugin));
        World lobby = mock(World.class);
        Player staff = player(lobby);
        doReturn(true).when(service).isActive(staff);
        when(staff.hasPermission("csdm.staffmode.use")).thenReturn(true);
        Player hidden = player(lobby);
        Player otherWorld = player(mock(World.class));
        Player moderator = player(lobby);
        doReturn(true).when(service).isActive(moderator);
        Player target = player(lobby);
        when(staff.canSee(otherWorld)).thenReturn(true);
        when(staff.canSee(moderator)).thenReturn(true);
        when(staff.canSee(target)).thenReturn(true);
        Location destination = new Location(lobby, 1, 101, 1);
        when(target.getLocation()).thenReturn(destination);
        doReturn(List.of(staff, hidden, otherWorld, moderator, target)).when(server).getOnlinePlayers();
        service.teleportRandom(staff);
        verify(staff).teleport(destination);
        clearInvocations(staff);
        doReturn(List.of(staff, hidden, otherWorld, moderator)).when(server).getOnlinePlayers();
        service.teleportRandom(staff);
        verify(staff, never()).teleport(any(Location.class));
        clearInvocations(staff);
        doReturn(false).when(service).isActive(staff);
        service.teleportRandom(staff);
        verify(staff, never()).teleport(any(Location.class));
    }

    @Test
    void playTimeUsesTicksNotMinutesAndHandlesInvalidValues() {
        assertEquals("1 h 1 min", StaffModeService.playTime(61 * 1200));
        assertEquals("0 h 0 min", StaffModeService.playTime(-1));
    }

    private static Player player(World world) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Jugador");
        when(player.getWorld()).thenReturn(world);
        when(player.isOnline()).thenReturn(true);
        return player;
    }
}
