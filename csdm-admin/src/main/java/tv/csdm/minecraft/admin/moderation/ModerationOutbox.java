package tv.csdm.minecraft.admin.moderation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/** Delivery payloads are immutable, even if the local sanction is pardoned later. */
final class ModerationOutbox {
    private final Path directory;
    ModerationOutbox(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }
    synchronized void put(UUID id, String payload) throws IOException {
        Path destination = directory.resolve(id + ".json");
        if (Files.exists(destination)) {
            if (!Files.readString(destination).equals(payload)) throw new IOException("Conflicto de ID de sanción");
            return;
        }
        Path temporary = Files.createTempFile(directory, id.toString(), ".tmp");
        try {
            Files.writeString(temporary, payload, StandardCharsets.UTF_8);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }
    synchronized List<UUID> pending() throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json"))
                    .sorted().map(n -> UUID.fromString(n.substring(0, 36))).toList();
        }
    }
    synchronized String read(UUID id) throws IOException { return Files.readString(directory.resolve(id + ".json")); }
    synchronized void delivered(UUID id) throws IOException { Files.deleteIfExists(directory.resolve(id + ".json")); }
}
