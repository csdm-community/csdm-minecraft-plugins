package tv.csdm.minecraft.admin.service;

import org.bukkit.configuration.file.FileConfiguration;

public record AdminSettings(
        String worldName,
        boolean permanentNight,
        long fixedTime,
        boolean lockWeather,
        boolean peaceful,
        boolean disableMobSpawning,
        boolean disableFireTick,
        boolean pvp,
        int enforceEverySeconds,
        boolean forceAdventure,
        boolean allowFlight,
        boolean teleportToSpawnOnJoin,
        int rescueBelowY,
        boolean staffBypassKeepsGamemode,
        boolean protectionEnabled,
        boolean blockBreaking,
        boolean blockPlacing,
        boolean blockInteractions,
        boolean blockItemDrops,
        boolean maintenanceEnabled,
        String maintenanceKickMessage) {

    public static AdminSettings load(FileConfiguration config) {
        String worldName = config.getString("world.name", "lobby");
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("world.name no puede estar vacio");
        }
        int interval = config.getInt("world.enforce-every-seconds", 60);
        if (interval < 10) {
            throw new IllegalArgumentException("world.enforce-every-seconds debe ser al menos 10");
        }
        return new AdminSettings(
                worldName,
                config.getBoolean("world.permanent-night", true),
                config.getLong("world.fixed-time", 18000L),
                config.getBoolean("world.lock-weather", true),
                config.getBoolean("world.peaceful", true),
                config.getBoolean("world.disable-mob-spawning", true),
                config.getBoolean("world.disable-fire-tick", true),
                config.getBoolean("world.pvp", false),
                interval,
                config.getBoolean("players.force-adventure", true),
                config.getBoolean("players.allow-flight", true),
                config.getBoolean("players.teleport-to-spawn-on-join", true),
                config.getInt("players.rescue-below-y", -20),
                config.getBoolean("players.staff-bypass-keeps-gamemode", true),
                config.getBoolean("protection.enabled", true),
                config.getBoolean("protection.block-breaking", true),
                config.getBoolean("protection.block-placing", true),
                config.getBoolean("protection.block-interactions", true),
                config.getBoolean("protection.block-item-drops", true),
                config.getBoolean("maintenance.enabled", false),
                config.getString("maintenance.kick-message", "Servidor en mantenimiento."));
    }
}

