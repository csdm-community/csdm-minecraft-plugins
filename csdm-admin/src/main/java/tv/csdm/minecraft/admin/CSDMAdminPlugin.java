package tv.csdm.minecraft.admin;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.admin.command.AdminCommand;
import tv.csdm.minecraft.admin.listener.AdminPlayerListener;
import tv.csdm.minecraft.admin.listener.LobbyProtectionListener;
import tv.csdm.minecraft.admin.listener.ServerListListener;
import tv.csdm.minecraft.admin.listener.WorldPolicyListener;
import tv.csdm.minecraft.admin.service.AdminSettings;
import tv.csdm.minecraft.admin.service.WorldPolicyService;

public final class CSDMAdminPlugin extends JavaPlugin {
    private AdminSettings settings;
    private WorldPolicyService worldPolicyService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadServices();

        AdminCommand adminCommand = new AdminCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("csdmadmin"));
        command.setExecutor(adminCommand);
        command.setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(new WorldPolicyListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(this), this);

        worldPolicyService.applyConfiguredWorld();
        worldPolicyService.startEnforcementTask();
        getLogger().info("CSDMAdmin activo para el mundo " + settings.worldName() + ".");
    }

    @Override
    public void onDisable() {
        if (worldPolicyService != null) {
            worldPolicyService.stopEnforcementTask();
        }
    }

    public void reloadServices() {
        reloadConfig();
        settings = AdminSettings.load(getConfig());
        if (worldPolicyService != null) {
            worldPolicyService.stopEnforcementTask();
        }
        worldPolicyService = new WorldPolicyService(this, settings);
    }

    public AdminSettings settings() {
        return settings;
    }

    public WorldPolicyService worldPolicyService() {
        return worldPolicyService;
    }

    public void setPermanentNight(boolean enabled) {
        getConfig().set("world.permanent-night", enabled);
        saveConfig();
        reloadServices();
        worldPolicyService.applyConfiguredWorld();
        worldPolicyService.startEnforcementTask();
    }

    public void setMaintenance(boolean enabled) {
        getConfig().set("maintenance.enabled", enabled);
        saveConfig();
        reloadServices();
        worldPolicyService.applyConfiguredWorld();
        worldPolicyService.startEnforcementTask();
    }
}
