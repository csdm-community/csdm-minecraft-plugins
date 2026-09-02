package tv.csdm.minecraft.verify.version;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.verify.model.ClientVersion;

public final class ViaVersionClientVersionResolver implements ClientVersionResolver {
    private static final Map<Integer, String> KNOWN_PROTOCOLS = Map.of(
            767, "1.21–1.21.1",
            768, "1.21.2–1.21.3",
            769, "1.21.4",
            770, "1.21.5",
            771, "1.21.6",
            772, "1.21.7–1.21.8",
            773, "1.21.9–1.21.10",
            774, "1.21.11",
            775, "26.1",
            776, "26.2");

    private final JavaPlugin plugin;
    private volatile Method getApiMethod;
    private volatile Method getPlayerVersionMethod;

    public ViaVersionClientVersionResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ClientVersion resolve(Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ViaVersion")) {
            return ClientVersion.unknown();
        }
        try {
            initializeReflection();
            Object api = getApiMethod.invoke(null);
            int protocol = (int) getPlayerVersionMethod.invoke(api, player.getUniqueId());
            return new ClientVersion(protocol, KNOWN_PROTOCOLS.getOrDefault(protocol, "protocol-" + protocol));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | ClassCastException exception) {
            plugin.getLogger().warning("No se pudo leer el protocolo de ViaVersion: "
                    + exception.getClass().getSimpleName());
            return ClientVersion.unknown();
        }
    }

    private synchronized void initializeReflection() throws ClassNotFoundException, NoSuchMethodException {
        if (getApiMethod != null && getPlayerVersionMethod != null) {
            return;
        }
        Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
        getApiMethod = viaClass.getMethod("getAPI");
        Class<?> apiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
        getPlayerVersionMethod = apiClass.getMethod("getPlayerVersion", java.util.UUID.class);
    }
}

