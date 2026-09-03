package tv.csdm.minecraft.community.staff;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.WeightNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class StaffRankService {
    private static final String TEAM_PREFIX = "csdm_t_";

    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile StaffRankRegistry registry;

    public StaffRankService(JavaPlugin plugin, LuckPerms luckPerms, StaffRankRegistry registry) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.registry = registry;
    }

    public void replaceRegistry(StaffRankRegistry registry) {
        this.registry = registry;
    }

    public List<StaffRankDefinition> ranks() {
        return registry.all().stream()
                .sorted(Comparator.comparingInt(StaffRankDefinition::priority).reversed())
                .toList();
    }

    public Optional<StaffRankDefinition> current(Player player) {
        return current(player, RankKind.FUNCTIONAL);
    }

    public Optional<StaffRankDefinition> current(Player player, RankKind kind) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        return user.getNodes(NodeType.INHERITANCE).stream()
                .map(InheritanceNode::getGroupName)
                .map(registry::byGroup)
                .flatMap(Optional::stream)
                .filter(rank -> rank.kind() == kind)
                .max(Comparator.comparingInt(StaffRankDefinition::priority));
    }

    public CompletableFuture<Boolean> assign(Player player, String rankId) {
        StaffRankDefinition rank = registry.find(rankId)
                .orElseThrow(() -> new IllegalArgumentException("Rango desconocido: " + rankId));
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        removeManagedGroups(user, rank.kind());
        user.data().add(InheritanceNode.builder(rank.group()).build());
        return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> true);
    }

    public CompletableFuture<Boolean> ensureDefaultFunctionalRank(Player player) {
        if (current(player, RankKind.FUNCTIONAL).isPresent()) {
            return CompletableFuture.completedFuture(false);
        }
        StaffRankDefinition userRank = registry.find("usuario")
                .orElseThrow(() -> new IllegalStateException("Falta el rango funcional usuario"));
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        user.data().add(InheritanceNode.builder(userRank.group()).build());
        return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> true);
    }

    public CompletableFuture<Boolean> clear(Player player) {
        return clear(player, RankKind.FUNCTIONAL);
    }

    public CompletableFuture<Boolean> clear(Player player, RankKind kind) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        boolean changed = removeManagedGroups(user, kind);
        if (!changed) {
            return CompletableFuture.completedFuture(false);
        }
        return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> true);
    }

    public void syncDisplay(Player player) {
        Component name = Component.empty();
        Optional<StaffRankDefinition> functional = current(player, RankKind.FUNCTIONAL);
        Optional<StaffRankDefinition> prestige = current(player, RankKind.PRESTIGE);
        if (functional.isPresent()) {
            name = name.append(miniMessage.deserialize(functional.get().prefix()));
        }
        if (prestige.isPresent()) {
            name = name.append(miniMessage.deserialize(prestige.get().prefix()));
        }
        name = name.append(Component.text(player.getName()));
        player.playerListName(name);
        player.displayName(name);
        syncNametag(player, functional, prestige);
    }

    public void removeDisplay(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            team.unregister();
        }
    }

    public void clearManagedNametags() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Set.copyOf(scoreboard.getTeams()).stream()
                .filter(team -> team.getName().startsWith(TEAM_PREFIX))
                .forEach(Team::unregister);
    }

    public void ensureManagedGroups() {
        for (StaffRankDefinition definition : registry.all()) {
            luckPerms.getGroupManager().createAndLoadGroup(definition.group())
                    .thenCompose(group -> configureGroup(group, definition))
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("No se pudo configurar el grupo "
                                + definition.group() + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }

    private CompletableFuture<Void> configureGroup(Group group, StaffRankDefinition definition) {
        for (PermissionNode existing : group.getNodes(NodeType.PERMISSION)) {
            if (existing.getKey().startsWith("csdm.")) {
                group.data().remove(existing);
            }
        }
        for (String permission : definition.permissions()) {
            group.data().add(PermissionNode.builder(permission).value(true).build());
        }
        for (InheritanceNode existing : group.getNodes(NodeType.INHERITANCE)) {
            if (registry.managedGroups().contains(existing.getGroupName())) {
                group.data().remove(existing);
            }
        }
        group.getNodes(NodeType.PREFIX).forEach(group.data()::remove);
        group.getNodes(NodeType.WEIGHT).forEach(group.data()::remove);
        for (String parent : definition.inherits()) {
            group.data().add(InheritanceNode.builder(parent).build());
        }
        String legacyPrefix = miniMessage.stripTags(definition.prefix()).trim();
        if (!legacyPrefix.isEmpty()) {
            group.data().add(PrefixNode.builder("[" + definition.displayName().toUpperCase() + "] ", definition.priority()).build());
        }
        group.data().add(WeightNode.builder(definition.priority()).build());
        return luckPerms.getGroupManager().saveGroup(group);
    }

    private boolean removeManagedGroups(User user, RankKind kind) {
        boolean changed = false;
        for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
            if (registry.managedGroups(kind).contains(node.getGroupName())) {
                user.data().remove(node);
                changed = true;
            }
        }
        return changed;
    }

    private void syncNametag(
            Player player,
            Optional<StaffRankDefinition> functional,
            Optional<StaffRankDefinition> prestige) {
        if (!plugin.getConfig().getBoolean("nametags.enabled", true)) {
            removeDisplay(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamName(player);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        String separator = plugin.getConfig().getString(
                "nametags.separator", "<dark_gray> • </dark_gray>");

        if (plugin.getConfig().getBoolean("nametags.show-functional", true) && functional.isPresent()) {
            team.prefix(miniMessage.deserialize(functional.get().nametagLabel() + separator));
        } else {
            team.prefix(Component.empty());
        }
        if (plugin.getConfig().getBoolean("nametags.show-prestige", true) && prestige.isPresent()) {
            team.suffix(miniMessage.deserialize(separator + prestige.get().nametagLabel()));
        } else {
            team.suffix(Component.empty());
        }

        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.addEntry(player.getName());
    }

    private String teamName(Player player) {
        String compactUuid = player.getUniqueId().toString().replace("-", "");
        return TEAM_PREFIX + compactUuid.substring(0, 8);
    }
}
