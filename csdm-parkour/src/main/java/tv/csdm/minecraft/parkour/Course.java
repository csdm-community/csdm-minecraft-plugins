package tv.csdm.minecraft.parkour;

import java.util.List;
import org.bukkit.Location;

record Course(String id, Location start, List<Location> checkpoints, Location finish, Location exit, double fallY) {
    Course { checkpoints = List.copyOf(checkpoints); }

    static boolean onBlock(Location feet, Location point) {
        return feet.getWorld().equals(point.getWorld())
                && feet.getBlockX() == point.getBlockX() && feet.getBlockZ() == point.getBlockZ()
                && Math.abs(feet.getY() - point.getY()) < 0.15;
    }

    Location checkpoint(int index) { return (index == 0 ? start : checkpoints.get(index - 1)).clone(); }
}
