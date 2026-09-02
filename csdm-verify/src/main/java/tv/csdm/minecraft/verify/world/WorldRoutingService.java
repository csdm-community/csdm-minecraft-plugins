package tv.csdm.minecraft.verify.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import tv.csdm.minecraft.verify.CSDMVerifyPlugin;
import tv.csdm.minecraft.verify.config.VerifySettings;

public final class WorldRoutingService {
    private final CSDMVerifyPlugin plugin;
    private final VerifySettings settings;
    private final World verificationWorld;

    public WorldRoutingService(CSDMVerifyPlugin plugin, VerifySettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.verificationWorld = loadVerificationWorld();
    }

    public boolean isVerificationWorld(World world) {
        return world != null && world.getName().equals(settings.verificationWorldName());
    }

    public void sendToVerification(Player player) {
        player.teleportAsync(verificationWorld.getSpawnLocation());
    }

    public void sendToMuseum(Player player) {
        if (player.getWorld().getName().equals(settings.museumWorldName())) {
            return;
        }
        World museum = Bukkit.getWorld(settings.museumWorldName());
        if (museum == null) {
            plugin.getLogger().warning("No se encontro el mundo museo: " + settings.museumWorldName());
            return;
        }
        player.teleportAsync(museum.getSpawnLocation());
    }

    public org.bukkit.Location verificationSpawn() {
        return verificationWorld.getSpawnLocation();
    }

    private World loadVerificationWorld() {
        World world = Bukkit.getWorld(settings.verificationWorldName());
        if (world == null) {
            WorldCreator creator = new WorldCreator(settings.verificationWorldName());
            creator.environment(World.Environment.NORMAL);
            creator.generator(new VoidChunkGenerator());
            world = creator.createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("No se pudo crear el mundo de verificacion");
        }

        int floorY = settings.verificationSpawnY();
        if (world.getBlockAt(0, floorY, 0).getType().isAir()) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    world.getBlockAt(x, floorY, z).setType(Material.SMOOTH_QUARTZ, false);
                }
            }
        }
        world.setSpawnLocation(0, floorY + 1, 0);
        world.setTime(18000L);
        world.setStorm(false);
        world.setThundering(false);
        return world;
    }
}
