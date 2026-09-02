package tv.csdm.minecraft.admin.moderation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class SanctionService {
    private final SanctionRepository repository;
    private final ModerationBridge bridge;

    public SanctionService(SanctionRepository repository, ModerationBridge bridge) {
        this.repository = repository;
        this.bridge = bridge;
    }

    public Sanction apply(Player actor, Player target, SanctionType type, String reason, Duration duration) {
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = duration == null ? null : createdAt.plus(duration);
        boolean publicAnnouncement = type == SanctionType.BAN
                || (type == SanctionType.TEMPBAN && duration != null && duration.compareTo(Duration.ofDays(1)) >= 0);
        Sanction sanction = new Sanction(
                UUID.randomUUID(),
                target.getUniqueId(),
                target.getName(),
                actor.getUniqueId(),
                actor.getName(),
                type,
                reason,
                createdAt,
                expiresAt,
                type == SanctionType.TEMPBAN || type == SanctionType.BAN,
                publicAnnouncement);
        repository.save(sanction);
        enforce(target, sanction);
        bridge.publish(sanction);
        return sanction;
    }

    public Sanction pardon(Player actor, UUID targetUuid, String targetName, String reason) {
        repository.pardon(targetUuid);
        Sanction sanction = new Sanction(
                UUID.randomUUID(),
                targetUuid,
                targetName,
                actor.getUniqueId(),
                actor.getName(),
                SanctionType.PARDON,
                reason,
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                null,
                false,
                false);
        repository.save(sanction);
        bridge.publish(sanction);
        return sanction;
    }

    public SanctionRepository.KnownTarget knownTarget(String playerName) {
        return repository.findTarget(playerName);
    }

    public Sanction activeRestriction(UUID playerId) {
        return repository.activeRestriction(playerId, Instant.now());
    }

    public Component restrictionMessage(Sanction sanction) {
        Component heading = Component.text("ARCHIVO CSDM\n", NamedTextColor.AQUA);
        Component reason = Component.text("Acceso suspendido: " + sanction.reason(), NamedTextColor.RED);
        if (sanction.expiresAt() == null) {
            return heading.append(reason).append(Component.text("\nBloqueo permanente.", NamedTextColor.GRAY));
        }
        return heading.append(reason).append(Component.text(
                "\nFinaliza: " + sanction.expiresAt() + " UTC", NamedTextColor.GRAY));
    }

    private void enforce(Player target, Sanction sanction) {
        switch (sanction.type()) {
            case WARNING -> target.sendMessage(Component.text(
                    "Advertencia del equipo CSDM: " + sanction.reason(), NamedTextColor.YELLOW));
            case KICK -> target.kick(Component.text(
                    "Has sido expulsado del Archivo: " + sanction.reason(), NamedTextColor.RED));
            case TEMPBAN, BAN -> target.kick(restrictionMessage(sanction));
            case PARDON -> {
            }
        }
    }
}
