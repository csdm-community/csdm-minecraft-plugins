package tv.csdm.minecraft.community.medal;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MedalRegistry {
    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{1,48}$");
    private final Map<String, MedalDefinition> definitions;

    private MedalRegistry(Map<String, MedalDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public static MedalRegistry load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "medals.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("medals");
        if (section == null) {
            throw new IllegalArgumentException("medals.yml no contiene la seccion medals");
        }

        Map<String, MedalDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : section.getKeys(false)) {
            String id = normalizeId(rawId);
            if (!VALID_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("ID de medalla invalido: " + rawId);
            }
            ConfigurationSection medal = section.getConfigurationSection(rawId);
            if (medal == null) {
                continue;
            }
            definitions.put(id, new MedalDefinition(
                    id,
                    medal.getString("name", id),
                    medal.getString("symbol", "◆"),
                    medal.getString("color", "#00E5FF"),
                    medal.getString("description", "")));
        }
        return new MedalRegistry(definitions);
    }

    public Optional<MedalDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(normalizeId(id)));
    }

    public Collection<MedalDefinition> all() {
        return definitions.values();
    }

    public static String normalizeId(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

