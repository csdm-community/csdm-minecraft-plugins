package tv.csdm.minecraft.admin.moderation;

import java.time.Instant;
import java.util.UUID;

public record Sanction(
        UUID id,
        UUID targetUuid,
        String targetName,
        UUID actorUuid,
        String actorName,
        SanctionType type,
        String reason,
        Instant createdAt,
        Instant expiresAt,
        boolean active,
        boolean publicAnnouncement) {

    public boolean restriction() {
        return type == SanctionType.TEMPBAN || type == SanctionType.BAN;
    }

    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
