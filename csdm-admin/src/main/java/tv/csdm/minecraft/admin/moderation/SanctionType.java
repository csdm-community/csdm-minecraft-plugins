package tv.csdm.minecraft.admin.moderation;

public enum SanctionType {
    WARNING("advertencia"),
    KICK("expulsión"),
    TEMPBAN("suspensión"),
    BAN("bloqueo"),
    PARDON("perdón");

    private final String displayName;

    SanctionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
