package tv.csdm.minecraft.verify.model;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

public record VerificationRequest(
        String code,
        UUID minecraftUuid,
        String minecraftUsername,
        InetAddress address,
        Integer clientProtocol,
        String clientVersion,
        Instant serverTimestamp,
        String edition,
        String bedrockXuid) {
    public VerificationRequest(String code, UUID uuid, String name, InetAddress address,
            Integer protocol, String version, Instant timestamp) {
        this(code, uuid, name, address, protocol, version, timestamp, "java", null);
    }
}

