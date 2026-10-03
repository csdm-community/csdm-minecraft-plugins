package tv.csdm.minecraft.admin.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class StaffMessagesTest {
    @Test
    void fillsOldConfigurationWithoutOverwritingCustomTextOrDisabledNotices() throws Exception {
        var configuration = new YamlConfiguration();
        configuration.set("enabled.title.text", "Mi título");
        configuration.set("enabled.title.enabled", false);
        configuration.set("frozen.chat", List.of());
        assertTrue(StaffMessages.copyMissingValues(configuration, defaults()));
        assertFalse(StaffMessages.copyMissingValues(configuration, defaults()));
        var reloaded = new YamlConfiguration();
        reloaded.loadFromString(configuration.saveToString());
        assertEquals("Mi título", reloaded.getString("enabled.title.text"));
        assertFalse(reloaded.getBoolean("enabled.title.enabled"));
        assertTrue(reloaded.getStringList("frozen.chat").isEmpty());
        assertFalse(reloaded.getString("unfrozen.title.text").isBlank());
        var audience = new RecordingAudience();
        new StaffMessages(reloaded).send(audience, "enabled", "7245");
        assertEquals(1, audience.chat.size());
        assertTrue(audience.titles.isEmpty());
    }

    @Test
    void deliversFreezeNoticeWithLiteralPlayerNameAndTitle() throws Exception {
        var audience = new RecordingAudience();
        new StaffMessages(defaults()).send(audience, "frozen", "<red>Nombre</red>");
        assertEquals(6, audience.chat.size());
        assertTrue(plain(audience.chat.get(2)).contains("<red>Nombre</red>"));
        assertEquals(1, audience.titles.size());
        assertEquals("EN REVISIÓN", plain(audience.titles.getFirst().title()));
        assertEquals(Duration.ofMillis(2500), audience.titles.getFirst().times().stay());
    }

    @Test
    void permitsSilentChatAndClampsInvalidDurations() throws Exception {
        var configuration = defaults();
        configuration.set("unfrozen.chat", List.of());
        configuration.set("timing.fade-in-ms", -10);
        configuration.set("timing.stay-ms", Long.MAX_VALUE);
        var audience = new RecordingAudience();
        new StaffMessages(configuration).send(audience, "unfrozen", "7245");
        assertTrue(audience.chat.isEmpty());
        assertEquals(Duration.ZERO, audience.titles.getFirst().times().fadeIn());
        assertEquals(Duration.ofSeconds(60), audience.titles.getFirst().times().stay());
    }

    @Test
    void everyBundledNoticeRendersAndStaffConfirmationDoesNotReplaceTitle() throws Exception {
        var messages = new StaffMessages(defaults());
        for (String key : List.of("enabled", "disabled", "vanished", "visible", "frozen", "unfrozen")) {
            var audience = new RecordingAudience();
            messages.send(audience, key, "7245");
            assertFalse(audience.chat.isEmpty(), key);
            assertEquals(1, audience.titles.size(), key);
            assertFalse(plain(audience.titles.getFirst().title()).isBlank(), key);
        }
        var staff = new RecordingAudience();
        messages.send(staff, "freeze-confirmed", "Jugador");
        assertEquals("Jugador quedó congelado.", plain(staff.chat.getFirst()));
        assertTrue(staff.titles.isEmpty());
    }

    private static YamlConfiguration defaults() throws Exception {
        try (var stream = StaffMessagesTest.class.getResourceAsStream("/staff-messages.yml")) {
            assertNotNull(stream);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static final class RecordingAudience implements Audience {
        private final List<Component> chat = new ArrayList<>();
        private final List<Title> titles = new ArrayList<>();

        @Override
        public void sendMessage(Component message) {
            chat.add(message);
        }

        @Override
        public void showTitle(Title title) {
            titles.add(title);
        }
    }
}
