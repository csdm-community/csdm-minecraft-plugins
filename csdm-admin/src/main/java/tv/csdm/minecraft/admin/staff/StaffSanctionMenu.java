package tv.csdm.minecraft.admin.staff;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import tv.csdm.minecraft.admin.moderation.Sanction;

/** Read-only local history; opening or filtering it never changes a sanction. */
public final class StaffSanctionMenu implements InventoryHolder {
    public static final String PERMISSION = "csdm.sanctions.view";
    public static final int PREVIOUS = 45, ALL_PLAYERS = 46, FILTER = 48, REFRESH = 49, CLOSE = 50, NEXT = 53;
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);
    private final UUID owner;
    private final UUID target;
    private final String targetName;
    private final boolean activeOnly;
    private final Page page;
    private final Inventory inventory;

    public StaffSanctionMenu(Player viewer, Collection<Sanction> history, UUID target, String name,
            boolean activeOnly, int requestedPage) {
        this.owner = viewer.getUniqueId();
        this.target = target;
        this.targetName = name;
        this.activeOnly = activeOnly;
        Instant now = Instant.now();
        this.page = paginate(history, target, activeOnly, requestedPage, now);
        inventory = Bukkit.createInventory(this, 54, Component.text(
                "Sanciones • " + (target == null ? "Todos" : name) + " • " + (page.number() + 1) + "/" + page.total(),
                NamedTextColor.DARK_AQUA));
        for (int slot = 0; slot < page.entries().size(); slot++) {
            Sanction sanction = page.entries().get(slot);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Staff: " + sanction.actorName(), NamedTextColor.GRAY));
            lore.add(Component.text("Estado: " + status(sanction, now), NamedTextColor.YELLOW));
            lore.add(Component.text("Fecha: " + DATE.format(sanction.createdAt()), NamedTextColor.GRAY));
            if (sanction.restriction()) {
                lore.add(Component.text("Vencimiento: " + (sanction.expiresAt() == null ? "Permanente"
                        : DATE.format(sanction.expiresAt())), NamedTextColor.GRAY));
            }
            lore.add(Component.text("Motivo:", NamedTextColor.WHITE));
            for (String line : wrap(sanction.reason())) lore.add(Component.text(line, NamedTextColor.GRAY));
            lore.add(Component.text("ID: " + sanction.id(), NamedTextColor.DARK_GRAY));
            lore.add(Component.text("Clic: historial de este jugador", NamedTextColor.AQUA));
            Material icon = switch (sanction.type()) {
                case WARNING -> Material.PAPER;
                case KICK -> Material.LEATHER_BOOTS;
                case TEMPBAN -> Material.CLOCK;
                case BAN -> Material.BARRIER;
                case PARDON -> Material.LIME_DYE;
            };
            inventory.setItem(slot, item(icon, sanction.targetName() + " • " + sanction.type().displayName(), lore));
        }
        if (page.entries().isEmpty()) inventory.setItem(22, item(Material.GRAY_DYE, "Sin sanciones para este filtro", List.of()));
        if (page.number() > 0) inventory.setItem(PREVIOUS, item(Material.ARROW, "Página anterior", List.of()));
        if (target != null) inventory.setItem(ALL_PLAYERS, item(Material.BOOKSHELF, "Ver todos los jugadores", List.of()));
        inventory.setItem(FILTER, item(Material.HOPPER, activeOnly ? "Filtro: restricciones activas" : "Filtro: todo el historial",
                List.of(Component.text("Clic para cambiar • " + page.count() + " registros", NamedTextColor.GRAY))));
        inventory.setItem(REFRESH, item(Material.SUNFLOWER, "Actualizar historial", List.of()));
        inventory.setItem(CLOSE, item(Material.BARRIER, "Cerrar", List.of()));
        if (page.number() + 1 < page.total()) inventory.setItem(NEXT, item(Material.ARROW, "Página siguiente", List.of()));
    }

    static Page paginate(Collection<Sanction> history, UUID target, boolean activeOnly, int requestedPage, Instant now) {
        List<Sanction> entries = history.stream()
                .filter(s -> target == null || s.targetUuid().equals(target))
                .filter(s -> !activeOnly || s.restriction() && s.active() && !s.expired(now))
                .sorted(Comparator.comparing(Sanction::createdAt).reversed().thenComparing(Sanction::id))
                .toList();
        int total = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int number = Math.max(0, Math.min(requestedPage, total - 1));
        int start = Math.min(number * PAGE_SIZE, entries.size());
        return new Page(number, total, entries.size(), List.copyOf(entries.subList(start, Math.min(start + PAGE_SIZE, entries.size()))));
    }

    static String status(Sanction sanction, Instant now) {
        if (!sanction.restriction()) return switch (sanction.type()) {
            case WARNING -> "Advertencia registrada";
            case KICK -> "Expulsión ejecutada";
            case PARDON -> "Perdón aplicado";
            default -> throw new IllegalStateException("Tipo de sanción desconocido");
        };
        if (!sanction.active()) return "Retirada";
        return sanction.expired(now) ? "Finalizada" : "Activa";
    }

    static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        String remaining = text.replaceAll("\\s+", " ").strip();
        while (remaining.length() > 48) {
            int cut = remaining.lastIndexOf(' ', 48);
            if (cut < 1) cut = 48;
            lines.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).stripLeading();
        }
        if (!remaining.isEmpty()) lines.add(remaining);
        return List.copyOf(lines);
    }

    private static ItemStack item(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean belongsTo(Player viewer) { return owner.equals(viewer.getUniqueId()); }
    public UUID target() { return target; }
    public String targetName() { return targetName; }
    public boolean activeOnly() { return activeOnly; }
    public int page() { return page.number(); }
    public int pages() { return page.total(); }
    public Sanction entryAt(int slot) { return slot >= 0 && slot < page.entries().size() ? page.entries().get(slot) : null; }
    @Override public Inventory getInventory() { return inventory; }
    record Page(int number, int total, int count, List<Sanction> entries) { }
}
