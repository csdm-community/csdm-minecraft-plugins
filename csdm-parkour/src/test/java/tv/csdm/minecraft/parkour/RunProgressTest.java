package tv.csdm.minecraft.parkour;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunProgressTest {
    @Test void cannotSkipOrReplayCheckpoints() {
        var run = new RunProgress(3, 0);
        assertFalse(run.touch(2));
        assertFalse(run.canFinish());
        assertTrue(run.touch(1));
        assertFalse(run.touch(1));
        assertFalse(run.touch(3));
        assertTrue(run.touch(2));
        assertTrue(run.touch(3));
        assertTrue(run.canFinish());
        assertFalse(run.touch(4));
    }
    @Test void restartClearsProgressAndTimeTogether() {
        var run = new RunProgress(2, 1_000_000_000L);
        run.touch(1);
        assertEquals(250, run.elapsedMillis(1_250_000_000L));
        run.restart(2_000_000_000L);
        assertEquals(0, run.checkpoint());
        assertFalse(run.canFinish());
        assertEquals(500, run.elapsedMillis(2_500_000_000L));
    }
    @Test void supportsCourseWithoutCheckpointsAndLongSessions() {
        assertTrue(new RunProgress(0, 0).canFinish());
        assertEquals("61:01.007", RunProgress.format(3_661_007));
    }
    @Test void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new RunProgress(-1, 0));
    }
}
