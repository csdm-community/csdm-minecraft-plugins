package tv.csdm.minecraft.visuals;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Per-recipient presentation only; never changes player identities or signed chat bodies. */
final class BedrockTextListener extends PacketListenerAbstract implements Listener {
    private final Predicate<UUID> isBedrock;
    private final BooleanSupplier enabled;

    BedrockTextListener(Predicate<UUID> isBedrock, BooleanSupplier enabled) {
        super(PacketListenerPriority.HIGHEST);
        this.isBedrock = isBedrock;
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // CSDMCommunity loads first. Wrap its renderer instead of replacing its format.
        ChatRenderer previous = event.renderer();
        event.renderer((source, displayName, message, viewer) -> {
            boolean bedrock = enabled.getAsBoolean() && viewer instanceof Player player
                    && isBedrock.test(player.getUniqueId());
            return BedrockText.forViewer(previous.render(source,
                    bedrock ? BedrockRankStyle.name(displayName) : displayName, message, viewer), bedrock);
        });
    }

    @Override public void onPacketSend(PacketSendEvent event) {
        if (!enabled.getAsBoolean() || event.getUser() == null) return;
        var type = event.getPacketType();
        if (type != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE
                && type != PacketType.Play.Server.TEAMS
                && type != PacketType.Play.Server.PLAYER_INFO_UPDATE) return;
        UUID viewer = event.getUser().getUUID();
        if (viewer == null || !isBedrock.test(viewer)) return;

        if (type == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            var packet = new WrapperPlayServerSystemChatMessage(event);
            Component original = packet.getMessage();
            Component compatible = BedrockText.forViewer(BedrockRankStyle.systemMessage(original), true);
            if (!compatible.equals(original)) {
                packet.setMessage(compatible);
                event.markForReEncode(true);
            }
        } else if (type == PacketType.Play.Server.TEAMS) {
            var packet = new WrapperPlayServerTeams(event);
            if (!packet.getTeamName().startsWith("csdm_t_")) return;
            packet.getTeamInfo().ifPresent(info -> {
                // The wrapper copy is shallow: do not mutate an info object shared with Java.
                packet.setTeamInfo(compatibleTeam(info));
                event.markForReEncode(true);
            });
        } else {
            var packet = new WrapperPlayServerPlayerInfoUpdate(event);
            if (!packet.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME)) return;
            packet.setEntries(packet.getEntries().stream().map(BedrockTextListener::compatibleEntry).toList());
            event.markForReEncode(true);
        }
    }

    static ScoreBoardTeamInfo compatibleTeam(ScoreBoardTeamInfo info) {
        return new ScoreBoardTeamInfo(BedrockText.forViewer(BedrockRankStyle.display(info.getDisplayName()), true),
                BedrockText.forViewer(BedrockRankStyle.display(info.getPrefix()), true),
                BedrockText.forViewer(BedrockRankStyle.display(info.getSuffix()), true),
                info.getTagVisibility(), info.getCollisionRule(), info.getColor(), info.getOptionData());
    }

    static WrapperPlayServerPlayerInfoUpdate.PlayerInfo compatibleEntry(
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry) {
        var copy = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(entry);
        copy.setDisplayName(BedrockText.forViewer(BedrockRankStyle.name(entry.getDisplayName()), true));
        return copy;
    }
}
