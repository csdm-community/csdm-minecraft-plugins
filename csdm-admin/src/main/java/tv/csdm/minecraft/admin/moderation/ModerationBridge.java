package tv.csdm.minecraft.admin.moderation;

import java.net.URI;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModerationBridge {
    private final JavaPlugin plugin;
    private final ModerationSettings settings;
    private final HttpClient httpClient;
    private final ModerationOutbox outbox;
    private int retryOffset;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public ModerationBridge(JavaPlugin plugin, ModerationSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        try { outbox = new ModerationOutbox(plugin.getDataFolder().toPath().resolve("moderation-outbox")); }
        catch (IOException e) { throw new IllegalStateException("No se pudo abrir la cola de moderación", e); }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(settings.requestTimeoutSeconds()))
                .build();
        if (!settings.enabled()) {
            plugin.getLogger().warning(
                    "Puente de moderación deshabilitado: configura moderation.backend-url y la variable secreta.");
        }
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::retryPending, 20L, 1200L);
    }

    public void publish(Sanction sanction) {
        try { outbox.put(sanction.id(), payload(sanction)); }
        catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el aviso pendiente de " + sanction.id() + ": " + e.getMessage());
            return;
        }
        send(sanction.id());
    }

    private void retryPending() {
        if (!settings.enabled()) return;
        try {
            var pending = outbox.pending();
            if (pending.isEmpty()) return;
            int count = Math.min(20, pending.size());
            for (int i = 0; i < count; i++) send(pending.get(Math.floorMod(retryOffset + i, pending.size())));
            retryOffset = Math.floorMod(retryOffset + count, pending.size());
        }
        catch (IOException e) { plugin.getLogger().warning("No se pudo leer la cola de sanciones: " + e.getMessage()); }
    }

    private void send(UUID id) {
        if (!settings.enabled() || !inFlight.add(id)) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.backendUrl()))
                    .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()))
                    .header("Authorization", "Bearer " + settings.internalSecret())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(outbox.read(id))).build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, failure) -> {
                        try {
                            if (failure == null && deliveryConfirmed(response.statusCode(), response.body())) outbox.delivered(id);
                            else plugin.getLogger().warning("Aviso de sanción pendiente " + id + "; se reintentará en 60 s.");
                        } catch (IOException e) { plugin.getLogger().warning("No se pudo retirar el aviso entregado " + id); }
                        finally { inFlight.remove(id); }
                    });
        } catch (IOException | RuntimeException e) {
            inFlight.remove(id);
            plugin.getLogger().warning("No se pudo enviar el aviso de sanción " + id + ": " + e.getClass().getSimpleName());
        }
    }

    static boolean deliveryConfirmed(int status, String body) {
        return (status == 200 || status == 201) && body != null
                && body.matches("(?s).*\"discordDelivered\"\\s*:\\s*true\\b.*");
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
