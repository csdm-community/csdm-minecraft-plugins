package tv.csdm.minecraft.community.medal;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlMedalRepository implements MedalRepository {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public YamlMedalRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-medals.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized MedalProfile find(UUID playerUuid) {
        String path = base(playerUuid);
        Set<String> unlocked = new LinkedHashSet<>(yaml.getStringList(path + ".unlocked"));
        String featured = yaml.getString(path + ".featured");
        return new MedalProfile(unlocked, featured);
    }

    @Override
    public synchronized boolean grant(UUID playerUuid, String medalId) {
        MedalProfile profile = find(playerUuid);
        Set<String> unlocked = new LinkedHashSet<>(profile.unlocked());
        if (!unlocked.add(medalId)) {
            return false;
        }
        yaml.set(base(playerUuid) + ".unlocked", List.copyOf(unlocked));
        save();
        return true;
    }

    @Override
    public synchronized boolean revoke(UUID playerUuid, String medalId) {
        MedalProfile profile = find(playerUuid);
        Set<String> unlocked = new LinkedHashSet<>(profile.unlocked());
        if (!unlocked.remove(medalId)) {
            return false;
        }
        String path = base(playerUuid);
        yaml.set(path + ".unlocked", List.copyOf(unlocked));
        if (medalId.equals(profile.featured())) {
            yaml.set(path + ".featured", null);
        }
        save();
        return true;
    }

    @Override
    public synchronized boolean feature(UUID playerUuid, String medalId) {
        MedalProfile profile = find(playerUuid);
        if (!profile.unlocked().contains(medalId)) {
            return false;
        }
        yaml.set(base(playerUuid) + ".featured", medalId);
        save();
        return true;
    }

    private String base(UUID playerUuid) {
        return "players." + playerUuid;
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("No se pudo guardar player-medals.yml: " + exception.getMessage());
            throw new IllegalStateException("No se pudieron guardar las medallas", exception);
        }
    }
}

