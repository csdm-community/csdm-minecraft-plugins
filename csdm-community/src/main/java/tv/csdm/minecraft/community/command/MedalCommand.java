package tv.csdm.minecraft.community.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.csdm.minecraft.community.CSDMCommunityPlugin;
import tv.csdm.minecraft.community.medal.MedalDefinition;
import tv.csdm.minecraft.community.medal.MedalService;

public final class MedalCommand implements CommandExecutor, TabCompleter {
    private final CSDMCommunityPlugin plugin;
    private final MedalService service;

    public MedalCommand(CSDMCommunityPlugin plugin, MedalService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1])
                    : sender instanceof Player player ? player : null;
            if (target == null) {
                sender.sendMessage(Component.text("Indica un jugador conectado.", NamedTextColor.RED));
                return true;
            }
            show(sender, target);
            return true;
        }
        if (args[0].equalsIgnoreCase("recargar")) {
            if (!sender.hasPermission("csdm.medals.manage")) {
                return noPermission(sender);
            }
            plugin.reloadCommunityConfiguration();
            sender.sendMessage(Component.text("Configuracion de comunidad recargada.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length != 3 || !sender.hasPermission("csdm.medals.manage")) {
            if (!sender.hasPermission("csdm.medals.manage")) {
                return noPermission(sender);
            }
            sender.sendMessage(Component.text(
                    "/medalla <otorgar|retirar|destacar> <jugador> <id>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("El jugador debe estar conectado.", NamedTextColor.RED));
            return true;
        }
        try {
            boolean changed = switch (args[0].toLowerCase(Locale.ROOT)) {
                case "otorgar" -> service.grant(target.getUniqueId(), args[2]);
                case "retirar" -> service.revoke(target.getUniqueId(), args[2]);
                case "destacar" -> service.feature(target.getUniqueId(), args[2]);
                default -> throw new IllegalArgumentException("Accion desconocida");
            };
            sender.sendMessage(Component.text(
                    changed ? "Operacion aplicada correctamente." : "No hubo cambios.",
                    changed ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void show(CommandSender sender, Player target) {
        List<MedalDefinition> medals = service.unlocked(target.getUniqueId());
        sender.sendMessage(Component.text("Medallas de " + target.getName(), NamedTextColor.AQUA));
        if (medals.isEmpty()) {
            sender.sendMessage(Component.text("Aun no tiene medallas.", NamedTextColor.GRAY));
            return;
        }
        String featured = service.featured(target.getUniqueId()).map(MedalDefinition::id).orElse(null);
        for (MedalDefinition medal : medals) {
            String marker = medal.id().equals(featured) ? " ★" : "";
            sender.sendMessage(Component.text(
                    medal.symbol() + " " + medal.name() + marker + " — " + medal.description(),
                    NamedTextColor.GRAY));
        }
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("lista", "otorgar", "retirar", "destacar", "recargar"), args[0]);
        }
        if (args.length == 2 && List.of("otorgar", "retirar", "destacar", "lista")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3) {
            return filter(service.definitions().stream().map(MedalDefinition::id).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

