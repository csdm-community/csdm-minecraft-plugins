package tv.csdm.minecraft.admin;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.admin.command.AdminCommand;
import tv.csdm.minecraft.admin.command.FlyCommand;
import tv.csdm.minecraft.admin.command.SanctionCommand;
import tv.csdm.minecraft.admin.command.StaffModeCommand;
import tv.csdm.minecraft.admin.listener.AdminPlayerListener;
import tv.csdm.minecraft.admin.listener.LobbyProtectionListener;
import tv.csdm.minecraft.admin.listener.SanctionListener;
import tv.csdm.minecraft.admin.listener.ServerListListener;
import tv.csdm.minecraft.admin.listener.StaffModeListener;
import tv.csdm.minecraft.admin.listener.WorldPolicyListener;
import tv.csdm.minecraft.admin.moderation.ModerationBridge;
import tv.csdm.minecraft.admin.moderation.ModerationSettings;
import tv.csdm.minecraft.admin.moderation.SanctionRepository;
import tv.csdm.minecraft.admin.moderation.SanctionService;
import tv.csdm.minecraft.admin.service.AdminSettings;
import tv.csdm.minecraft.admin.service.WorldPolicyService;
import tv.csdm.minecraft.admin.staff.StaffModeService;

public final class CSDMAdminPlugin extends JavaPlugin {
    private AdminSettings settings;
    private WorldPolicyService worldPolicyService;
    private StaffModeService staffModeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadServices();

        AdminCommand adminCommand = new AdminCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("csdmadmin"));
        command.setExecutor(adminCommand);
        command.setTabCompleter(adminCommand);

        staffModeService = new StaffModeService(this);
        StaffModeCommand staffModeCommand = new StaffModeCommand(staffModeService);
        PluginCommand staffCommand = Objects.requireNonNull(getCommand("staff"));
        staffCommand.setExecutor(staffModeCommand);
        staffCommand.setTabCompleter(staffModeCommand);

        FlyCommand flyCommand = new FlyCommand();
        PluginCommand fly = Objects.requireNonNull(getCommand("fly"));
        fly.setExecutor(flyCommand);
        fly.setTabCompleter(flyCommand);

        ModerationSettings moderationSettings = ModerationSettings.load(getConfig());
        SanctionRepository sanctionRepository = new SanctionRepository(this);
        SanctionService sanctions = new SanctionService(
                sanctionRepository,
                new ModerationBridge(this, moderationSettings));
        SanctionCommand sanctionCommand = new SanctionCommand(sanctions);
        PluginCommand sanction = Objects.requireNonNull(getCommand("sancionar"));
        sanction.setExecutor(sanctionCommand);
        sanction.setTabCompleter(sanctionCommand);

        getServer().getPluginManager().registerEvents(new WorldPolicyListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffModeListener(staffModeService), this);
        getServer().getPluginManager().registerEvents(new SanctionListener(sanctions), this);

        worldPolicyService.applyConfiguredWorld();
        worldPolicyService.startEnforcementTask();
        getLogger().info("CSDMAdmin activo para el mundo " + settings.worldName() + ".");
    }

    @Override
    public void onDisable() {
        if (worldPolicyService != null) {
            worldPolicyService.stopEnforcementTask();
        }
        if (staffModeService != null) {
            staffModeService.restoreAll();
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
