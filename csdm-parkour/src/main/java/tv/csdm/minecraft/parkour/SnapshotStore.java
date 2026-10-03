package tv.csdm.minecraft.parkour;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** One atomic, durable snapshot per player. Never overwrite an unresolved snapshot. */
final class SnapshotStore {
    private final Path directory;

    SnapshotStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    private Path path(UUID id) { return directory.resolve(id + ".yml"); }
    boolean contains(UUID id) { return Files.exists(path(id)); }

    void capture(Player player) throws IOException {
        Path target = path(player.getUniqueId());
        if (Files.exists(target)) throw new IOException("Hay una recuperación pendiente");
        var data = new YamlConfiguration();
        data.set("schema", 1);
        data.set("owner", player.getUniqueId().toString());
        data.set("size", 41);
        var inventory = player.getInventory().getContents();
        for (int i = 0; i < inventory.length; i++) data.set("items." + i, inventory[i]);
        data.set("held-slot", player.getInventory().getHeldItemSlot());
        data.set("game-mode", player.getGameMode().name());
        data.set("allow-flight", player.getAllowFlight());
        data.set("flying", player.isFlying());
        data.set("collidable", player.isCollidable());
        Path temporary = Files.createTempFile(directory, "snapshot-", ".tmp");
        try {
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var bytes = ByteBuffer.wrap(data.saveToString().getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    void restore(Player player) throws Exception {
        Path target = path(player.getUniqueId());
        if (!Files.exists(target)) return;
        var data = new YamlConfiguration();
        data.load(target.toFile()); // Unlike loadConfiguration, malformed YAML must not silently become empty.
        if (data.getInt("schema") != 1 || data.getInt("size") != 41
                || !player.getUniqueId().toString().equals(data.getString("owner"))) {
            throw new IOException("Snapshot inválido: " + target.getFileName());
        }
        GameMode mode = GameMode.valueOf(data.getString("game-mode", ""));
        int held = data.getInt("held-slot", -1);
        if (held < 0 || held > 8) throw new IOException("Slot inválido");
        ItemStack[] contents = new ItemStack[41];
        for (int i = 0; i < contents.length; i++) {
            String key = "items." + i;
            if (data.contains(key) && !data.isItemStack(key)) throw new IOException("Objeto inválido: " + i);
            contents[i] = data.getItemStack(key);
        }
        player.closeInventory();
        player.setItemOnCursor(null);
        player.getInventory().setContents(contents);
        player.getInventory().setHeldItemSlot(held);
        player.setGameMode(mode);
        player.setAllowFlight(data.getBoolean("allow-flight"));
        player.setFlying(data.getBoolean("allow-flight") && data.getBoolean("flying"));
        player.setCollidable(data.getBoolean("collidable", true));
        // Persist restored playerdata before retiring the recovery file.
        player.saveData();
        Files.delete(target);
        try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
    }
}
