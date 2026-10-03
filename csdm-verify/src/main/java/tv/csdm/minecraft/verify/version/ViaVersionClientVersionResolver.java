package tv.csdm.minecraft.verify.version;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tv.csdm.minecraft.verify.model.ClientVersion;

public final class ViaVersionClientVersionResolver implements ClientVersionResolver {
    private static final Map<Integer, String> KNOWN_PROTOCOLS = Map.ofEntries(
            Map.entry(767, "1.21–1.21.1"),
            Map.entry(768, "1.21.2–1.21.3"),
            Map.entry(769, "1.21.4"),
            Map.entry(770, "1.21.5"),
            Map.entry(771, "1.21.6"),
            Map.entry(772, "1.21.7–1.21.8"),
            Map.entry(773, "1.21.9–1.21.10"),
            Map.entry(774, "1.21.11"),
            Map.entry(775, "26.1"),
            Map.entry(776, "26.2"),
            Map.entry(777, "26.3"));

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

