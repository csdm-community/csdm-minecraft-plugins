package tv.csdm.minecraft.admin.moderation;

import org.bukkit.configuration.file.FileConfiguration;

public record ModerationSettings(String backendUrl, String internalSecret, int requestTimeoutSeconds) {
    public static ModerationSettings load(FileConfiguration config) {
        String backendUrl = value(config, "moderation.backend-url", "").trim();
        String secretEnv = value(
                config, "moderation.internal-secret-env", "CSDM_MINECRAFT_ADMIN_SECRET").trim();
        String secret = secretEnv.isEmpty() ? "" : System.getenv().getOrDefault(secretEnv, "");
        int timeout = Math.max(2, Math.min(15, config.getInt("moderation.request-timeout-seconds", 5)));
        return new ModerationSettings(backendUrl, secret, timeout);
    }

    public boolean enabled() {
        return !backendUrl.isBlank() && !internalSecret.isBlank();
    }

    private static String value(FileConfiguration config, String path, String fallback) {
        String configured = config.getString(path);
        return configured == null ? fallback : configured;
    }
}
