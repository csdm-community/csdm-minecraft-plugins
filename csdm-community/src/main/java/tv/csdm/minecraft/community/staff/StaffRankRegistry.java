package tv.csdm.minecraft.community.staff;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class StaffRankRegistry {
    private final Map<String, StaffRankDefinition> definitions;

    private StaffRankRegistry(Map<String, StaffRankDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public static StaffRankRegistry load(FileConfiguration config) {
        Map<String, StaffRankDefinition> definitions = new LinkedHashMap<>();
        loadSection(config, definitions, "functional-ranks", RankKind.FUNCTIONAL);
        loadSection(config, definitions, "prestige-ranks", RankKind.PRESTIGE);
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("config.yml no contiene rangos CSDM");
        }
        return new StaffRankRegistry(definitions);
    }

    private static void loadSection(
            FileConfiguration config,
            Map<String, StaffRankDefinition> definitions,
            String path,
            RankKind kind) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("config.yml no contiene " + path);
        }
        for (String rawId : section.getKeys(false)) {
            String id = normalize(rawId);
            ConfigurationSection rank = section.getConfigurationSection(rawId);
            if (rank == null) {
                continue;
            }
            String group = normalize(rank.getString("group", "csdm-" + id));
            String displayName = rank.getString("display-name", id);
            String prefix = rank.getString("prefix", "");
            definitions.put(id, new StaffRankDefinition(
                    id,
                    group,
                    displayName,
                    prefix,
                    rank.getString("nametag-label", defaultNametagLabel(prefix, displayName)),
                    rank.getInt("priority", 1),
                    kind,
                    rank.getStringList("inherits"),
                    rank.getStringList("permissions")));
        }
    }

    public Optional<StaffRankDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(normalize(id)));
    }

    public Optional<StaffRankDefinition> byGroup(String group) {
        String normalized = normalize(group);
        return definitions.values().stream()
                .filter(rank -> rank.group().equals(normalized))
                .max(Comparator.comparingInt(StaffRankDefinition::priority));
    }

    public Collection<StaffRankDefinition> all() {
        return definitions.values();
    }

    public Set<String> managedGroups() {
        return definitions.values().stream().map(StaffRankDefinition::group).collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> managedGroups(RankKind kind) {
        return definitions.values().stream()
                .filter(rank -> rank.kind() == kind)
                .map(StaffRankDefinition::group)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String defaultNametagLabel(String prefix, String displayName) {
        String withoutSeparator = prefix.replaceFirst(
                "(?i)\\s*<dark_gray>\\s*•\\s*</dark_gray>\\s*$", "").trim();
        return withoutSeparator.isEmpty() ? displayName : withoutSeparator;
    }
}
