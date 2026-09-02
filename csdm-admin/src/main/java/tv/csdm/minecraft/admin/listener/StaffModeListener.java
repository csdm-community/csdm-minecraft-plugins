package tv.csdm.minecraft.admin.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tv.csdm.minecraft.admin.staff.StaffModeService;

public final class StaffModeListener implements Listener {
    private final StaffModeService staffMode;

    public StaffModeListener(StaffModeService staffMode) {
        this.staffMode = staffMode;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        staffMode.restoreStaleSnapshot(event.getPlayer());
        staffMode.hideActiveStaffFrom(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        staffMode.clearFreeze(event.getPlayer());
        if (staffMode.isActive(event.getPlayer())) {
            staffMode.disable(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (!staffMode.isFrozen(event.getPlayer()) || !event.hasChangedBlock()) {
            return;
        }
        var from = event.getFrom().clone();
        from.setYaw(event.getTo().getYaw());
        from.setPitch(event.getTo().getPitch());
        event.setTo(from);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToolUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffMode.isActive(player)) {
            return;
        }
        String action = staffMode.action(event.getItem());
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        switch (action) {
            case StaffModeService.VANISH -> {
                boolean vanished = staffMode.toggleVanish(player);
                player.sendMessage(Component.text(
                        vanished ? "Vanish activado." : "Vanish desactivado.",
                        vanished ? NamedTextColor.AQUA : NamedTextColor.YELLOW));
            }
            case StaffModeService.EXIT -> staffMode.disable(player);
            case StaffModeService.TELEPORT -> showTargets(player, "tp", false);
            case StaffModeService.FREEZE -> showTargets(player, "congelar", false);
            case StaffModeService.INSPECT -> showTargets(player, "inspeccionar", false);
            case StaffModeService.SANCTION -> showTargets(player, "", true);
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerToolUse(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        Entity clicked = event.getRightClicked();
        if (!staffMode.isActive(staff) || !(clicked instanceof Player target)) {
            return;
        }
        String action = staffMode.action(staff.getInventory().getItemInMainHand());
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        switch (action) {
            case StaffModeService.TELEPORT -> staff.teleportAsync(target.getLocation());
            case StaffModeService.FREEZE -> {
                boolean frozen = staffMode.toggleFreeze(target);
                staff.sendMessage(Component.text(
                        target.getName() + (frozen ? " quedó congelado." : " fue liberado."),
                        NamedTextColor.AQUA));
            }
            case StaffModeService.INSPECT -> staffMode.openInspection(staff, target);
            case StaffModeService.SANCTION -> staff.sendMessage(Component.text(
                            "Preparar sanción para " + target.getName(), NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.suggestCommand("/sancionar " + target.getName() + " ")));
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && staffMode.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && staffMode.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (staffMode.isActive(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && staffMode.isActive(player)) {
            event.setCancelled(true);
        }
    }

    private void showTargets(Player staff, String staffAction, boolean sanction) {
        staff.sendMessage(Component.text("Selecciona un jugador:", NamedTextColor.AQUA));
        for (Player target : staff.getServer().getOnlinePlayers()) {
            if (target.equals(staff)) {
                continue;
            }
            String command = sanction
                    ? "/sancionar " + target.getName() + " "
                    : "/staff " + staffAction + " " + target.getName();
            Component entry = Component.text("• " + target.getName(), NamedTextColor.WHITE)
                    .clickEvent(sanction ? ClickEvent.suggestCommand(command) : ClickEvent.runCommand(command))
                    .hoverEvent(HoverEvent.showText(Component.text("Haz clic para seleccionar", NamedTextColor.GRAY)));
            staff.sendMessage(entry);
        }
    }
}
