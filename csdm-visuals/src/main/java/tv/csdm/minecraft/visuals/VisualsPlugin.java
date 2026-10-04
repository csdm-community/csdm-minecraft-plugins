package tv.csdm.minecraft.visuals;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Bukkit reads happen only on the main thread; packet callbacks read immutable snapshots. */
public final class VisualsPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private record Pair(UUID viewer, int entityId) {}
    private volatile Map<Pair, Double> scales = Map.of();
    private Map<Pair, Double> sent = Map.of();
    private PacketListenerAbstract listener;
    private BedrockTextListener textListener;
    private volatile boolean bedrockTextEnabled;
    private BukkitTask refresh;
    private boolean enabled;
    private boolean refreshQueued;
    private double smallScale;
    private Set<String> worlds = Set.of();
    private String fullsizePermission;

    @Override public void onEnable() {
        saveDefaultConfig();
        readSettings();
        java.util.function.Predicate<UUID> bedrock = getServer().getPluginManager().isPluginEnabled("floodgate")
                ? org.geysermc.floodgate.api.FloodgateApi.getInstance()::isFloodgatePlayer : id -> false;
        textListener = new BedrockTextListener(bedrock, () -> bedrockTextEnabled);
        PacketEvents.getAPI().getEventManager().registerListener(textListener);
        getServer().getPluginManager().registerEvents(textListener, this);
        listener = new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.UPDATE_ATTRIBUTES || event.getUser() == null) return;
                var packet = new WrapperPlayServerUpdateAttributes(event);
                Double scale = scales.get(new Pair(event.getUser().getUUID(), packet.getEntityId()));
                if (scale == null) return;
                // Replace the list and property, never mutate a shared property for another recipient.
                var properties = new ArrayList<Property>();
                for (Property property : packet.getProperties()) {
                    if (!Attributes.SCALE.equals(property.getAttribute())) properties.add(property);
                }
                properties.add(property(scale));
                packet.setProperties(properties);
                event.markForReEncode(true);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("csdmvisuals")).setExecutor(this);
        schedule();
    }

    private void readSettings() {
        bedrockTextEnabled = getConfig().getBoolean("bedrock-text.enabled", true);
        enabled = getConfig().getBoolean("enabled", false);
        smallScale = getConfig().getDouble("other-player-scale", .3);
        if (!Double.isFinite(smallScale) || smallScale < .0625 || smallScale > 1) throw new IllegalArgumentException("other-player-scale debe estar entre 0.0625 y 1");
        worlds = Set.copyOf(getConfig().getStringList("worlds"));
        fullsizePermission = getConfig().getString("fullsize-permission", "csdm.visual.fullsize");
    }
    private void schedule() {
        if (refresh != null) refresh.cancel();
        refreshNow();
        refresh = getServer().getScheduler().runTaskTimer(this, () -> refreshNow(), 1, Math.max(1, getConfig().getLong("refresh-ticks", 20)));
    }

    private void refreshNow() { refreshNow(false); }

    private void refreshNow(boolean force) {
        Map<Pair, Double> next = new HashMap<>();
        Map<Pair, Double> desired = new HashMap<>();
        for (Player target : getServer().getOnlinePlayers()) {
            Set<Player> viewers = new HashSet<>(target.getTrackedBy());
            viewers.add(target);
            for (Player viewer : viewers) {
                if (!viewer.canSee(target) || !viewer.getWorld().equals(target.getWorld())) continue;
                Pair pair = new Pair(viewer.getUniqueId(), target.getEntityId());
                boolean inLobby = enabled && worlds.contains(target.getWorld().getName())
                        && (getConfig().getBoolean("bedrock-viewers-enabled", false) || !isBedrock(viewer));
                double scale = inLobby ? ScalePolicy.scale(viewer.equals(target), target.hasPermission(fullsizePermission), smallScale) : actual(target);
                if (inLobby) next.put(pair, scale);
                if (inLobby || sent.containsKey(pair)) desired.put(pair, scale);
            }
        }
        scales = Map.copyOf(next);
        for (var entry : desired.entrySet()) {
            if (force || !Objects.equals(sent.get(entry.getKey()), entry.getValue()) || !next.containsKey(entry.getKey())) {
                Player viewer = getServer().getPlayer(entry.getKey().viewer());
                if (viewer != null) send(viewer, entry.getKey().entityId(), entry.getValue());
            }
        }
        sent = Map.copyOf(next);
    }

    private boolean isBedrock(Player player) {
        return getServer().getPluginManager().isPluginEnabled("floodgate")
                && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    private static double actual(Player target) {
        var attribute = target.getAttribute(Attribute.SCALE);
        return attribute == null ? 1 : attribute.getValue();
    }
    private static Property property(double value) { return new Property(Attributes.SCALE, value, List.of()); }
    private static void send(Player viewer, int entityId, double scale) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer,
                new WrapperPlayServerUpdateAttributes(entityId, List.of(property(scale))));
    }
    private void laterRefresh() {
        if (refreshQueued) return;
        refreshQueued = true;
        getServer().getScheduler().runTask(this, () -> { refreshQueued = false; refreshNow(true); });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void track(PlayerTrackEntityEvent e) {
        if (e.getEntity() instanceof Player) laterRefresh();
    }
    @EventHandler(priority = EventPriority.MONITOR) public void join(PlayerJoinEvent e) { refreshNow(); laterRefresh(); }
    @EventHandler(priority = EventPriority.MONITOR) public void respawn(PlayerRespawnEvent e) { laterRefresh(); }
    @EventHandler(priority = EventPriority.MONITOR) public void world(PlayerChangedWorldEvent e) { refreshNow(); laterRefresh(); }
    @EventHandler(priority = EventPriority.MONITOR) public void quit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId(); int entityId = e.getPlayer().getEntityId();
        Map<Pair, Double> remaining = new HashMap<>(scales);
        remaining.keySet().removeIf(p -> p.viewer().equals(id) || p.entityId() == entityId);
        scales = Map.copyOf(remaining);
        laterRefresh();
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        try { readSettings(); schedule(); sender.sendMessage(Component.text("Escala visual: " + (enabled ? "activada" : "desactivada"))); }
        catch (IllegalArgumentException e) {
            enabled = false; refreshNow();
            sender.sendMessage(Component.text("Configuración inválida; escala desactivada: " + e.getMessage()));
        }
        return true;
    }
    @Override public void onDisable() {
        if (refresh != null) refresh.cancel();
        scales = Map.of();
        if (listener != null) PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        if (textListener != null) PacketEvents.getAPI().getEventManager().unregisterListener(textListener);
        for (Player target : getServer().getOnlinePlayers()) {
            Set<Player> viewers = new HashSet<>(target.getTrackedBy()); viewers.add(target);
            for (Player viewer : viewers) if (viewer.canSee(target) && sent.containsKey(new Pair(viewer.getUniqueId(), target.getEntityId())))
                send(viewer, target.getEntityId(), actual(target));
        }
        sent = Map.of();
    }
}
