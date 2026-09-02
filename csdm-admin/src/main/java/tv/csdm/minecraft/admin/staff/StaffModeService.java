package tv.csdm.minecraft.admin.staff;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffModeService {
    public static final String TELEPORT = "teleport";
    public static final String VANISH = "vanish";
    public static final String FREEZE = "freeze";
    public static final String INSPECT = "inspect";
    public static final String SANCTION = "sanction";
    public static final String EXIT = "exit";

    private final JavaPlugin plugin;
    private final StaffSnapshotStore snapshots;
    private final NamespacedKey actionKey;
    private final Set<UUID> active = new HashSet<>();
    private final Set<UUID> visible = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();

    public StaffModeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.snapshots = new StaffSnapshotStore(plugin);
        this.actionKey = new NamespacedKey(plugin, "staff_action");
    }

    public boolean isActive(Player player) {
        return active.contains(player.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return frozen.contains(player.getUniqueId());
    }

    public boolean toggleFreeze(Player target) {
        if (!frozen.add(target.getUniqueId())) {
            frozen.remove(target.getUniqueId());
            return false;
        }
        return true;
    }

    public void clearFreeze(Player target) {
        frozen.remove(target.getUniqueId());
    }

    public boolean enable(Player player) {
        if (isActive(player)) {
            return false;
        }
        StaffSnapshot snapshot = capture(player);
        snapshots.save(player.getUniqueId(), snapshot);
        active.add(player.getUniqueId());
        visible.remove(player.getUniqueId());

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setCanPickupItems(false);
        installTools(player);
        applyVisibility(player);
        player.sendMessage(Component.text("Staff Mode activado.", NamedTextColor.AQUA));
        return true;
    }

    public boolean disable(Player player) {
        if (!active.remove(player.getUniqueId())) {
            return false;
        }
        visible.remove(player.getUniqueId());
        frozen.remove(player.getUniqueId());
        restoreStored(player);
        showToEveryone(player);
        player.sendMessage(Component.text("Staff Mode desactivado y estado restaurado.", NamedTextColor.GREEN));
        return true;
    }

    public void restoreStaleSnapshot(Player player) {
        if (snapshots.contains(player.getUniqueId())) {
            restoreStored(player);
            showToEveryone(player);
            player.sendMessage(Component.text(
                    "Se restauró tu inventario anterior tras un cierre inesperado.",
                    NamedTextColor.YELLOW));
        }
    }

    public void restoreAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isActive(player)) {
                disable(player);
            }
        }
    }

    public boolean toggleVanish(Player player) {
        if (!isActive(player)) {
            return false;
        }
        if (!visible.add(player.getUniqueId())) {
            visible.remove(player.getUniqueId());
        }
        applyVisibility(player);
        return !visible.contains(player.getUniqueId());
    }

    public void hideActiveStaffFrom(Player viewer) {
        if (viewer.hasPermission("csdm.staffmode.see")) {
            return;
        }
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (isActive(staff) && !visible.contains(staff.getUniqueId())) {
                viewer.hidePlayer(plugin, staff);
            }
        }
    }

    public String action(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public void openInspection(Player viewer, Player target) {
        if (!isActive(viewer)) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                Component.text("Inventario de " + target.getName(), NamedTextColor.DARK_AQUA));
        ItemStack[] storage = target.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length && slot < 36; slot++) {
            inventory.setItem(slot, cloneItem(storage[slot]));
        }
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            inventory.setItem(45 + slot, cloneItem(armor[slot]));
        }
        inventory.setItem(49, cloneItem(target.getInventory().getItemInOffHand()));
        viewer.openInventory(inventory);
    }

    private StaffSnapshot capture(Player player) {
        return new StaffSnapshot(
                cloneItems(player.getInventory().getStorageContents()),
                cloneItems(player.getInventory().getArmorContents()),
                cloneItem(player.getInventory().getItemInOffHand()),
                player.getLocation().clone(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying(),
                player.isInvulnerable(),
                player.isCollidable(),
                player.getCanPickupItems(),
                player.getInventory().getHeldItemSlot());
    }

    private void restoreStored(Player player) {
        StaffSnapshot snapshot = snapshots.load(player.getUniqueId());
        if (snapshot == null) {
            plugin.getLogger().severe("No existe snapshot restaurable para " + player.getName());
            return;
        }
        player.getInventory().clear();
        player.getInventory().setStorageContents(cloneItems(snapshot.storage()));
        player.getInventory().setArmorContents(cloneItems(snapshot.armor()));
        player.getInventory().setItemInOffHand(cloneItem(snapshot.offhand()));
        player.getInventory().setHeldItemSlot(snapshot.heldSlot());
        player.setGameMode(snapshot.gameMode());
        player.setAllowFlight(snapshot.allowFlight());
        player.setFlying(snapshot.allowFlight() && snapshot.flying());
        player.setInvulnerable(snapshot.invulnerable());
        player.setCollidable(snapshot.collidable());
        player.setCanPickupItems(snapshot.canPickupItems());
        player.teleport(snapshot.location());
        snapshots.remove(player.getUniqueId());
    }

    private void installTools(Player player) {
        player.getInventory().setItem(0, tool(Material.COMPASS, "Teletransportarse", TELEPORT));
        player.getInventory().setItem(1, tool(Material.ENDER_EYE, "Vanish", VANISH));
        player.getInventory().setItem(2, tool(Material.BLUE_ICE, "Congelar", FREEZE));
        player.getInventory().setItem(3, tool(Material.CHEST, "Inspeccionar inventario", INSPECT));
        player.getInventory().setItem(4, tool(Material.IRON_AXE, "Sancionar", SANCTION));
        player.getInventory().setItem(8, tool(Material.RED_DYE, "Salir de Staff Mode", EXIT));
    }

    private ItemStack tool(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void applyVisibility(Player staff) {
        boolean vanished = !visible.contains(staff.getUniqueId());
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(staff)) {
                continue;
            }
            if (vanished && !viewer.hasPermission("csdm.staffmode.see")) {
                viewer.hidePlayer(plugin, staff);
            } else {
                viewer.showPlayer(plugin, staff);
            }
        }
    }

    private void showToEveryone(Player staff) {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            viewer.showPlayer(plugin, staff);
        }
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clone[i] = cloneItem(items[i]);
        }
        return clone;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }
}
