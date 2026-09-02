package tv.csdm.minecraft.admin.moderation;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SanctionRepository {
    public record KnownTarget(UUID uuid, String name) {
    }
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public SanctionRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sanctions.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save(Sanction sanction) {
        String path = "sanctions." + sanction.id();
        data.set(path + ".target-uuid", sanction.targetUuid().toString());
        data.set(path + ".target-name", sanction.targetName());
        data.set(path + ".actor-uuid", sanction.actorUuid().toString());
        data.set(path + ".actor-name", sanction.actorName());
        data.set(path + ".type", sanction.type().name());
        data.set(path + ".reason", sanction.reason());
        data.set(path + ".created-at", sanction.createdAt().toString());
        data.set(path + ".expires-at", sanction.expiresAt() == null ? null : sanction.expiresAt().toString());
        data.set(path + ".active", sanction.active());
        data.set(path + ".public-announcement", sanction.publicAnnouncement());
        flush();
    }

    public synchronized Sanction activeRestriction(UUID playerId, Instant now) {
        ConfigurationSection root = data.getConfigurationSection("sanctions");
        if (root == null) {
            return null;
        }
        return root.getKeys(false).stream()
                .map(id -> read(root.getConfigurationSection(id), id))
                .filter(java.util.Objects::nonNull)
                .filter(sanction -> sanction.targetUuid().equals(playerId))
                .filter(Sanction::active)
                .filter(Sanction::restriction)
                .filter(sanction -> !sanction.expired(now))
                .max(Comparator.comparing(Sanction::createdAt))
                .orElse(null);
    }

    public synchronized int pardon(UUID playerId) {
        ConfigurationSection root = data.getConfigurationSection("sanctions");
        if (root == null) {
            return 0;
        }
        int changed = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            Sanction sanction = read(section, id);
            if (sanction != null && sanction.targetUuid().equals(playerId)
                    && sanction.active() && sanction.restriction()) {
                section.set("active", false);
                changed++;
            }
        }
        if (changed > 0) {
            flush();
        }
        return changed;
    }

    public synchronized KnownTarget findTarget(String playerName) {
        ConfigurationSection root = data.getConfigurationSection("sanctions");
        if (root == null) {
            return null;
        }
        return root.getKeys(false).stream()
                .map(id -> read(root.getConfigurationSection(id), id))
                .filter(java.util.Objects::nonNull)
                .filter(sanction -> sanction.targetName().equalsIgnoreCase(playerName))
                .max(Comparator.comparing(Sanction::createdAt))
                .map(sanction -> new KnownTarget(sanction.targetUuid(), sanction.targetName()))
                .orElse(null);
    }

    private Sanction read(ConfigurationSection section, String id) {
        if (section == null) {
            return null;
        }
        try {
            String expires = section.getString("expires-at");
            return new Sanction(
                    UUID.fromString(id),
                    UUID.fromString(require(section, "target-uuid")),
                    require(section, "target-name"),
                    UUID.fromString(require(section, "actor-uuid")),
                    require(section, "actor-name"),
                    SanctionType.valueOf(require(section, "type")),
                    require(section, "reason"),
                    Instant.parse(require(section, "created-at")),
                    expires == null || expires.isBlank() ? null : Instant.parse(expires),
                    section.getBoolean("active"),
                    section.getBoolean("public-announcement"));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Se ignoró una sanción local inválida: " + id);
            return null;
        }
    }

    private String require(ConfigurationSection section, String key) {
        String value = section.getString(key);
        if (value == null) {
            throw new IllegalArgumentException("Falta " + key);
        }
        return value;
    }

    private void flush() {
        try {
            data.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar sanctions.yml", exception);
        }
    }
}
