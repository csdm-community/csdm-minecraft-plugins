package tv.csdm.minecraft.admin.staff;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffSnapshotStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public StaffSnapshotStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "staff-snapshots.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save(UUID playerId, StaffSnapshot snapshot) {
        String path = "snapshots." + playerId;
        data.set(path, null);
        data.set(path + ".location", snapshot.location());
        data.set(path + ".game-mode", snapshot.gameMode().name());
        data.set(path + ".allow-flight", snapshot.allowFlight());
        data.set(path + ".flying", snapshot.flying());
        data.set(path + ".invulnerable", snapshot.invulnerable());
        data.set(path + ".collidable", snapshot.collidable());
        data.set(path + ".can-pickup-items", snapshot.canPickupItems());
        data.set(path + ".held-slot", snapshot.heldSlot());
        data.set(path + ".offhand", snapshot.offhand());
        saveItems(path + ".storage", snapshot.storage());
        saveItems(path + ".armor", snapshot.armor());
        flush();
    }

    public synchronized StaffSnapshot load(UUID playerId) {
        String path = "snapshots." + playerId;
        if (!data.isConfigurationSection(path)) {
            return null;
        }
        Location location = data.getLocation(path + ".location");
        String rawMode = data.getString(path + ".game-mode", GameMode.ADVENTURE.name());
        if (location == null) {
            plugin.getLogger().severe("Snapshot sin ubicación para " + playerId);
            return null;
        }
        return new StaffSnapshot(
                loadItems(path + ".storage", 36),
                loadItems(path + ".armor", 4),
                data.getItemStack(path + ".offhand"),
                location,
                GameMode.valueOf(rawMode),
                data.getBoolean(path + ".allow-flight"),
                data.getBoolean(path + ".flying"),
                data.getBoolean(path + ".invulnerable"),
                data.getBoolean(path + ".collidable", true),
                data.getBoolean(path + ".can-pickup-items", true),
                data.getInt(path + ".held-slot"));
    }

    public synchronized boolean contains(UUID playerId) {
        return data.isConfigurationSection("snapshots." + playerId);
    }

    public synchronized void remove(UUID playerId) {
        data.set("snapshots." + playerId, null);
        flush();
    }

    private void saveItems(String path, ItemStack[] items) {
        data.set(path, null);
        for (int index = 0; index < items.length; index++) {
            if (items[index] != null) {
                data.set(path + "." + index, items[index]);
            }
        }
    }

    private ItemStack[] loadItems(String path, int size) {
        ItemStack[] items = new ItemStack[size];
        ConfigurationSection section = data.getConfigurationSection(path);
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < size) {
                    items[index] = section.getItemStack(key);
                }
            } catch (NumberFormatException ignored) {
                // Una clave ajena no invalida el resto del respaldo.
            }
        }
        return items;
    }

    private void flush() {
        try {
            data.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar staff-snapshots.yml", exception);
        }
    }
}
