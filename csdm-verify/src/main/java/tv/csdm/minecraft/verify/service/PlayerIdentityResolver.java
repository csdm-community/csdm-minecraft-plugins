package tv.csdm.minecraft.verify.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import tv.csdm.minecraft.verify.model.PlayerIdentity;

public final class PlayerIdentityResolver {
    private final JavaPlugin plugin;
    public PlayerIdentityResolver(JavaPlugin plugin) { this.plugin = plugin; }

    public PlayerIdentity resolve(Player player) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            var api = FloodgateApi.getInstance();
            if (api == null) throw new IllegalArgumentException("Floodgate no está listo");
            if (api.isFloodgatePlayer(player.getUniqueId())) {
                var bedrock = api.getPlayer(player.getUniqueId());
                // Global/local account linking changes the UUID. CSDM keeps each edition separate.
                if (bedrock == null || bedrock.isLinked())
                    throw new IllegalArgumentException("Desactiva player-link en Floodgate para usar las identidades separadas de CSDM");
                if (!plugin.getConfig().getBoolean("crossplay.bedrock-enabled", false))
                    throw new IllegalArgumentException("La verificación Bedrock aún no está habilitada");
                return new PlayerIdentity(player.getUniqueId(), bedrock.getUsername(), "bedrock",
                        bedrock.getXuid(), bedrock.getVersion());
            }
        }
        return PlayerIdentity.javaPlayer(player.getUniqueId(), player.getName());
    }
}
