package tv.csdm.minecraft.verify.config;

import java.io.File;
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
    private final String prefix;

    private Messages(YamlConfiguration configuration) {
        this.miniMessage = MiniMessage.miniMessage();
        this.configuration = configuration;
        this.prefix = configuration.getString("prefix", "");
    }

    public static Messages load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        return new Messages(YamlConfiguration.loadConfiguration(file));
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
        return configuration.getString(path, "<red>Mensaje faltante: " + path + "</red>");
    }

    private Component deserialize(String template, Map<String, String> placeholders) {
        List<TagResolver> resolvers = new ArrayList<>();
        placeholders.forEach((key, value) ->
                resolvers.add(Placeholder.component(key, Component.text(value))));
        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }
}

