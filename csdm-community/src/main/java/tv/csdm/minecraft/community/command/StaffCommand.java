package tv.csdm.minecraft.community.command;

import java.util.List;
import java.util.Locale;
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
import tv.csdm.minecraft.community.staff.RankKind;
import tv.csdm.minecraft.community.staff.StaffRankDefinition;
import tv.csdm.minecraft.community.staff.StaffRankService;

public final class StaffCommand implements CommandExecutor, TabCompleter {
    private final CSDMCommunityPlugin plugin;
    private final StaffRankService service;

    public StaffCommand(CSDMCommunityPlugin plugin, StaffRankService service) {
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
            sender.sendMessage(Component.text("Rangos funcionales y de prestigio CSDM:", NamedTextColor.AQUA));
            service.ranks().forEach(rank -> sender.sendMessage(Component.text(
                    "- " + rank.id() + " → " + rank.displayName() + " ["
                            + rank.kind().name().toLowerCase(Locale.ROOT) + "]",
                    NamedTextColor.GRAY)));
            return true;
        }
        if (args[0].equalsIgnoreCase("recargar")) {
            if (!sender.hasPermission("csdm.ranks.manage")) {
                return noPermission(sender);
            }
            plugin.reloadCommunityConfiguration();
            sender.sendMessage(Component.text("Rangos recargados.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                    "/rangos <ver|asignar|retirar> <jugador> [rango|funcional|prestigio]",
                    NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("El jugador debe estar conectado.", NamedTextColor.RED));
            return true;
        }
        if (args[0].equalsIgnoreCase("ver")) {
            String functional = service.current(target, RankKind.FUNCTIONAL)
                    .map(StaffRankDefinition::displayName).orElse("Usuario");
            String prestige = service.current(target, RankKind.PRESTIGE)
                    .map(StaffRankDefinition::displayName).orElse("Sin prestigio");
            sender.sendMessage(Component.text(
                    target.getName() + ": " + functional + " · " + prestige,
                    NamedTextColor.AQUA));
            return true;
        }
        if (!sender.hasPermission("csdm.ranks.manage")) {
            return noPermission(sender);
        }
        if (args[0].equalsIgnoreCase("retirar") && args.length == 3) {
            RankKind kind = parseKind(args[2]);
            if (kind == null) {
                sender.sendMessage(Component.text("Indica funcional o prestigio.", NamedTextColor.RED));
                return true;
            }
            service.clear(target, kind).thenAccept(changed -> onMain(() -> {
                service.syncDisplay(target);
                sender.sendMessage(Component.text(
                        changed ? "Rango retirado." : "El jugador no tenia un rango administrado.",
                        changed ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            }));
            return true;
        }
        if (args[0].equalsIgnoreCase("asignar") && args.length == 3) {
            try {
                service.assign(target, args[2]).thenRun(() -> onMain(() -> {
                    service.syncDisplay(target);
                    sender.sendMessage(Component.text("Rango asignado correctamente.", NamedTextColor.GREEN));
                }));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return true;
        }
        sender.sendMessage(Component.text("/rangos asignar <jugador> <rango>", NamedTextColor.YELLOW));
        return true;
    }

    private void onMain(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
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
            return filter(List.of("lista", "ver", "asignar", "retirar", "recargar"), args[0]);
        }
        if (args.length == 2) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("asignar")) {
            return filter(service.ranks().stream().map(StaffRankDefinition::id).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("retirar")) {
            return filter(List.of("funcional", "prestigio"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    private RankKind parseKind(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "funcional", "staff" -> RankKind.FUNCTIONAL;
            case "prestigio", "medalla" -> RankKind.PRESTIGE;
            default -> null;
        };
    }
}
