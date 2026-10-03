package tv.csdm.minecraft.admin.staff;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Presentation only: sending a notice never changes permissions or sanctions. */
public final class StaffMessages {
    private final YamlConfiguration configuration;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    StaffMessages(YamlConfiguration configuration) {
        this.configuration = configuration;
    }

    public static StaffMessages load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "staff-messages.yml");
        YamlConfiguration configuration = new YamlConfiguration();
        if (file.exists()) {
            try {
                configuration.load(file);
            } catch (IOException | InvalidConfigurationException exception) {
                throw new IllegalStateException("Revisa staff-messages.yml; no se ha sobrescrito el archivo", exception);
            }
        }
        try (var stream = plugin.getResource("staff-messages.yml")) {
            if (stream == null) {
                throw new IllegalStateException("Falta staff-messages.yml dentro de CSDMAdmin");
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                boolean changed = copyMissingValues(configuration, defaults);
                if (changed) {
                    try {
                        configuration.save(file);
                    } catch (IOException exception) {
                        plugin.getLogger().warning("No se pudo guardar staff-messages.yml: " + exception.getMessage());
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudieron cargar los mensajes de Staff Mode", exception);
        }
        return new StaffMessages(configuration);
    }

    static boolean copyMissingValues(YamlConfiguration configuration, YamlConfiguration defaults) {
        boolean changed = false;
        for (String path : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(path) && !configuration.contains(path)) {
                configuration.set(path, defaults.get(path));
                changed = true;
            }
        }
        return changed;
    }

    public void send(Audience audience, String notice, String playerName) {
        var player = Placeholder.unparsed("player", playerName);
        for (String line : configuration.getStringList(notice + ".chat")) {
            audience.sendMessage(miniMessage.deserialize(line, player));
        }
        if (configuration.getBoolean(notice + ".title.enabled", true)) {
            audience.showTitle(Title.title(
                    miniMessage.deserialize(configuration.getString(notice + ".title.text", ""), player),
                    miniMessage.deserialize(configuration.getString(notice + ".title.subtitle", ""), player),
                    Title.Times.times(
                            duration("fade-in-ms", 200),
                            duration("stay-ms", 2500),
                            duration("fade-out-ms", 400))));
        }
    }

    private Duration duration(String key, long fallback) {
        // Invalid/extreme durations must not break a moderation action.
        long milliseconds = configuration.getLong("timing." + key, fallback);
        return Duration.ofMillis(Math.max(0, Math.min(60_000, milliseconds)));
    }
}
