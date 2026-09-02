package tv.csdm.minecraft.community.presence;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tv.csdm.minecraft.community.CSDMCommunityPlugin;
import tv.csdm.minecraft.community.medal.MedalDefinition;
import tv.csdm.minecraft.community.medal.MedalService;
import tv.csdm.minecraft.community.staff.StaffRankDefinition;
import tv.csdm.minecraft.community.staff.StaffRankService;

public final class CommunityPresenceListener implements Listener {
    private final CSDMCommunityPlugin plugin;
    private final StaffRankService staffRanks;
    private final MedalService medals;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CommunityPresenceListener(
            CSDMCommunityPlugin plugin,
            StaffRankService staffRanks,
            MedalService medals) {
        this.plugin = plugin;
        this.staffRanks = staffRanks;
        this.medals = medals;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) {
            return;
        }
        String path = !event.getPlayer().hasPlayedBefore()
                ? "presence-messages.first-join"
                : overridePath("join", event.getPlayer());
        event.joinMessage(render(path, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled()) {
            return;
        }
        event.quitMessage(render(overridePath("quit", event.getPlayer()), event.getPlayer()));
    }

    private String overridePath(String kind, Player player) {
        return staffRanks.current(player)
                .map(StaffRankDefinition::id)
                .map(id -> "presence-messages." + kind + "-rank-overrides." + id)
                .filter(path -> plugin.getConfig().isString(path))
                .orElse("presence-messages." + kind);
    }

    private Component render(String path, Player player) {
        FileConfiguration config = plugin.getConfig();
        String template = config.getString(path, "<gray><player></gray>");
        StaffRankDefinition rank = staffRanks.current(player).orElse(null);
        MedalDefinition medal = config.getBoolean("presence-messages.show-featured-medal", true)
                ? medals.featured(player.getUniqueId()).orElse(null)
                : null;

        Component medalComponent = medal == null
                ? Component.empty()
                : Component.text("  " + medal.symbol() + " " + medal.name());
        Component rankComponent = rank == null ? Component.empty() : Component.text(rank.displayName());
        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.component("player", Component.text(player.getName())));
        resolvers.add(Placeholder.component("rank", rankComponent));
        resolvers.add(Placeholder.component("medal", medalComponent));
        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("presence-messages.enabled", true);
    }
}

