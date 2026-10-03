package tv.csdm.minecraft.admin.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Dedicated holder keeps the copied items read-only even after /sm off. */
public final class StaffInspection implements InventoryHolder {
    private final Inventory inventory;

    public StaffInspection(String targetName) {
        inventory = Bukkit.createInventory(this, 54,
                Component.text("Inspección de " + targetName, NamedTextColor.DARK_AQUA));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
