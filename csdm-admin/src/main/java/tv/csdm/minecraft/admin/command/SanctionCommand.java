package tv.csdm.minecraft.admin.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tv.csdm.minecraft.admin.moderation.DurationParser;
import tv.csdm.minecraft.admin.moderation.SanctionRepository;
import tv.csdm.minecraft.admin.moderation.SanctionService;
import tv.csdm.minecraft.admin.moderation.SanctionType;

public final class SanctionCommand implements CommandExecutor, TabCompleter {
    private static final Duration MOD_MAX_TEMPBAN = Duration.ofDays(7);
    private final SanctionService sanctions;

    public SanctionCommand(SanctionService sanctions) {
        this.sanctions = sanctions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player actor)) {
            sender.sendMessage(Component.text("Este comando solo puede usarse dentro del juego.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            usage(actor);
            return true;
        }
        SanctionType type = parseType(args[1]);
        if (type == null) {
            usage(actor);
            return true;
        }
        String permission = permission(type);
        if (!actor.hasPermission(permission)) {
            actor.sendMessage(Component.text("No tienes permiso para aplicar esa sanción.", NamedTextColor.RED));
            return true;
        }
        if (type == SanctionType.PARDON) {
            pardon(actor, args);
            return true;
        }
        Player target = actor.getServer().getPlayerExact(args[0]);
        if (target == null) {
            actor.sendMessage(Component.text(
                    "Para sancionar, el jugador debe estar conectado. Los perdones sí admiten jugadores desconectados.",
                    NamedTextColor.RED));
            return true;
        }
        if (target.equals(actor)) {
            actor.sendMessage(Component.text("No puedes sancionarte a ti mismo.", NamedTextColor.RED));
            return true;
        }

        Duration duration = null;
        int reasonStart = 2;
        if (type == SanctionType.TEMPBAN) {
            if (args.length < 4) {
                usage(actor);
                return true;
            }
            try {
                duration = DurationParser.parse(args[2]);
            } catch (IllegalArgumentException exception) {
                actor.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                return true;
            }
            if (duration.compareTo(MOD_MAX_TEMPBAN) > 0 && !actor.hasPermission("csdm.sanctions.unlimited")) {
                actor.sendMessage(Component.text("Un Mod puede suspender como máximo 7 días.", NamedTextColor.RED));
                return true;
            }
            reasonStart = 3;
        }
        String reason = reason(args, reasonStart);
        if (!validReason(actor, reason)) {
            return true;
        }
        var sanction = sanctions.apply(actor, target, type, reason, duration);
        actor.sendMessage(Component.text(
                type.displayName() + " aplicada a " + target.getName() + " · ID " + sanction.id(),
                NamedTextColor.GREEN));
    
        return true;
    }

    private void pardon(Player actor, String[] args) {
        String reason = reason(args, 2);
        if (!validReason(actor, reason)) {
            return;
        }
        Player online = actor.getServer().getPlayerExact(args[0]);
        SanctionRepository.KnownTarget target = online == null
                ? sanctions.knownTarget(args[0])
                : new SanctionRepository.KnownTarget(online.getUniqueId(), online.getName());
        if (target == null) {
            actor.sendMessage(Component.text("No existe historial local para ese jugador.", NamedTextColor.RED));
            return;
        }
        var sanction = sanctions.pardon(actor, target.uuid(), target.name(), reason);
        actor.sendMessage(Component.text(
                "Sanciones activas retiradas para " + target.name() + " · ID " + sanction.id(),
                NamedTextColor.GREEN));
    }

    private boolean validReason(Player actor, String reason) {
        if (reason.length() < 3 || reason.length() > 240) {
            actor.sendMessage(Component.text("El motivo debe tener entre 3 y 240 caracteres.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private String reason(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }

    private SanctionType parseType(String raw) {
        return switch (raw.toLowerCase()) {
            case "advertir", "advertencia", "warn" -> SanctionType.WARNING;
            case "expulsar", "kick" -> SanctionType.KICK;
            case "suspender", "tempban" -> SanctionType.TEMPBAN;
            case "bloquear", "ban" -> SanctionType.BAN;
            case "perdonar", "pardon" -> SanctionType.PARDON;
            default -> null;
        };
    }

    private String permission(SanctionType type) {
        return switch (type) {
            case WARNING -> "csdm.sanctions.warn";
            case KICK -> "csdm.sanctions.kick";
            case TEMPBAN -> "csdm.sanctions.tempban";
            case BAN -> "csdm.sanctions.ban";
            case PARDON -> "csdm.sanctions.pardon";
        };
    }

    private void usage(Player player) {
        player.sendMessage(Component.text("Uso de sanciones CSDM:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("/sancionar <jugador> advertir <motivo>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/sancionar <jugador> expulsar <motivo>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/sancionar <jugador> suspender <30m|12h|7d> <motivo>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/sancionar <jugador> bloquear|perdonar <motivo>", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(sender.getServer().getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
        }
        if (args.length == 2) {
            return filter(List.of("advertir", "expulsar", "suspender", "bloquear", "perdonar"), args[1]);
        }
        if (args.length == 3 && List.of("suspender", "tempban").contains(args[1].toLowerCase())) {
            return filter(List.of("30m", "12h", "1d", "7d"), args[2]);
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
