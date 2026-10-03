package tv.csdm.minecraft.admin.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Server;
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
import tv.csdm.minecraft.admin.moderation.SanctionRepository;

class StaffSanctionInteractionTest {
    @TempDir Path directory;

    @Test
    void historyCannotTransferItemsEvenOutsideStaffMode() {
        StaffModeService service = mock(StaffModeService.class);
        StaffModeListener listener = new StaffModeListener(service);
        StaffSanctionMenu menu = mock(StaffSanctionMenu.class);
        Inventory top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(menu);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        Player viewer = mock(Player.class);
        for (ClickType type : List.of(ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.SWAP_OFFHAND,
                ClickType.DOUBLE_CLICK, ClickType.DROP, ClickType.CONTROL_DROP)) {
            InventoryClickEvent event = mock(InventoryClickEvent.class);
            when(event.getView()).thenReturn(view);
            when(event.getWhoClicked()).thenReturn(viewer);
            when(event.getClick()).thenReturn(type);
            when(event.getRawSlot()).thenReturn(0);
            listener.onInventoryClick(event);
            verify(event).setCancelled(true);
        }
        InventoryClickEvent lower = mock(InventoryClickEvent.class);
        when(lower.getView()).thenReturn(view);
        when(lower.getWhoClicked()).thenReturn(viewer);
        when(lower.getRawSlot()).thenReturn(54);
        listener.onInventoryClick(lower);
        verify(lower).setCancelled(true);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        listener.onInventoryDrag(drag);
        verify(drag).setCancelled(true);
        verifyNoInteractions(service);
        InventoryClickEvent normal = mock(InventoryClickEvent.class);
        when(normal.getView()).thenReturn(view);
        when(normal.getWhoClicked()).thenReturn(viewer);
        when(normal.getRawSlot()).thenReturn(StaffSanctionMenu.FILTER);
        when(normal.getClick()).thenReturn(ClickType.LEFT);
        listener.onInventoryClick(normal);
        verify(service).clickSanctionHistory(viewer, menu, StaffSanctionMenu.FILTER);
    }

    @Test
    void historyActionsAreDeferredAndRequireCurrentOwnerSessionAndPermission() {
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
        doAnswer(inv -> { tasks.add(inv.getArgument(1, Runnable.class)); return mock(BukkitTask.class); })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        SanctionRepository repository = mock(SanctionRepository.class);
        StaffModeService service = spy(new StaffModeService(plugin, repository));
        Player viewer = mock(Player.class);
        // Neither the command nor the menu may query history without permission.
        service.openSanctionHistory(viewer, "Desconectado");
        verifyNoInteractions(repository);
        doReturn(true).when(service).isActive(viewer);
        when(viewer.hasPermission("csdm.staffmode.use")).thenReturn(true);
        when(viewer.hasPermission(StaffSanctionMenu.PERMISSION)).thenReturn(true);
        when(viewer.isOnline()).thenReturn(true);
        doNothing().when(service).openSanctionHistory(eq(viewer), nullable(UUID.class), nullable(String.class), anyBoolean(), anyInt());
        StaffSanctionMenu menu = mock(StaffSanctionMenu.class);
        when(menu.belongsTo(viewer)).thenReturn(true);
        InventoryView view = mock(InventoryView.class);
        Inventory top = mock(Inventory.class);
        when(viewer.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(top);
        when(top.getHolder()).thenReturn(menu);
        service.clickSanctionHistory(viewer, menu, StaffSanctionMenu.FILTER);
        verify(service, never()).openSanctionHistory(viewer, null, null, true, 0);
        tasks.removeFirst().run();
        verify(service).openSanctionHistory(viewer, null, null, true, 0);
        clearInvocations(service);
        service.clickSanctionHistory(viewer, menu, StaffSanctionMenu.FILTER);
        when(viewer.hasPermission(StaffSanctionMenu.PERMISSION)).thenReturn(false);
        tasks.removeFirst().run();
        verify(service, never()).openSanctionHistory(viewer, null, null, true, 0);
        when(viewer.hasPermission(StaffSanctionMenu.PERMISSION)).thenReturn(true);
        service.clickSanctionHistory(viewer, menu, StaffSanctionMenu.FILTER);
        when(top.getHolder()).thenReturn(null);
        tasks.removeFirst().run();
        verify(service, never()).openSanctionHistory(viewer, null, null, true, 0);
        when(menu.belongsTo(viewer)).thenReturn(false);
        service.clickSanctionHistory(viewer, menu, StaffSanctionMenu.FILTER);
        assertTrue(tasks.isEmpty());
        verifyNoInteractions(repository);
    }
}
