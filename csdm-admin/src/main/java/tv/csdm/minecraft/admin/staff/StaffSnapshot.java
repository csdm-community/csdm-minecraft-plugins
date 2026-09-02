package tv.csdm.minecraft.admin.staff;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record StaffSnapshot(
        ItemStack[] storage,
        ItemStack[] armor,
        ItemStack offhand,
        Location location,
        GameMode gameMode,
        boolean allowFlight,
        boolean flying,
        boolean invulnerable,
        boolean collidable,
        boolean canPickupItems,
        int heldSlot) {
}
