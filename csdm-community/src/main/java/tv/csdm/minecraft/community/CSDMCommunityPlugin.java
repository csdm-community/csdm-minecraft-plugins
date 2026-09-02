package tv.csdm.minecraft.community;

import java.util.Objects;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.community.command.MedalCommand;
import tv.csdm.minecraft.community.command.StaffCommand;
import tv.csdm.minecraft.community.medal.MedalRegistry;
import tv.csdm.minecraft.community.medal.MedalService;
import tv.csdm.minecraft.community.medal.YamlMedalRepository;
import tv.csdm.minecraft.community.presence.CommunityPresenceListener;
import tv.csdm.minecraft.community.staff.StaffDisplayListener;
import tv.csdm.minecraft.community.staff.StaffRankRegistry;
import tv.csdm.minecraft.community.staff.StaffRankService;

public final class CSDMCommunityPlugin extends JavaPlugin {
    private MedalRegistry medalRegistry;
    private StaffRankRegistry staffRankRegistry;
    private MedalService medalService;
    private StaffRankService staffRankService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("medals.yml", false);

        LuckPerms luckPerms;
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException exception) {
            getLogger().severe("LuckPerms no esta disponible. CSDMCommunity se deshabilitara.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        medalRegistry = MedalRegistry.load(this);
        staffRankRegistry = StaffRankRegistry.load(getConfig());
        medalService = new MedalService(medalRegistry, new YamlMedalRepository(this));
        staffRankService = new StaffRankService(this, luckPerms, staffRankRegistry);
        staffRankService.ensureManagedGroups();

        MedalCommand medalCommand = new MedalCommand(this, medalService);
        PluginCommand medal = Objects.requireNonNull(getCommand("medalla"));
        medal.setExecutor(medalCommand);
        medal.setTabCompleter(medalCommand);

        StaffCommand staffCommand = new StaffCommand(this, staffRankService);
        PluginCommand ranks = Objects.requireNonNull(getCommand("rangos"));
        ranks.setExecutor(staffCommand);
        ranks.setTabCompleter(staffCommand);

        getServer().getPluginManager().registerEvents(new StaffDisplayListener(this, staffRankService), this);
        getServer().getPluginManager().registerEvents(
                new CommunityPresenceListener(this, staffRankService, medalService), this);
        getLogger().info("CSDMCommunity activo con " + medalRegistry.all().size()
                + " medallas y " + staffRankRegistry.all().size() + " rangos administrados.");
    }

    public void reloadCommunityConfiguration() {
        reloadConfig();
        medalRegistry = MedalRegistry.load(this);
        staffRankRegistry = StaffRankRegistry.load(getConfig());
        medalService.replaceRegistry(medalRegistry);
        staffRankService.replaceRegistry(staffRankRegistry);
        staffRankService.ensureManagedGroups();
    }
}
