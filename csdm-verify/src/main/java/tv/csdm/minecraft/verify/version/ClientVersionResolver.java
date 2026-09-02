package tv.csdm.minecraft.verify.version;

import org.bukkit.entity.Player;
import tv.csdm.minecraft.verify.model.ClientVersion;

public interface ClientVersionResolver {
    ClientVersion resolve(Player player);
}

