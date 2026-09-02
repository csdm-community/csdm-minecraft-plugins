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
            sender.sendMessage(Component.text("Rangos administrados por CSDM:", NamedTextColor.AQUA));
            service.ranks().forEach(rank -> sender.sendMessage(Component.text(
                    "- " + rank.id() + " → " + rank.displayName() + " [" + rank.group() + "]",
                    NamedTextColor.GRAY)));
            return true;
        }
        if (args[0].equalsIgnoreCase("recargar")) {
            if (!sender.hasPermission("csdm.staff.manage")) {
                return noPermission(sender);
            }
            plugin.reloadCommunityConfiguration();
            sender.sendMessage(Component.text("Rangos recargados.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/staff <ver|asignar|retirar> <jugador> [rango]", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("El jugador debe estar conectado.", NamedTextColor.RED));
            return true;
        }
        if (args[0].equalsIgnoreCase("ver")) {
            String rank = service.current(target).map(StaffRankDefinition::displayName).orElse("Sin rango CSDM");
            sender.sendMessage(Component.text(target.getName() + ": " + rank, NamedTextColor.AQUA));
            return true;
        }
        if (!sender.hasPermission("csdm.staff.manage")) {
            return noPermission(sender);
        }
        if (args[0].equalsIgnoreCase("retirar")) {
            service.clear(target).thenAccept(changed -> onMain(() -> {
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
        sender.sendMessage(Component.text("/staff asignar <jugador> <rango>", NamedTextColor.YELLOW));
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
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

