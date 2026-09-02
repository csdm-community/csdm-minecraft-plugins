package tv.csdm.minecraft.admin.service;

import java.util.Optional;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class WorldPolicyService {
    private final CSDMAdminPlugin plugin;
    private final AdminSettings settings;
    private BukkitTask enforcementTask;

    public WorldPolicyService(CSDMAdminPlugin plugin, AdminSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void applyConfiguredWorld() {
        World world = plugin.getServer().getWorld(settings.worldName());
        if (world == null) {
            plugin.getLogger().warning("El mundo configurado aun no esta cargado: " + settings.worldName());
            return;
        }
        apply(world);
    }

    public void apply(World world) {
        if (!world.getName().equals(settings.worldName())) {
            return;
        }
        world.setGameRule(GameRules.ADVANCE_TIME, !settings.permanentNight());
        if (settings.permanentNight()) {
            world.setTime(settings.fixedTime());
        }
        world.setGameRule(GameRules.ADVANCE_WEATHER, !settings.lockWeather());
        if (settings.lockWeather()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setClearWeatherDuration(Integer.MAX_VALUE);
        }
        if (settings.peaceful()) {
            world.setDifficulty(Difficulty.PEACEFUL);
        }
        world.setGameRule(GameRules.SPAWN_MOBS, !settings.disableMobSpawning());
        if (settings.disableFireTick()) {
            world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        }
        world.setGameRule(GameRules.PVP, settings.pvp());
    }

    public void startEnforcementTask() {
        stopEnforcementTask();
        long period = settings.enforceEverySeconds() * 20L;
        enforcementTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::applyConfiguredWorld, period, period);
    }

    public void stopEnforcementTask() {
        if (enforcementTask != null) {
            enforcementTask.cancel();
            enforcementTask = null;
        }
    }

    public Optional<Location> configuredSpawn() {
        if (!plugin.getConfig().getBoolean("spawn.configured", false)) {
            World world = plugin.getServer().getWorld(settings.worldName());
            return world == null ? Optional.empty() : Optional.of(world.getSpawnLocation());
        }
        String worldName = plugin.getConfig().getString("spawn.world", settings.worldName());
        World world = plugin.getServer().getWorld(worldName == null ? settings.worldName() : worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
                world,
                plugin.getConfig().getDouble("spawn.x"),
                plugin.getConfig().getDouble("spawn.y"),
                plugin.getConfig().getDouble("spawn.z"),
                (float) plugin.getConfig().getDouble("spawn.yaw"),
                (float) plugin.getConfig().getDouble("spawn.pitch")));
    }

    public void saveSpawn(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("El spawn necesita un mundo");
        }
        plugin.getConfig().set("spawn.configured", true);
        plugin.getConfig().set("spawn.world", location.getWorld().getName());
        plugin.getConfig().set("spawn.x", location.getX());
        plugin.getConfig().set("spawn.y", location.getY());
        plugin.getConfig().set("spawn.z", location.getZ());
        plugin.getConfig().set("spawn.yaw", location.getYaw());
        plugin.getConfig().set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
        location.getWorld().setSpawnLocation(location);
    }
}
