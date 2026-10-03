package tv.csdm.minecraft.admin.moderation;
import java.nio.file.Path;
import java.util.UUID;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModerationOutboxTest {
    @TempDir Path directory;
    @Test void pendingDeliverySurvivesRestartAndRejectsPayloadReplacement() throws Exception {
        var id = UUID.randomUUID();
        new ModerationOutbox(directory).put(id, "{\"reason\":\"original\"}");
        var restarted = new ModerationOutbox(directory);
        assertEquals(1, restarted.pending().size());
        assertThrows(IOException.class, () -> restarted.put(id, "different"));
        assertEquals("{\"reason\":\"original\"}", restarted.read(id));
        restarted.delivered(id);
        assertTrue(new ModerationOutbox(directory).pending().isEmpty());
    }
    @Test void pendingOrIncompleteResponsesNeverClearOutbox() {
        assertFalse(ModerationBridge.deliveryConfirmed(202, "{\"discordDelivered\":false}"));
        assertFalse(ModerationBridge.deliveryConfirmed(200, "{\"ok\":true}"));
        assertFalse(ModerationBridge.deliveryConfirmed(500, "{\"discordDelivered\":true}"));
        assertTrue(ModerationBridge.deliveryConfirmed(201, "{\"discordDelivered\": true}"));
    }
}
