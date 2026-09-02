package tv.csdm.minecraft.verify.model;

public record ClientVersion(Integer protocol, String displayName) {
    public static ClientVersion unknown() {
        return new ClientVersion(null, null);
    }

    public boolean known() {
        return protocol != null;
    }
}

