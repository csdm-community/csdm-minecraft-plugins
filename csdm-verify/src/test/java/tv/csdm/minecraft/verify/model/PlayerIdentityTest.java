package tv.csdm.minecraft.verify.model;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerIdentityTest {
    private final UUID bedrock = UUID.fromString("00000000-0000-0000-ffff-ffffffffffff");

    @Test void supportsUnsignedXboxIdsAndSpacedGamertags() {
        var identity = new PlayerIdentity(bedrock, "Xbox Player", "bedrock", "18446744073709551615", "26.30");
        assertTrue(identity.bedrock());
        assertEquals("Xbox Player", identity.username());
    }

    @Test void rejectsMismatchedOrMalformedXboxIds() {
        for (String xuid : new String[]{"1", "0", "01", "-1", "18446744073709551616", "1e3"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlayerIdentity(bedrock, "Xbox Player", "bedrock", xuid, "26.30"));
        }
    }

    @Test void cannotTreatFloodgateUuidOrPrefixAsJavaAuthentication() {
        assertThrows(IllegalArgumentException.class, () -> PlayerIdentity.javaPlayer(bedrock, "Player"));
        assertThrows(IllegalArgumentException.class, () -> PlayerIdentity.javaPlayer(UUID.randomUUID(), ".Xbox_Player"));
        assertFalse(PlayerIdentity.javaPlayer(UUID.randomUUID(), "Java_Player").bedrock());
    }
}
