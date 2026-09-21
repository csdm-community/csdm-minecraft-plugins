package tv.csdm.minecraft.admin.staff;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tv.csdm.minecraft.admin.listener.StaffModeListener;

class StaffTeleportInteractionTest {
    @TempDir Path directory;

    @Test
    void menuBlocksTransfersAndOnlyDispatchesNormalClicksInTopInventory() {
        StaffModeService service = mock(StaffModeService.class);
        StaffModeListener listener = new StaffModeListener(service);
        StaffTeleportMenu menu = mock(StaffTeleportMenu.class);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(menu);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        Player viewer = mock(Player.class);
        for (ClickType type : List.of(ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY,
                ClickType.SWAP_OFFHAND, ClickType.DOUBLE_CLICK, ClickType.DROP)) {
            InventoryClickEvent event = click(view, viewer, type, 0);
            listener.onInventoryClick(event);
            verify(event).setCancelled(true);
        }
        InventoryClickEvent lower = click(view, viewer, ClickType.LEFT, 54);
        listener.onInventoryClick(lower);
        verify(lower).setCancelled(true);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        listener.onInventoryDrag(drag);
        verify(drag).setCancelled(true);
        verifyNoInteractions(service);
        InventoryClickEvent normal = click(view, viewer, ClickType.LEFT, 27);
        listener.onInventoryClick(normal);
        verify(normal).setCancelled(true);
        verify(service).clickTeleportMenu(viewer, menu, 27);
    }

    @Test
    void teleportRunsNextTickAndRevalidatesSessionPermissionsAndTarget() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("CSDMAdmin");
        when(plugin.namespace()).thenReturn("csdmadmin");
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource("staff-messages.yml"))
                .thenAnswer(inv -> getClass().getResourceAsStream("/staff-messages.yml"));
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        List<Runnable> tasks = new ArrayList<>();
        doAnswer(inv -> {
            tasks.add(inv.getArgument(1, Runnable.class));
            return mock(BukkitTask.class);
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        StaffModeService service = spy(new StaffModeService(plugin, mock(tv.csdm.minecraft.admin.moderation.SanctionRepository.class)));
        Player viewer = mock(Player.class);
        when(viewer.getName()).thenReturn("Staff");
        when(viewer.isOnline()).thenReturn(true);
        when(viewer.hasPermission("csdm.staffmode.use")).thenReturn(true);
        doReturn(true).when(service).isActive(viewer);
        doNothing().when(service).openTeleportMenu(eq(viewer), anyInt());
        StaffTeleportMenu menu = mock(StaffTeleportMenu.class);
        when(menu.belongsTo(viewer)).thenReturn(true);
        InventoryView view = mock(InventoryView.class);
        Inventory inventory = mock(Inventory.class);
        when(viewer.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(inventory);
        when(inventory.getHolder()).thenReturn(menu);
        UUID id = UUID.randomUUID();
        when(menu.targetAt(27)).thenReturn(id);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Destino");
        when(target.isOnline()).thenReturn(true);
        when(server.getPlayer(id)).thenReturn(target);
        when(viewer.canSee(target)).thenReturn(true);
        Location location = new Location(mock(World.class), 10, 101, 10);
        when(target.getLocation()).thenReturn(location);
        when(viewer.teleport(location)).thenReturn(true);

        service.clickTeleportMenu(viewer, menu, 27);
        verify(viewer, never()).teleport(any(Location.class));
        tasks.removeFirst().run();
        verify(viewer).teleport(location);
        verify(viewer).closeInventory();
        clearInvocations(viewer);

        // A target disappearing after the click must not be teleported to.
        service.clickTeleportMenu(viewer, menu, 27);
        when(server.getPlayer(id)).thenReturn(null);
        tasks.removeFirst().run();
        verify(viewer, never()).teleport(any(Location.class));
        verify(service).openTeleportMenu(viewer, 0);
        when(server.getPlayer(id)).thenReturn(target);
        service.clickTeleportMenu(viewer, menu, 27);
        when(viewer.canSee(target)).thenReturn(false);
        tasks.removeFirst().run();
        verify(viewer, never()).teleport(any(Location.class));
        when(viewer.canSee(target)).thenReturn(true);

        service.clickTeleportMenu(viewer, menu, 27);
        when(viewer.hasPermission("csdm.staffmode.use")).thenReturn(false);
        tasks.removeFirst().run();
        verify(viewer, never()).teleport(any(Location.class));
        when(viewer.hasPermission("csdm.staffmode.use")).thenReturn(true);
        service.clickTeleportMenu(viewer, menu, 27);
        when(inventory.getHolder()).thenReturn(null);
        tasks.removeFirst().run();
        verify(viewer, never()).teleport(any(Location.class));

        when(menu.belongsTo(viewer)).thenReturn(false);
        service.clickTeleportMenu(viewer, menu, 27);
        assertTrue(tasks.isEmpty());
    }

    private static InventoryClickEvent click(InventoryView view, Player viewer, ClickType type, int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(viewer);
        when(event.getClick()).thenReturn(type);
        when(event.getRawSlot()).thenReturn(slot);
        return event;
    }
}
