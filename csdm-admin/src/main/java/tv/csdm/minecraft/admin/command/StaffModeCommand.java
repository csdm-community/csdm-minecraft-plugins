package tv.csdm.minecraft.admin.command;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tv.csdm.minecraft.admin.staff.StaffModeService;

public final class StaffModeCommand implements CommandExecutor, TabCompleter {
    private final StaffModeService staffMode;

    public StaffModeCommand(StaffModeService staffMode) {
        this.staffMode = staffMode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede usarse dentro del juego.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("csdm.staffmode.use")) {
            player.sendMessage(Component.text("No tienes permiso para usar Staff Mode.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            toggle(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "on", "activar" -> staffMode.enable(player);
            case "off", "desactivar" -> staffMode.disable(player);
            case "vanish", "ocultar" -> {
                if (!requireActive(player)) {
                    return true;
                }
                boolean vanished = staffMode.toggleVanish(player);
                player.sendMessage(Component.text(
                        vanished ? "Ahora eres invisible para los usuarios." : "Ahora eres visible para todos.",
                        vanished ? NamedTextColor.AQUA : NamedTextColor.YELLOW));
            }
            case "tp", "teleportar" -> withTarget(player, args, target -> {
                player.teleportAsync(target.getLocation());
                player.sendMessage(Component.text("Te has teletransportado a " + target.getName() + ".", NamedTextColor.AQUA));
            });
            case "freeze", "congelar" -> withTarget(player, args, target -> {
                boolean frozen = staffMode.toggleFreeze(target);
                target.sendMessage(Component.text(
                        frozen ? "Has sido congelado por el equipo de moderación." : "Ya puedes moverte de nuevo.",
                        frozen ? NamedTextColor.RED : NamedTextColor.GREEN));
                player.sendMessage(Component.text(
                        target.getName() + (frozen ? " quedó congelado." : " fue liberado."),
                        NamedTextColor.AQUA));
            });
            case "inspect", "inspeccionar" -> withTarget(player, args, target -> staffMode.openInspection(player, target));
            case "ayuda", "help" -> help(player);
            default -> help(player);
        }
        return true;
    }

    private void toggle(Player player) {
        if (staffMode.isActive(player)) {
            staffMode.disable(player);
        } else {
            staffMode.enable(player);
        }
    }

    private void withTarget(Player player, String[] args, java.util.function.Consumer<Player> action) {
        if (!requireActive(player)) {
            return;
        }
        if (args.length < 2) {
            showTargets(player, args[0]);
            return;
        }
        Player target = player.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Ese jugador no está conectado.", NamedTextColor.RED));
            return;
        }
        action.accept(target);
    }

    private boolean requireActive(Player player) {
        if (staffMode.isActive(player)) {
            return true;
        }
        player.sendMessage(Component.text("Activa primero /staff o /sm.", NamedTextColor.RED));
        return false;
    }

    private void showTargets(Player player, String action) {
        player.sendMessage(Component.text("Selecciona un jugador:", NamedTextColor.AQUA));
        for (Player target : player.getServer().getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }
            Component entry = Component.text("• " + target.getName(), NamedTextColor.WHITE)
                    .clickEvent(ClickEvent.runCommand("/staff " + action + " " + target.getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("Haz clic para seleccionar", NamedTextColor.GRAY)));
            player.sendMessage(entry);
        }
    }

    private void help(Player player) {
        player.sendMessage(Component.text("Staff Mode CSDM", NamedTextColor.AQUA));
        player.sendMessage(Component.text("/staff o /sm — activar/desactivar", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/staff tp|congelar|inspeccionar <jugador>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/sancionar <jugador> <advertir|expulsar|suspender|bloquear|perdonar>", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("activar", "desactivar", "vanish", "tp", "congelar", "inspeccionar", "ayuda"), args[0]);
        }
        if (args.length == 2 && List.of("tp", "teleportar", "freeze", "congelar", "inspect", "inspeccionar")
                .contains(args[0].toLowerCase())) {
            return filter(sender.getServer().getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(normalized)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
