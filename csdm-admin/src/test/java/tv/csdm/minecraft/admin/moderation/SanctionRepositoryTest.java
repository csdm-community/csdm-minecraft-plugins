package tv.csdm.minecraft.admin.moderation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SanctionRepositoryTest {
    @TempDir Path directory;

    @Test
    void historySurvivesReloadAndPardonWithoutRemovingEvidence() throws Exception {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        SanctionRepository repository = new SanctionRepository(plugin);
        UUID target = UUID.randomUUID();
        Instant created = Instant.parse("2026-09-20T12:00:00Z");
        Sanction ban = new Sanction(UUID.randomUUID(), target, "Desconectado", UUID.randomUUID(), "Staff",
                SanctionType.BAN, "Prueba", created, null, true, true);
        repository.save(ban);
        assertEquals(ban, repository.history().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> repository.history().clear());
        repository.pardon(target);
        Sanction pardon = new Sanction(UUID.randomUUID(), target, "NombreNuevo", ban.actorUuid(), "Staff",
                SanctionType.PARDON, "Revisión", created.plusSeconds(1), null, false, false);
        repository.save(pardon);
        SanctionRepository reloaded = new SanctionRepository(plugin);
        assertEquals(2, reloaded.history().size());
        assertEquals(pardon, reloaded.history().getFirst());
        assertFalse(reloaded.history().get(1).active());
        assertNull(reloaded.activeRestriction(target, created.plusSeconds(2)));
        assertEquals(target, reloaded.findTarget("desconectado").uuid());
        // One malformed timestamp must not prevent opening the rest of the history.
        var file = directory.resolve("sanctions.yml").toFile();
        var yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("sanctions." + ban.id() + ".created-at", "invalid date");
        yaml.save(file);
        assertEquals(java.util.List.of(pardon), new SanctionRepository(plugin).history());
    }
}
