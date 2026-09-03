package tv.csdm.minecraft.verify.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {
    private final MiniMessage miniMessage;
    private final YamlConfiguration configuration;
    private final YamlConfiguration defaults;
    private final String prefix;

    private Messages(YamlConfiguration configuration, YamlConfiguration defaults) {
        this.miniMessage = MiniMessage.miniMessage();
        this.configuration = configuration;
        this.defaults = defaults;
        this.prefix = configuration.getString("prefix", "");
    }

    public static Messages load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration bundledDefaults = new YamlConfiguration();
        try (var stream = plugin.getResource("messages.yml")) {
            if (stream == null) {
                throw new IllegalArgumentException("Falta messages.yml dentro del plugin");
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                bundledDefaults = YamlConfiguration.loadConfiguration(reader);
                boolean changed = copyMissingValues(configuration, bundledDefaults);
                if (changed) {
                    configuration.save(file);
                    plugin.getLogger().info("Se incorporaron mensajes nuevos a messages.yml.");
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "No se pudieron incorporar los mensajes nuevos: " + exception.getMessage());
        }
        return new Messages(configuration, bundledDefaults);
    }

    public Component prefixed(String path) {
        return deserialize(prefix + raw(path), Map.of());
    }

    public Component prefixed(String path, Map<String, String> placeholders) {
        return deserialize(prefix + raw(path), placeholders);
    }

    public Component plain(String path) {
        return deserialize(raw(path), Map.of());
    }

    private String raw(String path) {
        String bundledDefault = defaults.getString(path, "<red>Mensaje faltante: " + path + "</red>");
        return configuration.getString(path, bundledDefault);
    }

    private Component deserialize(String template, Map<String, String> placeholders) {
        List<TagResolver> resolvers = new ArrayList<>();
        placeholders.forEach((key, value) ->
                resolvers.add(Placeholder.component(key, Component.text(value))));
        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }

    static boolean copyMissingValues(
            YamlConfiguration configuration,
            YamlConfiguration defaults) {
        boolean changed = false;
        for (String path : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(path) || configuration.contains(path)) {
                continue;
            }
            configuration.set(path, defaults.get(path));
            changed = true;
        }
        return changed;
    }
}
