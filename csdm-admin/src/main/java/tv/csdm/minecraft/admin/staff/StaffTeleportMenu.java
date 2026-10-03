package tv.csdm.minecraft.admin.staff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/** A viewer-owned snapshot. Slots resolve to UUIDs, never item names or lore. */
public final class StaffTeleportMenu implements InventoryHolder {
    public static final String PRIORITY_PERMISSION = "csdm.staffmode.priority";
    public static final int PREVIOUS = 45;
    public static final int REFRESH = 49;
    public static final int CLOSE = 50;
    public static final int NEXT = 53;
    private static final int GROUP_SIZE = 18;

    private final UUID owner;
    private final Page page;
    private final Inventory inventory;
    private final Map<Integer, UUID> targets = new HashMap<>();

    public StaffTeleportMenu(Player viewer, Collection<? extends Player> online, int requestedPage) {
        owner = viewer.getUniqueId();
        page = paginate(viewer, online, requestedPage);
        inventory = Bukkit.createInventory(this, 54, Component.text(
                "Brújula • Jugadores " + (page.number() + 1) + "/" + page.total(), NamedTextColor.DARK_AQUA));
        putHeads(page.staff(), 0, "Staff / prioridad");
        putHeads(page.players(), 27, "Jugador");
        for (int slot = 18; slot < 27; slot++) {
            inventory.setItem(slot, button(Material.CYAN_STAINED_GLASS_PANE, "↑ Staff y prioridad • Jugadores ↓"));
        }
        if (page.staff().isEmpty()) {
            inventory.setItem(4, button(Material.GRAY_DYE, "Sin staff en esta página"));
        }
        if (page.players().isEmpty()) {
            inventory.setItem(31, button(Material.GRAY_DYE, "Sin jugadores en esta página"));
        }
        if (page.number() > 0) {
            inventory.setItem(PREVIOUS, button(Material.ARROW, "Página anterior"));
        }
        inventory.setItem(48, button(Material.BOOK,
                "Staff/prioridad: " + page.staffCount() + " • Jugadores: " + page.playerCount()));
        inventory.setItem(REFRESH, button(Material.SUNFLOWER, "Actualizar jugadores"));
        inventory.setItem(CLOSE, button(Material.BARRIER, "Cerrar"));
        if (page.number() + 1 < page.total()) {
            inventory.setItem(NEXT, button(Material.ARROW, "Página siguiente"));
        }
    }

    static Page paginate(Player viewer, Collection<? extends Player> online, int requestedPage) {
        List<Player> staff = new ArrayList<>();
        List<Player> players = new ArrayList<>();
        for (Player target : online) {
            if (!target.isOnline() || target.getUniqueId().equals(viewer.getUniqueId()) || !viewer.canSee(target)) {
                continue;
            }
            if (target.hasPermission("csdm.staffmode.use") || target.hasPermission(PRIORITY_PERMISSION)) {
                staff.add(target);
            } else {
                players.add(target);
            }
        }
        Comparator<Player> order = Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Player::getUniqueId);
        staff.sort(order);
        players.sort(order);
        int total = Math.max(1, (Math.max(staff.size(), players.size()) + GROUP_SIZE - 1) / GROUP_SIZE);
        int number = Math.max(0, Math.min(requestedPage, total - 1));
        return new Page(number, total, slice(staff, number), slice(players, number), staff.size(), players.size());
    }

    private static List<Player> slice(List<Player> players, int page) {
        int start = Math.min(page * GROUP_SIZE, players.size());
        return List.copyOf(players.subList(start, Math.min(start + GROUP_SIZE, players.size())));
    }

    private void putHeads(List<Player> players, int start, String category) {
        for (int index = 0; index < players.size(); index++) {
            Player target = players.get(index);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(target.displayName());
            meta.lore(List.of(
                    Component.text(category, NamedTextColor.AQUA),
                    Component.text("Mundo: " + target.getWorld().getName(), NamedTextColor.GRAY),
                    Component.text("Haz clic para teletransportarte", NamedTextColor.YELLOW)));
            head.setItemMeta(meta);
            int slot = start + index;
            inventory.setItem(slot, head);
            targets.put(slot, target.getUniqueId());
        }
    }

    private static ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA));
        item.setItemMeta(meta);
        return item;
    }

    public boolean belongsTo(Player viewer) {
        return owner.equals(viewer.getUniqueId());
    }

    public int page() { return page.number(); }
    public int pages() { return page.total(); }
    public UUID targetAt(int slot) { return targets.get(slot); }

    @Override
    public Inventory getInventory() { return inventory; }

    record Page(int number, int total, List<Player> staff, List<Player> players, int staffCount, int playerCount) { }
}
