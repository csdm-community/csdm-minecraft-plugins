package tv.csdm.minecraft.parkour;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class ParkourPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    // Shared ownership contract with CSDMAdmin. Runtime metadata, not a persisted stale lock.
    public static final String ACTIVE = "csdm_parkour_active";
    public static final String STAFF = "csdm_staff_active";
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Set<UUID> internalTeleports = new HashSet<>();
    private final Set<UUID> recovering = new HashSet<>();
    private final Map<UUID, Long> startCooldown = new HashMap<>();
    private SnapshotStore snapshots;
    private NamespacedKey actionKey;

    private static final class Session {
        final Course course;
        final RunProgress progress;
        boolean hidden;
        boolean finishing;
        long lastTool = System.nanoTime() - 300_000_000L;
        Session(Course course) { this.course = course; this.progress = new RunProgress(course.checkpoints().size(), System.nanoTime()); }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        actionKey = new NamespacedKey(this, "action");
        try { snapshots = new SnapshotStore(getDataFolder().toPath().resolve("recovery")); }
        catch (Exception e) { throw new IllegalStateException("No se puede abrir la recuperación de inventarios", e); }
        loadCourses();
        Objects.requireNonNull(getCommand("parkour")).setExecutor(this);
        Objects.requireNonNull(getCommand("parkour")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : getServer().getOnlinePlayers()) recover(player);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                Session s = sessions.get(p.getUniqueId());
                if (s != null) p.sendActionBar(Component.text(s.course.id() + " • " + RunProgress.format(s.progress.elapsedMillis(System.nanoTime()))
                        + " • CP " + s.progress.checkpoint() + "/" + s.course.checkpoints().size(), NamedTextColor.AQUA));
            }
        }, 2, 2);
    }

    @Override public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (sessions.containsKey(player.getUniqueId())) leave(player, false);
        }
    }

    private void loadCourses() {
        courses.clear();
        var root = getConfig().getConfigurationSection("courses");
        if (root == null) return;
        Set<String> occupied = new HashSet<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled")) continue;
            try {
                Location start = point(section, "start"), finish = point(section, "finish"), exit = point(section, "exit");
                var points = new ArrayList<Location>();
                int count = section.getInt("checkpoint-count");
                if (count < 0 || count > 1000) throw new IllegalArgumentException("Cantidad de checkpoints inválida");
                for (int i = 1; i <= count; i++) points.add(point(section, "checkpoints." + i));
                var all = new ArrayList<>(points); all.add(start); all.add(finish);
                Set<String> local = new HashSet<>();
                for (Location location : all) {
                    if (!location.getWorld().equals(start.getWorld())) throw new IllegalArgumentException("Todos los puntos deben estar en el mismo mundo");
                    String key = blockKey(location);
                    if (!local.add(key) || occupied.contains(key)) throw new IllegalArgumentException("Hay bloques de recorrido superpuestos");
                }
                if (!start.getWorld().equals(exit.getWorld()) || local.contains(blockKey(exit)))
                    throw new IllegalArgumentException("La salida debe estar fuera del recorrido, en el mismo mundo");
                if (!getConfig().getStringList("worlds").contains(start.getWorld().getName())) throw new IllegalArgumentException("Mundo no autorizado");
                double fallY = section.getDouble("fall-y", -10);
                if (!Double.isFinite(fallY) || all.stream().anyMatch(p -> p.getY() <= fallY)) throw new IllegalArgumentException("La cota de caída debe quedar debajo de todos los puntos");
                courses.put(id, new Course(id, start, points, finish, exit, fallY));
                occupied.addAll(local);
            } catch (Exception e) { getLogger().warning("Recorrido " + id + " desactivado: " + e.getMessage()); }
        }
    }

    private Location point(ConfigurationSection section, String name) {
        Location p = section.getLocation(name);
        if (p == null || p.getWorld() == null) throw new IllegalArgumentException("Falta " + name + " o su mundo no está cargado");
        p.checkFinite();
        return p;
    }
    private static String blockKey(Location p) { return p.getWorld().getUID() + ":" + p.getBlockX() + ":" + p.getBlockY() + ":" + p.getBlockZ(); }
    private boolean active(Player p) { return sessions.containsKey(p.getUniqueId()) || recovering.contains(p.getUniqueId()); }
    private void message(CommandSender p, String text) { p.sendMessage(Component.text(text, NamedTextColor.AQUA)); }

    private void start(Player p, Course course) {
        Long cooldown = startCooldown.get(p.getUniqueId());
        if (active(p) || !p.hasPermission("csdm.parkour.use") || p.hasMetadata(STAFF)
                || p.isDead() || p.isInsideVehicle() || p.isGliding() || p.getGameMode() == GameMode.SPECTATOR
                || (cooldown != null && System.nanoTime() - cooldown < 0)) return;
        // Refuse snapshots of another plugin's tools, including an older CSDMAdmin installation.
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().getKeys()
                    .stream().anyMatch(k -> k.getNamespace().equals("csdmadmin") && k.getKey().equals("staff_action"))) {
                message(p, "Sal de Staff Mode antes de comenzar."); return;
            }
        }
        p.closeInventory();
        if (!p.getItemOnCursor().getType().isAir()) { message(p, "Guarda el objeto del cursor antes de comenzar."); return; }
        try { snapshots.capture(p); }
        catch (Exception e) {
            startCooldown.put(p.getUniqueId(), System.nanoTime() + 5_000_000_000L);
            getLogger().severe("No se inició el parkour de " + p.getUniqueId() + ": " + e.getMessage());
            message(p, "No pudimos respaldar tu inventario. El parkour no ha comenzado."); return;
        }
        Session s = new Session(course);
        sessions.put(p.getUniqueId(), s);
        p.setMetadata(ACTIVE, new org.bukkit.metadata.FixedMetadataValue(this, true));
        try {
            p.setFlying(false); p.setAllowFlight(false); p.setGameMode(GameMode.ADVENTURE); p.setCollidable(false);
            p.getInventory().clear();
            tool(p, 0, Material.COMPASS, "Volver al último checkpoint", "checkpoint");
            tool(p, 1, Material.LIME_DYE, "Reiniciar parkour", "restart");
            tool(p, 4, Material.CLOCK, "Tiempo y progreso", "time");
            tool(p, 7, Material.ENDER_EYE, "Ocultar / mostrar jugadores", "visibility");
            tool(p, 8, Material.RED_DYE, "Abandonar parkour", "exit");
            p.getInventory().setHeldItemSlot(0);
            message(p, "Parkour iniciado: " + course.id());
        } catch (RuntimeException e) { getLogger().severe(e.toString()); leave(p, false); }
    }

    private void tool(Player p, int slot, Material material, String title, String action) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.AQUA));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta); p.getInventory().setItem(slot, item);
    }

    private boolean teleport(Player p, Location target) {
        internalTeleports.add(p.getUniqueId());
        try {
            p.setFallDistance(0); p.setVelocity(new Vector());
            return p.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } finally { internalTeleports.remove(p.getUniqueId()); }
    }

    private void returnToCheckpoint(Player p, Session s, boolean restart) {
        if (teleport(p, s.course.checkpoint(restart ? 0 : s.progress.checkpoint()))) {
            if (restart) s.progress.restart(System.nanoTime());
        } else message(p, "El teletransporte fue rechazado; tu sesión sigue activa.");
    }

    private boolean leave(Player p, boolean goToExit) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null) return !recovering.contains(p.getUniqueId());
        if (goToExit && !teleport(p, s.course.exit())) { message(p, "No se pudo llegar a la salida. Intenta de nuevo."); return false; }
        if (!recover(p)) return false;
        message(p, "Has salido del parkour.");
        return true;
    }

    private boolean recover(Player p) {
        try {
            if (sessions.containsKey(p.getUniqueId()) && !snapshots.contains(p.getUniqueId()))
                throw new java.io.IOException("Falta el respaldo de una sesión activa");
            snapshots.restore(p);
            sessions.remove(p.getUniqueId()); recovering.remove(p.getUniqueId());
            p.removeMetadata(ACTIVE, this);
            for (Player other : getServer().getOnlinePlayers()) p.showPlayer(this, other);
            startCooldown.put(p.getUniqueId(), System.nanoTime() + 2_000_000_000L);
            p.sendActionBar(Component.empty());
            return true;
        } catch (Exception e) {
            recovering.add(p.getUniqueId());
            getLogger().severe("Recuperación pendiente para " + p.getUniqueId() + ": " + e.getMessage());
            p.kick(Component.text("No se pudo recuperar tu inventario. Tu respaldo sigue guardado; contacta al staff."));
            return false;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) public void join(PlayerJoinEvent e) {
        // Restore inventory before the existing verification/lobby routing; never teleport here.
        recover(e.getPlayer());
        for (var entry : sessions.entrySet()) {
            Player viewer = getServer().getPlayer(entry.getKey());
            if (viewer != null && entry.getValue().hidden) viewer.hidePlayer(this, e.getPlayer());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (sessions.containsKey(p.getUniqueId())) recover(p);
        sessions.remove(p.getUniqueId()); recovering.remove(p.getUniqueId()); startCooldown.remove(p.getUniqueId());
        p.removeMetadata(ACTIVE, this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        Player p = e.getPlayer(); Location to = e.getTo();
        if (to == null || recovering.contains(p.getUniqueId())) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null) {
            if (!e.hasChangedPosition()) return;
            for (Course c : courses.values()) if (Course.onBlock(to, c.start()) && to.clone().subtract(0, 0.2, 0).getBlock().getType().isSolid()) { start(p, c); break; }
            return;
        }
        if (to.getY() < s.course.fallY()) {
            p.setFallDistance(0); p.setVelocity(new Vector());
            e.setTo(s.course.checkpoint(s.progress.checkpoint())); return;
        }
        int next = s.progress.checkpoint() + 1;
        if (next <= s.course.checkpoints().size() && Course.onBlock(to, s.course.checkpoints().get(next - 1)) && s.progress.touch(next)) {
            message(p, "Checkpoint " + next + "/" + s.course.checkpoints().size());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
        }
        if (Course.onBlock(to, s.course.finish()) && s.progress.canFinish()) {
            long elapsed = s.progress.elapsedMillis(System.nanoTime());
            // Avoid a nested teleport from PlayerMoveEvent; finish once on the next tick.
            if (s.finishing) return;
            s.finishing = true;
            getServer().getScheduler().runTask(this, () -> {
                if (!p.isOnline() || sessions.get(p.getUniqueId()) != s) return;
                if (leave(p, true)) {
                    message(p, "¡Completado! " + s.course.id() + " • " + RunProgress.format(elapsed));
                    getServer().getPluginManager().callEvent(new ParkourCompletedEvent(p, s.course.id(), elapsed));
                } else s.finishing = false;
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) public void interact(PlayerInteractEvent e) {
        Player p = e.getPlayer(); if (!active(p)) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null || e.getItem() == null || !e.getItem().hasItemMeta()) return;
        long now = System.nanoTime();
        if (s.finishing || now - s.lastTool < 300_000_000L) return;
        s.lastTool = now;
        String action = e.getItem().getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        switch (action) {
            case "checkpoint" -> returnToCheckpoint(p, s, false);
            case "restart" -> returnToCheckpoint(p, s, true);
            case "exit" -> leave(p, true);
            case "time" -> message(p, RunProgress.format(s.progress.elapsedMillis(now)));
            case "visibility" -> {
                s.hidden = !s.hidden;
                for (Player other : getServer().getOnlinePlayers()) {
                    if (other.equals(p)) continue;
                    if (s.hidden) p.hidePlayer(this, other); else p.showPlayer(this, other);
                }
                message(p, s.hidden ? "Jugadores ocultos durante el recorrido." : "Jugadores visibles.");
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void externalTeleport(PlayerTeleportEvent e) {
        if (active(e.getPlayer()) && !internalTeleports.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void changedWorld(PlayerChangedWorldEvent e) {
        if (active(e.getPlayer()) && !internalTeleports.contains(e.getPlayer().getUniqueId())) leave(e.getPlayer(), false);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void damage(EntityDamageEvent e) { if (e.getEntity() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void attack(EntityDamageByEntityEvent e) { if (e.getDamager() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void food(FoodLevelChangeEvent e) { if (e.getEntity() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void open(InventoryOpenEvent e) { if (e.getPlayer() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p && active(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void entityInteract(PlayerInteractEntityEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockBreak(BlockBreakEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockPlace(BlockPlaceEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void flight(PlayerToggleFlightEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void death(PlayerDeathEvent e) {
        if (!active(e.getEntity())) return;
        e.getDrops().clear(); e.setKeepInventory(true); e.setKeepLevel(true); e.setDroppedExp(0);
    }
    @EventHandler(priority = EventPriority.MONITOR) public void respawn(PlayerRespawnEvent e) {
        if (active(e.getPlayer())) getServer().getScheduler().runTask(this, () -> recover(e.getPlayer()));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void command(PlayerCommandPreprocessEvent e) {
        if (!active(e.getPlayer())) return;
        String input = e.getMessage().toLowerCase(Locale.ROOT).trim();
        if (!input.matches("/(?:csdmparkour:)?parkour(?:\\s+(?:salir|reiniciar|checkpoint))?")) {
            e.setCancelled(true); message(e.getPlayer(), "Usa /parkour salir antes de ejecutar otros comandos.");
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            message(sender, "Recorridos: " + String.join(", ", courses.keySet()) + ". Durante una partida: /parkour salir|reiniciar|checkpoint"); return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (List.of("salir", "reiniciar", "checkpoint").contains(action)) {
            if (!(sender instanceof Player p)) return true;
            Session s = sessions.get(p.getUniqueId());
            if (s == null) { message(p, "No estás en un parkour."); return true; }
            if (s.finishing) return true;
            if (action.equals("salir")) leave(p, true); else returnToCheckpoint(p, s, action.equals("reiniciar"));
            return true;
        }
        if (!sender.hasPermission("csdm.parkour.admin")) { message(sender, "No tienes permiso para configurar recorridos."); return true; }
        if (!sessions.isEmpty()) { message(sender, "Espera a que terminen las sesiones antes de editar o recargar recorridos."); return true; }
        if (action.equals("recargar")) { reloadConfig(); loadCourses(); message(sender, "Recorridos activos: " + courses.size()); return true; }
        if (!(sender instanceof Player p) || args.length < 2) { message(sender, "Desde el juego: /parkour crear|inicio|cp|meta|salida|caida|activar <id>"); return true; }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) { message(p, "ID: letras, números, guion y guion bajo; máximo 32."); return true; }
        String path = "courses." + id;
        if (action.equals("crear")) {
            if (getConfig().contains(path)) { message(p, "Ese recorrido ya existe."); return true; }
            getConfig().set(path + ".enabled", false); getConfig().set(path + ".checkpoint-count", 0); getConfig().set(path + ".fall-y", -10);
        } else {
            if (!getConfig().contains(path)) { message(p, "Primero usa /parkour crear " + id); return true; }
            Location at = p.getLocation();
            Location centered = new Location(p.getWorld(), at.getBlockX() + .5, at.getY(), at.getBlockZ() + .5, at.getYaw(), at.getPitch());
            switch (action) {
                case "inicio", "meta", "cp", "salida" -> {
                    if (!at.clone().subtract(0, .2, 0).getBlock().getType().isSolid() || Math.abs(at.getY() - Math.rint(at.getY())) > .05) {
                        message(p, "Párate sobre un bloque sólido completo para guardar este punto."); return true;
                    }
                    String key = switch (action) { case "inicio" -> "start"; case "meta" -> "finish"; case "salida" -> "exit"; default -> "checkpoints." + (getConfig().getInt(path + ".checkpoint-count") + 1); };
                    getConfig().set(path + "." + key, centered);
                    if (action.equals("cp")) getConfig().set(path + ".checkpoint-count", getConfig().getInt(path + ".checkpoint-count") + 1);
                    getConfig().set(path + ".enabled", false);
                }
                case "caida" -> {
                    if (args.length < 3) { message(p, "/parkour caida " + id + " <Y>"); return true; }
                    try {
                        double y = Double.parseDouble(args[2]);
                        if (!Double.isFinite(y)) throw new NumberFormatException();
                        getConfig().set(path + ".fall-y", y); getConfig().set(path + ".enabled", false);
                    } catch (NumberFormatException e) { message(p, "La cota debe ser un número finito."); return true; }
                }
                case "activar" -> getConfig().set(path + ".enabled", true);
                default -> { message(p, "Acción desconocida."); return true; }
            }
        }
        saveConfig(); loadCourses();
        message(p, "Guardado: " + id + (courses.containsKey(id) ? " (activo)." : " (inactivo; revisa sus puntos y /parkour activar)."));
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> values = new ArrayList<>(List.of("lista", "salir", "reiniciar", "checkpoint"));
        if (sender.hasPermission("csdm.parkour.admin")) values.addAll(List.of("crear", "inicio", "cp", "meta", "salida", "caida", "activar", "recargar"));
        if (args.length == 2) {
            var root = getConfig().getConfigurationSection("courses"); values = root == null ? List.of() : new ArrayList<>(root.getKeys(false));
        } else if (args.length != 1) return List.of();
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(prefix)).toList();
    }
}
