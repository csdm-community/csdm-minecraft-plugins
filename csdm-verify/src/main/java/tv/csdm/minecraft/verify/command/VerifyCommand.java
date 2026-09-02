package tv.csdm.minecraft.verify.command;

import java.net.InetAddress;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.csdm.minecraft.verify.CSDMVerifyPlugin;
import tv.csdm.minecraft.verify.config.Messages;
import tv.csdm.minecraft.verify.config.VerifySettings;
import tv.csdm.minecraft.verify.model.ClientVersion;
import tv.csdm.minecraft.verify.model.VerificationRequest;
import tv.csdm.minecraft.verify.model.VerificationResponse;
import tv.csdm.minecraft.verify.service.VerificationCoordinator;
import tv.csdm.minecraft.verify.version.ClientVersionResolver;
import tv.csdm.minecraft.verify.world.WorldRoutingService;

public final class VerifyCommand implements CommandExecutor, TabCompleter {
    private final CSDMVerifyPlugin plugin;
    private final VerifySettings settings;
    private final Messages messages;
    private final VerificationCoordinator coordinator;
    private final ClientVersionResolver versionResolver;
    private final WorldRoutingService routing;

    public VerifyCommand(
            CSDMVerifyPlugin plugin,
            VerifySettings settings,
            Messages messages,
            VerificationCoordinator coordinator,
            ClientVersionResolver versionResolver,
            WorldRoutingService routing) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.coordinator = coordinator;
        this.versionResolver = versionResolver;
        this.routing = routing;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.prefixed("players-only"));
            return true;
        }
        if (!settings.enabled()) {
            player.sendMessage(messages.prefixed("disabled"));
            return true;
        }
        if (!routing.isVerificationWorld(player.getWorld())) {
            player.sendMessage(messages.prefixed("verify-world-only"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.prefixed("usage"));
            return true;
        }

        ClientVersion clientVersion = versionResolver.resolve(player);
        if (!isSupported(clientVersion)) {
            player.sendMessage(messages.prefixed("unsupported-version"));
            return true;
        }

        InetAddress address = player.getAddress() == null
                ? InetAddress.getLoopbackAddress()
                : player.getAddress().getAddress();
        VerificationRequest request = new VerificationRequest(
                settings.normalizeCode(args[0]),
                player.getUniqueId(),
                player.getName(),
                address,
                clientVersion.protocol(),
                clientVersion.displayName(),
                Instant.now());

        VerificationCoordinator.StartResult startResult = coordinator.start(request);
        if (startResult instanceof VerificationCoordinator.Rejected rejected) {
            String message = switch (rejected.reason()) {
                case INVALID_CODE -> "invalid-code";
                case COOLDOWN -> "cooldown";
                case RATE_LIMITED -> "rate-limited";
                case IN_FLIGHT -> "in-flight";
            };
            player.sendMessage(messages.prefixed(message));
            return true;
        }

        player.sendMessage(messages.prefixed("checking"));
        var accepted = (VerificationCoordinator.Accepted) startResult;
        accepted.response().thenAccept(response -> plugin.getServer().getScheduler().runTask(
                plugin, () -> deliver(player, response)));
        return true;
    }

    private boolean isSupported(ClientVersion version) {
        if (!version.known()) {
            return settings.allowUnknownClientVersion();
        }
        return version.protocol() >= settings.minClientProtocol()
                && version.protocol() <= settings.maxClientProtocol();
    }

    private void deliver(Player player, VerificationResponse response) {
        if (!player.isOnline()) {
            return;
        }
        switch (response.result()) {
            case VERIFIED -> celebrate(player);
            case INVALID_CODE -> player.sendMessage(messages.prefixed("invalid-backend-code"));
            case CODE_EXPIRED -> player.sendMessage(messages.prefixed("expired"));
            case CODE_USED -> player.sendMessage(messages.prefixed("used"));
            case UUID_ALREADY_LINKED -> player.sendMessage(messages.prefixed("uuid-linked"));
            case RATE_LIMITED -> player.sendMessage(messages.prefixed("backend-rate-limited"));
            case NETWORK_ERROR, SERVER_ERROR -> player.sendMessage(messages.prefixed("server-error"));
        }
    }

    private void celebrate(Player player) {
        player.sendMessage(messages.prefixed("verified"));
        player.sendMessage(messages.prefixed("verified-detail", Map.of("username", player.getName())));
        player.showTitle(Title.title(
                messages.plain("success-title"),
                messages.plain("success-subtitle"),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 40, 0.6, 1.0, 0.6, 0.02);
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (player.isOnline()) {
                        player.kick(messages.plain("kick-success"));
                    }
                },
                settings.kickAfterSuccessSeconds() * 20L);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        return List.of();
    }
}
