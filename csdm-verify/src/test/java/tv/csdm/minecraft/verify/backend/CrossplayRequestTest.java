package tv.csdm.minecraft.verify.backend;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.verify.model.*;
import static org.junit.jupiter.api.Assertions.*;

class CrossplayRequestTest {
    @Test void bedrockStatusAndVerificationCarryAuthenticatedIdentity() {
        UUID uuid = UUID.fromString("00000000-0000-0000-ffff-ffffffffffff");
        var identity = new PlayerIdentity(uuid, "Xbox Player", "bedrock", "18446744073709551615", "26.30");
        var request = new VerificationRequest("ABCD-EFGH", uuid, identity.username(), InetAddress.getLoopbackAddress(),
                null, identity.version(), Instant.EPOCH, identity.edition(), identity.xuid());
        for (String json : new String[]{JsonCodec.encode(request), JsonCodec.encodeStatus(identity)}) {
            assertTrue(json.contains("\"edition\":\"bedrock\""));
            assertTrue(json.contains("\"bedrockXuid\":\"18446744073709551615\""));
        }
        assertTrue(JsonCodec.encode(request).contains("\"clientProtocol\":null"));
        assertTrue(JsonCodec.encode(request).contains("\"minecraftUsername\":\"Xbox Player\""));
    }
    @Test void legacyJavaRequestsRemainJava() {
        var request = new VerificationRequest("ABCD-EFGH", UUID.randomUUID(), "JavaPlayer", InetAddress.getLoopbackAddress(),
                777, "26.3", Instant.EPOCH);
        assertTrue(JsonCodec.encode(request).contains("\"edition\":\"java\""));
        assertTrue(JsonCodec.encode(request).contains("\"bedrockXuid\":null"));
    }
    @Test void accessFailuresDoNotReportSuccessfulVerification() {
        assertEquals(VerificationResult.ACCOUNT_ALREADY_LINKED,
                BackendResponseMapper.map(409, "{\"code\":\"ACCOUNT_ALREADY_LINKED\"}").result());
        assertEquals(VerificationResult.DISCORD_VERIFICATION_REQUIRED,
                BackendResponseMapper.map(403, "{\"code\":\"DISCORD_VERIFICATION_REQUIRED\"}").result());
        assertEquals(VerificationResult.SERVER_ERROR, BackendResponseMapper.map(403, "{}").result());
    }
}
