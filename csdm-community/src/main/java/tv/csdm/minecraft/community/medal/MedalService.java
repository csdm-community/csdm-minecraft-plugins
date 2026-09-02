package tv.csdm.minecraft.community.medal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MedalService {
    private volatile MedalRegistry registry;
    private final MedalRepository repository;

    public MedalService(MedalRegistry registry, MedalRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    public void replaceRegistry(MedalRegistry registry) {
        this.registry = registry;
    }

    public Optional<MedalDefinition> definition(String id) {
        return registry.find(id);
    }

    public List<MedalDefinition> definitions() {
        return List.copyOf(registry.all());
    }

    public List<MedalDefinition> unlocked(UUID playerUuid) {
        return repository.find(playerUuid).unlocked().stream()
                .map(registry::find)
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<MedalDefinition> featured(UUID playerUuid) {
        String featured = repository.find(playerUuid).featured();
        return featured == null ? Optional.empty() : registry.find(featured);
    }

    public boolean grant(UUID playerUuid, String id) {
        MedalDefinition definition = registry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Medalla desconocida: " + id));
        return repository.grant(playerUuid, definition.id());
    }

    public boolean revoke(UUID playerUuid, String id) {
        MedalDefinition definition = registry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Medalla desconocida: " + id));
        return repository.revoke(playerUuid, definition.id());
    }

    public boolean feature(UUID playerUuid, String id) {
        MedalDefinition definition = registry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Medalla desconocida: " + id));
        return repository.feature(playerUuid, definition.id());
    }
}

