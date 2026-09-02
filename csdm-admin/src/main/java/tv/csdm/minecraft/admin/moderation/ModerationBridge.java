package tv.csdm.minecraft.admin.moderation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModerationBridge {
    private final JavaPlugin plugin;
    private final ModerationSettings settings;
    private final HttpClient httpClient;

    public ModerationBridge(JavaPlugin plugin, ModerationSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(settings.requestTimeoutSeconds()))
                .build();
        if (!settings.enabled()) {
            plugin.getLogger().warning(
                    "Puente de moderación deshabilitado: configura moderation.backend-url y la variable secreta.");
        }
    }

    public void publish(Sanction sanction) {
        if (!settings.enabled()) {
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.backendUrl()))
                .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()))
                .header("Authorization", "Bearer " + settings.internalSecret())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(sanction)))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("No se pudo sincronizar la sanción " + sanction.id()
                                + ": " + throwable.getClass().getSimpleName());
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("El backend rechazó la sanción " + sanction.id()
                                + " con HTTP " + response.statusCode());
                    }
                });
    }

    private String payload(Sanction sanction) {
        return "{" +
                "\"id\":\"" + sanction.id() + "\"," +
                "\"targetUuid\":\"" + sanction.targetUuid() + "\"," +
                "\"targetName\":\"" + escape(sanction.targetName()) + "\"," +
                "\"actorUuid\":\"" + sanction.actorUuid() + "\"," +
                "\"actorName\":\"" + escape(sanction.actorName()) + "\"," +
                "\"action\":\"" + sanction.type().name().toLowerCase() + "\"," +
                "\"reason\":\"" + escape(sanction.reason()) + "\"," +
                "\"createdAt\":\"" + sanction.createdAt() + "\"," +
                "\"expiresAt\":" + nullable(sanction.expiresAt() == null ? null : sanction.expiresAt().toString()) + "," +
                "\"active\":" + sanction.active() + "," +
                "\"publicAnnouncement\":" + sanction.publicAnnouncement() +
                "}";
    }

    private String nullable(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
