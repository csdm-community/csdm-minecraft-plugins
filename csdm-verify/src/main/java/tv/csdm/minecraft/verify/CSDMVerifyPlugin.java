package tv.csdm.minecraft.verify;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.verify.backend.HttpBackendClient;
import tv.csdm.minecraft.verify.command.VerifyCommand;
import tv.csdm.minecraft.verify.config.Messages;
import tv.csdm.minecraft.verify.config.VerifySettings;
import tv.csdm.minecraft.verify.listener.JoinListener;
import tv.csdm.minecraft.verify.listener.PlayerProtectionListener;
import tv.csdm.minecraft.verify.service.VerificationCoordinator;
import tv.csdm.minecraft.verify.version.ViaVersionClientVersionResolver;
import tv.csdm.minecraft.verify.world.WorldRoutingService;

public final class CSDMVerifyPlugin extends JavaPlugin {
    private VerifySettings settings;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        try {
            settings = VerifySettings.load(this);
            messages = Messages.load(this);
        } catch (IllegalArgumentException exception) {
            getLogger().severe("Configuracion invalida: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!getServer().getOnlineMode()) {
            getLogger().severe("CSDMVerify requiere online-mode=true. El plugin se deshabilitara.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var versionResolver = new ViaVersionClientVersionResolver(this);
        var backendClient = new HttpBackendClient(settings);
        var coordinator = new VerificationCoordinator(settings, backendClient);
        var routing = new WorldRoutingService(this, settings);
        var verifyCommand = new VerifyCommand(
                this, settings, messages, coordinator, versionResolver, routing);

        PluginCommand command = Objects.requireNonNull(getCommand("verificar"), "Falta /verificar en plugin.yml");
        command.setExecutor(verifyCommand);
        command.setTabCompleter(verifyCommand);

        getServer().getPluginManager().registerEvents(
                new JoinListener(this, settings, messages, backendClient, routing), this);
        getServer().getPluginManager().registerEvents(
                new PlayerProtectionListener(settings.blockChat(), routing), this);

        if (!settings.enabled()) {
            getLogger().warning("CSDMVerify esta instalado pero disabled: enabled=false.");
        } else {
            getLogger().info("CSDMVerify activo con rutas verify_void -> lobby.");
        }
    }

    public VerifySettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }
}
