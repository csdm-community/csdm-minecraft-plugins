package tv.csdm.minecraft.parkour;

import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotStoreTest {
    @TempDir Path directory;

    @Test void snapshotSurvivesStoreRecreationAndIsRetiredOnlyAfterSavingPlayer() throws Exception {
        var fixture = new Fixture();
        var store = new SnapshotStore(directory);
        store.capture(fixture.player);
        assertTrue(store.contains(fixture.id));
        fixture.mode = GameMode.ADVENTURE;
        fixture.allowFlight = false;
        fixture.heldSlot = 0;
        new SnapshotStore(directory).restore(fixture.player);
        assertEquals(GameMode.CREATIVE, fixture.mode);
        assertTrue(fixture.allowFlight);
        assertEquals(7, fixture.heldSlot);
        assertTrue(fixture.saved);
        assertFalse(store.contains(fixture.id));
    }

    @Test void duplicateStartCannotOverwriteOriginalSnapshot() throws Exception {
        var fixture = new Fixture(); var store = new SnapshotStore(directory);
        store.capture(fixture.player);
        byte[] original = Files.readAllBytes(directory.resolve(fixture.id + ".yml"));
        fixture.mode = GameMode.ADVENTURE;
        assertThrows(java.io.IOException.class, () -> store.capture(fixture.player));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve(fixture.id + ".yml")));
    }

    @Test void failedPlayerSavePreservesRecoveryFile() throws Exception {
        var fixture = new Fixture(); var store = new SnapshotStore(directory);
        store.capture(fixture.player); fixture.failSave = true;
        assertThrows(IllegalStateException.class, () -> store.restore(fixture.player));
        assertTrue(store.contains(fixture.id));
        fixture.failSave = false; store.restore(fixture.player);
        assertFalse(store.contains(fixture.id));
    }

    @Test void malformedSnapshotDoesNotClearInventory() throws Exception {
        var fixture = new Fixture(); var store = new SnapshotStore(directory);
        Files.writeString(directory.resolve(fixture.id + ".yml"), "schema: [broken");
        assertThrows(Exception.class, () -> store.restore(fixture.player));
        assertFalse(fixture.inventoryWritten);
        assertTrue(store.contains(fixture.id));
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        GameMode mode = GameMode.CREATIVE;
        boolean allowFlight = true;
        int heldSlot = 7;
        boolean saved, failSave, inventoryWritten;
        final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getContents" -> new ItemStack[41];
                case "getHeldItemSlot" -> heldSlot;
                case "setContents" -> { inventoryWritten = true; yield null; }
                case "setHeldItemSlot" -> { heldSlot = (int) args[0]; yield null; }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
        final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getInventory" -> inventory;
                case "getGameMode" -> mode;
                case "setGameMode" -> { mode = (GameMode) args[0]; yield null; }
                case "getAllowFlight" -> allowFlight;
                case "setAllowFlight" -> { allowFlight = (boolean) args[0]; yield null; }
                case "isFlying", "isCollidable" -> true;
                case "closeInventory", "setItemOnCursor", "setFlying", "setCollidable" -> null;
                case "saveData" -> {
                    if (failSave) throw new IllegalStateException("Simulated playerdata failure");
                    saved = true; yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }
}
