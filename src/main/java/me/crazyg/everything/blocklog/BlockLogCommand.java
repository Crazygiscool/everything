package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles /rollback, /lookup, /inspect and /lb.
 */
public class BlockLogCommand implements CommandExecutor, TabCompleter {

    private final Everything plugin;
    private final BlockLogDatabase database;
    private final RollbackManager rollbackManager;
    private final InspectWand inspectWand;

    public BlockLogCommand(Everything plugin, BlockLogDatabase database,
                           RollbackManager rollbackManager,
                           InspectWand inspectWand) {
        this.plugin = plugin;
        this.database = database;
        this.rollbackManager = rollbackManager;
        this.inspectWand = inspectWand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        String name = command.getName().toLowerCase();
        switch (name) {
            case "inspect":
                return handleInspect(sender, args);
            case "lb":
                return handleLb(sender, args);
            case "lookup":
                return handleLookup(sender, args);
            case "rollback":
                return handleRollback(sender, args);
            default:
                return false;
        }
    }

    // ---------------------------------------------------------
    // /inspect
    // ---------------------------------------------------------
    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            AdventureCompat.sendMessage(sender,
                Component.text("This command can only be used by a player.")
                    .color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("everything.blocklog.inspect")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to use the inspect wand.")
                    .color(NamedTextColor.RED));
            return true;
        }
        inspectWand.toggle(player);
        return true;
    }

    // ---------------------------------------------------------
    // /lb prune <days>
    // ---------------------------------------------------------
    private boolean handleLb(CommandSender sender, String[] args) {
        if (!sender.hasPermission("everything.blocklog.prune")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to manage block logs.")
                    .color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 1 || !args[0].equalsIgnoreCase("prune")) {
            AdventureCompat.sendMessage(sender,
                Component.text("Usage: /lb prune <days>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        if (args.length < 2) {
            AdventureCompat.sendMessage(sender,
                Component.text("Specify how many days of logs to keep.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        int days;
        try {
            days = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AdventureCompat.sendMessage(sender,
                Component.text("Invalid number of days.")
                    .color(NamedTextColor.RED));
            return true;
        }
        int removed = database.pruneOlderThan(days);
        AdventureCompat.sendMessage(sender,
            Component.text("Pruned " + removed + " log entries older than "
                + days + " days.").color(NamedTextColor.GREEN));
        return true;
    }

    // ---------------------------------------------------------
    // /lookup <player|*> [radius] [time]
    // /lookup here
    // ---------------------------------------------------------
    private boolean handleLookup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("everything.blocklog.lookup")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to lookup block changes.")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("here")) {
            if (!(sender instanceof Player player)) {
                AdventureCompat.sendMessage(sender,
                    Component.text("'here' can only be used by a player.")
                        .color(NamedTextColor.RED));
                return true;
            }
            Area area = resolveHereArea(player);
            List<BlockChange> changes = database.query(
                area.world, area.cx, area.cy, area.cz,
                area.radius, null, null);
            printLookup(sender, changes, "near you");
            return true;
        }

        QueryParams params = parseArgs(sender, args);
        if (params == null) return true;
        List<BlockChange> changes = database.query(
            params.world, params.cx, params.cy, params.cz,
            params.radius, params.uuid, params.since);
        printLookup(sender, changes, params.describe());
        return true;
    }

    // ---------------------------------------------------------
    // /rollback <player|*> [radius] [time] [-y]
    // /rollback here [-y]
    // ---------------------------------------------------------
    private boolean handleRollback(CommandSender sender, String[] args) {
        if (!sender.hasPermission("everything.blocklog.rollback")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to rollback blocks.")
                    .color(NamedTextColor.RED));
            return true;
        }

        boolean confirm = false;
        List<String> cleaned = new ArrayList<>();
        for (String a : args) {
            if (a.equals("-y") || a.equals("-yes")) {
                confirm = true;
            } else {
                cleaned.add(a);
            }
        }
        args = cleaned.toArray(new String[0]);

        if (args.length >= 1 && args[0].equalsIgnoreCase("here")) {
            if (!(sender instanceof Player player)) {
                AdventureCompat.sendMessage(sender,
                    Component.text("'here' can only be used by a player.")
                        .color(NamedTextColor.RED));
                return true;
            }
            Area area = resolveHereArea(player);
            List<BlockChange> changes = database.query(
                area.world, area.cx, area.cy, area.cz,
                area.radius, null, null);
            return doRollback(sender, changes, confirm);
        }

        QueryParams params = parseArgs(sender, args);
        if (params == null) return true;
        List<BlockChange> changes = database.query(
            params.world, params.cx, params.cy, params.cz,
            params.radius, params.uuid, params.since);
        return doRollback(sender, changes, confirm);
    }

    private boolean doRollback(CommandSender sender,
                               List<BlockChange> changes, boolean confirm) {
        int max = plugin.getConfig().getInt("blocklog.max-rollback-blocks", 10000);
        if (changes.isEmpty()) {
            AdventureCompat.sendMessage(sender,
                Component.text("No matching block changes to roll back.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        if (!confirm) {
            AdventureCompat.sendMessage(sender,
                Component.text("Found " + changes.size()
                    + " changes. Run again with -y to confirm rollback.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        int queued = rollbackManager.rollback(changes, max);
        AdventureCompat.sendMessage(sender,
            Component.text("Queued rollback of " + queued + " blocks...")
                .color(NamedTextColor.GREEN));
        return true;
    }

    // ---------------------------------------------------------
    // Lookup printing
    // ---------------------------------------------------------
    private void printLookup(CommandSender sender,
                             List<BlockChange> changes, String scope) {
        if (changes.isEmpty()) {
            AdventureCompat.sendMessage(sender,
                Component.text("No logged changes " + scope + ".")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        AdventureCompat.sendMessage(sender,
            Component.text("----- Block Log (" + changes.size()
                + " found " + scope + ") -----")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        int index = 0;
        LocalDateTime now = LocalDateTime.now();
        for (BlockChange c : changes) {
            String who = c.getPlayerName() == null ? "Natural" : c.getPlayerName();
            Component line = Component.text("")
                .append(Component.text("#" + index + " ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("[" + c.getAction().name() + "] ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(who).color(NamedTextColor.AQUA))
                .append(Component.text(" @ " + c.getX() + "," + c.getY()
                    + "," + c.getZ()).color(NamedTextColor.WHITE))
                .append(Component.text(" | " + c.getOldData().split(":")[0]
                    + " -> " + c.getNewData().split(":")[0])
                    .color(NamedTextColor.WHITE))
                .append(Component.text(" | " + c.getTimestamp()
                    .format(java.time.format.DateTimeFormatter
                        .ofPattern("MM-dd HH:mm"))).color(NamedTextColor.YELLOW));
            AdventureCompat.sendMessage(sender, line);
            index++;
            if (index >= 200) {
                AdventureCompat.sendMessage(sender,
                    Component.text("... and " + (changes.size() - index)
                        + " more (showing first 200).")
                        .color(NamedTextColor.GRAY));
                break;
            }
        }
    }

    // ---------------------------------------------------------
    // Argument parsing
    // ---------------------------------------------------------
    private static class QueryParams {
        World world;
        int cx, cy, cz;
        int radius = -1;
        UUID uuid = null;
        LocalDateTime since = null;
        String describe() {
            StringBuilder sb = new StringBuilder();
            if (uuid != null) sb.append("by ").append(uuid);
            if (radius >= 0) sb.append(" within ").append(radius).append(" blocks");
            if (since != null) sb.append(" since ").append(since);
            return sb.length() == 0 ? "" : sb.toString();
        }
    }

    private QueryParams parseArgs(CommandSender sender, String[] args) {
        QueryParams p = new QueryParams();
        if (args.length == 0) {
            AdventureCompat.sendMessage(sender,
                Component.text("Usage: /lookup|<rollback> <player|*> [radius] [time]")
                    .color(NamedTextColor.YELLOW));
            return null;
        }

        // First arg: player or *
        String target = args[0];
        if (!target.equals("*")) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(target);
            if (op.hasPlayedBefore() || op.isOnline()) {
                p.uuid = op.getUniqueId();
            } else {
                // tolerate online player name lookup
                Player online = Bukkit.getPlayerExact(target);
                if (online != null) {
                    p.uuid = online.getUniqueId();
                } else {
                    AdventureCompat.sendMessage(sender,
                        Component.text("Unknown player: " + target)
                            .color(NamedTextColor.RED));
                    return null;
                }
            }
        }

        // Center: executor location, or world spawn if console.
        if (sender instanceof Player player) {
            p.world = player.getWorld();
            p.cx = player.getLocation().getBlockX();
            p.cy = player.getLocation().getBlockY();
            p.cz = player.getLocation().getBlockZ();
        } else {
            p.world = Bukkit.getWorlds().get(0);
            p.cx = p.world.getSpawnLocation().getBlockX();
            p.cy = p.world.getSpawnLocation().getBlockY();
            p.cz = p.world.getSpawnLocation().getBlockZ();
        }

        // Remaining args: radius and/or time
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            Integer r = tryRadius(a);
            if (r != null) {
                p.radius = r;
                continue;
            }
            LocalDateTime since = tryTime(a);
            if (since != null) {
                p.since = since;
                continue;
            }
            AdventureCompat.sendMessage(sender,
                Component.text("Unrecognized argument: " + a
                    + " (expected radius like 20 or time like 1h/2d/10m)")
                    .color(NamedTextColor.RED));
            return null;
        }
        return p;
    }

    // ---------------------------------------------------------
    // "here" area resolution (uses the wand's configured inspect area)
    // ---------------------------------------------------------

    /** Resolved center + radius for the `here` subcommands. */
    private record Area(org.bukkit.World world, int cx, int cy, int cz,
                        int radius) {}

    private Area resolveHereArea(Player player) {
        InspectWand.InspectArea configured =
            inspectWand.getArea(player.getUniqueId());
        if (configured != null) {
            Location c = configured.center;
            int half = configured.size / 2;
            return new Area(c.getWorld(), c.getBlockX(), c.getBlockY(),
                c.getBlockZ(), half);
        }
        // Default: 10-block radius around the player.
        Location loc = player.getLocation();
        return new Area(player.getWorld(), loc.getBlockX(),
            loc.getBlockY(), loc.getBlockZ(), 10);
    }

    private Integer tryRadius(String s) {
        try {
            int v = Integer.parseInt(s);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime tryTime(String s) {
        // Formats: 10m, 2h, 5d, 30s
        if (s.length() < 2) return null;
        String numPart = s.substring(0, s.length() - 1);
        char unit = Character.toLowerCase(s.charAt(s.length() - 1));
        int amount;
        try {
            amount = Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return switch (unit) {
            case 's' -> now.minusSeconds(amount);
            case 'm' -> now.minusMinutes(amount);
            case 'h' -> now.minusHours(amount);
            case 'd' -> now.minusDays(amount);
            default -> null;
        };
    }

    // ---------------------------------------------------------
    // Tab completion
    // ---------------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("lb")) {
            if (args.length == 1) return List.of("prune");
            if (args.length == 2 && args[0].equalsIgnoreCase("prune")) {
                return List.of("7", "30", "90");
            }
            return List.of();
        }
        if (name.equals("inspect")) return List.of();

        // /lookup and /rollback share the player|* [radius] [time] shape
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("*"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                opts.add(p.getName());
            }
            String input = args[0].toLowerCase();
            return opts.stream()
                .filter(o -> o.toLowerCase().startsWith(input))
                .toList();
        }
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return List.of("10", "20", "50", "100").stream()
                .filter(o -> o.startsWith(input)).toList();
        }
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            return List.of("10m", "1h", "6h", "1d", "7d").stream()
                .filter(o -> o.startsWith(input)).toList();
        }
        return List.of();
    }
}
