package tv.csdm.minecraft.admin.command;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class FlyCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede usarse dentro del juego.", NamedTextColor.RED));
            return true;
        }
        Player target = player;
        if (args.length > 0) {
            if (!player.hasPermission("csdm.command.fly.others")) {
                player.sendMessage(Component.text("No puedes cambiar el vuelo de otros jugadores.", NamedTextColor.RED));
                return true;
            }
            target = player.getServer().getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Ese jugador no está conectado.", NamedTextColor.RED));
                return true;
            }
        }
        boolean enabled = !target.getAllowFlight();
        target.setAllowFlight(enabled);
        if (!enabled) {
            target.setFlying(false);
        }
        target.sendMessage(Component.text("Vuelo " + (enabled ? "activado." : "desactivado."),
                enabled ? NamedTextColor.AQUA : NamedTextColor.YELLOW));
        if (!target.equals(player)) {
            player.sendMessage(Component.text("Vuelo actualizado para " + target.getName() + ".", NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("csdm.command.fly.others")) {
            String prefix = args[0].toLowerCase();
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
