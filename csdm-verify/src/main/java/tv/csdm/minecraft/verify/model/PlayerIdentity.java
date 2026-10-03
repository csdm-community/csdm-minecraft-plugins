package tv.csdm.minecraft.verify.model;

import java.util.UUID;

/** Authenticated identity, obtained from Paper or Floodgate, never from a nickname prefix. */
public record PlayerIdentity(UUID uuid, String username, String edition, String xuid, String version) {
    public PlayerIdentity {
        if (uuid == null || username == null || username.isBlank()) throw new IllegalArgumentException("Identidad incompleta");
        if ("bedrock".equals(edition)) {
            if (xuid == null || !xuid.matches("[1-9][0-9]{0,19}")
                    || !uuid.equals(new UUID(0, Long.parseUnsignedLong(xuid))))
                throw new IllegalArgumentException("XUID y UUID de Floodgate no coinciden");
            if (username.length() > 32 || username.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Gamertag inválido");
        } else if (!"java".equals(edition) || xuid != null || uuid.getMostSignificantBits() == 0
                || !username.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Identidad Java inválida");
        }
    }

    public static PlayerIdentity javaPlayer(UUID uuid, String name) {
        return new PlayerIdentity(uuid, name, "java", null, null);
    }
    public boolean bedrock() { return "bedrock".equals(edition); }
}
