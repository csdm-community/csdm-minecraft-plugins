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
        Instant serverTimestamp) {}

