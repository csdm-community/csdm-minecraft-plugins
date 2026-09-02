package tv.csdm.minecraft.admin.command;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.csdm.minecraft.admin.CSDMAdminPlugin;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final CSDMAdminPlugin plugin;

    public AdminCommand(CSDMAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        boolean spawnOnly = args.length > 0 && args[0].equalsIgnoreCase("spawn")
                && sender.hasPermission("csdm.admin.spawn");
        if (!sender.hasPermission("csdm.admin.manage") && !spawnOnly) {
            sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("estado")) {
            showStatus(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "noche" -> {
                Boolean value = parseToggle(args);
                if (value == null) {
                    sender.sendMessage(Component.text("/csdmadmin noche <on|off>", NamedTextColor.YELLOW));
                } else {
                    plugin.setPermanentNight(value);
                    sender.sendMessage(Component.text("Noche permanente: " + value, NamedTextColor.GREEN));
                }
            }
            case "mantenimiento" -> {
                Boolean value = parseToggle(args);
                if (value == null) {
                    sender.sendMessage(Component.text("/csdmadmin mantenimiento <on|off>", NamedTextColor.YELLOW));
                } else {
                    plugin.setMaintenance(value);
                    sender.sendMessage(Component.text("Modo mantenimiento: " + value, NamedTextColor.GREEN));
                }
            }
            case "aplicar" -> {
                plugin.worldPolicyService().applyConfiguredWorld();
                sender.sendMessage(Component.text("Politicas reaplicadas.", NamedTextColor.GREEN));
            }
            case "recargar" -> {
                plugin.reloadServices();
                plugin.worldPolicyService().applyConfiguredWorld();
                plugin.worldPolicyService().startEnforcementTask();
                sender.sendMessage(Component.text("CSDMAdmin recargado.", NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Solo un jugador puede fijar el spawn.", NamedTextColor.RED));
                } else {
                    plugin.worldPolicyService().saveSpawn(player.getLocation());
                    sender.sendMessage(Component.text("Spawn del lobby guardado.", NamedTextColor.GREEN));
                }
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Solo un jugador puede teletransportarse.", NamedTextColor.RED));
                } else {
                    plugin.worldPolicyService().configuredSpawn().ifPresentOrElse(
                            player::teleportAsync,
                            () -> sender.sendMessage(Component.text("No hay spawn disponible.", NamedTextColor.RED)));
                }
            }
            default -> sender.sendMessage(Component.text(
                    "/csdmadmin <estado|noche|mantenimiento|spawn|setspawn|aplicar|recargar>",
                    NamedTextColor.YELLOW));
        }
        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(Component.text("CSDMAdmin", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Mundo: " + plugin.settings().worldName(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Noche permanente: " + plugin.settings().permanentNight(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Mantenimiento: " + plugin.settings().maintenanceEnabled(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Proteccion: " + plugin.settings().protectionEnabled(), NamedTextColor.GRAY));
    }

    private Boolean parseToggle(String[] args) {
        if (args.length != 2) {
            return null;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on", "true", "si" -> true;
            case "off", "false", "no" -> false;
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("estado", "noche", "mantenimiento", "spawn", "setspawn", "aplicar", "recargar"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("noche") || args[0].equalsIgnoreCase("mantenimiento"))) {
            return filter(List.of("on", "off"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
