package tv.csdm.minecraft.verify.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record VerifySettings(
        boolean enabled,
        URI backendUri,
        String internalSecret,
        Duration requestTimeout,
        Pattern codePattern,
        int maxCodeLength,
        Duration cooldown,
        int attemptsPerWindow,
        Duration rateLimitWindow,
        int minClientProtocol,
        int maxClientProtocol,
        boolean allowUnknownClientVersion,
        int kickAfterSuccessSeconds,
        boolean blockChat) {

    public static VerifySettings load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean enabled = config.getBoolean("enabled", false);
        URI uri = parseUri(required(config, "backend.url"));
        boolean allowInsecure = config.getBoolean("backend.allow-insecure-http", false);
        if (!allowInsecure && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("backend.url debe usar HTTPS");
        }

        String secretEnv = config.getString("backend.secret-env", "CSDM_INTERNAL_SECRET");
        String secret = secretEnv == null || secretEnv.isBlank() ? "" : System.getenv(secretEnv);
        if (secret == null || secret.isBlank()) {
            secret = config.getString("backend.secret", "");
        }
        secret = Objects.requireNonNullElse(secret, "").trim();
        if (enabled && secret.length() < 32) {
            throw new IllegalArgumentException(
                    "el secreto interno debe existir y tener al menos 32 caracteres cuando enabled=true");
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(required(config, "codes.regex"), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("codes.regex no es valido", exception);
        }

        int minProtocol = positive(config, "client-versions.min-protocol", 767);
        int maxProtocol = positive(config, "client-versions.max-protocol", 776);
        if (maxProtocol < minProtocol) {
            throw new IllegalArgumentException("max-protocol no puede ser menor que min-protocol");
        }

        return new VerifySettings(
                enabled,
                uri,
                secret,
                Duration.ofSeconds(positive(config, "backend.timeout-seconds", 5)),
                pattern,
                positive(config, "codes.max-length", 16),
                Duration.ofSeconds(positive(config, "rate-limit.cooldown-seconds", 5)),
                positive(config, "rate-limit.attempts", 5),
                Duration.ofSeconds(positive(config, "rate-limit.window-seconds", 60)),
                minProtocol,
                maxProtocol,
                config.getBoolean("client-versions.allow-unknown", true),
                positive(config, "experience.kick-after-success-seconds", 5),
                config.getBoolean("experience.block-chat", true));
    }

    public String normalizeCode(String input) {
        return input.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isValidCode(String input) {
        if (input == null || input.isBlank() || input.length() > maxCodeLength) {
            return false;
        }
        return codePattern.matcher(normalizeCode(input)).matches();
    }

    private static URI parseUri(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("backend.url no contiene un host");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("backend.url no es una URI valida", exception);
        }
    }

    private static String required(FileConfiguration config, String path) {
        String value = config.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("falta " + path);
        }
        return value.trim();
    }

    private static int positive(FileConfiguration config, String path, int fallback) {
        int value = config.getInt(path, fallback);
        if (value <= 0) {
            throw new IllegalArgumentException(path + " debe ser mayor que cero");
        }
        return value;
    }
}

