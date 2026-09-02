package tv.csdm.minecraft.admin.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationParserTest {
    @Test
    void parsesMinutesHoursAndDays() {
        assertEquals(Duration.ofMinutes(30), DurationParser.parse("30m"));
        assertEquals(Duration.ofHours(12), DurationParser.parse("12H"));
        assertEquals(Duration.ofDays(7), DurationParser.parse("7d"));
    }

    @Test
    void rejectsInvalidAndZeroDurations() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0m"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("forever"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10s"));
    }
}
