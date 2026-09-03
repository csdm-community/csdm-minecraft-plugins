package tv.csdm.minecraft.verify.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MessagesTest {
    @Test
    void copiesMissingMessagesWithoutOverwritingCustomValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("museum-title", "Titulo personalizado");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("museum-title", "Titulo incluido");
        defaults.set("museum-subtitle", "Subtitulo incluido");
        defaults.set("museum-welcome", "Bienvenida incluida");

        assertTrue(Messages.copyMissingValues(configuration, defaults));
        assertEquals("Titulo personalizado", configuration.getString("museum-title"));
        assertEquals("Subtitulo incluido", configuration.getString("museum-subtitle"));
        assertEquals("Bienvenida incluida", configuration.getString("museum-welcome"));
        assertFalse(Messages.copyMissingValues(configuration, defaults));
    }
}
