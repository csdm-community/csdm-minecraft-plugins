package tv.csdm.minecraft.community.medal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MedalRegistryTest {
    @Test
    void normalizesIdsDeterministically() {
        assertEquals("maestro-umbral", MedalRegistry.normalizeId("Maestro_Umbral"));
    }
}

