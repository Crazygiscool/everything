package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
    private final LookupResultCache lookupCache = new LookupResultCache();

    private static final int PAGE_SIZE = 25;

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
    // /inspect [area <size>|clear]
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

        if (args.length == 0) {
            inspectWand.toggle(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "clear" -> {
                inspectWand.clearArea(player.getUniqueId());
                AdventureCompat.sendMessage(player,
                    Component.text("Inspect area cleared.")
                        .color(NamedTextColor.YELLOW));
            }
            case "area" -> {
                if (args.length < 2) {
                    AdventureCompat.sendMessage(player,
                        Component.text("Usage: /inspect area <size>")
                            .color(NamedTextColor.YELLOW));
                    return true;
                }
                int size;
                try {
                    size = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    AdventureCompat.sendMessage(player,
                        Component.text("Invalid size number.")
                            .color(NamedTextColor.RED));
                    return true;
                }
                size = Math.max(3, Math.min(100, size));
                inspectWand.setArea(player.getUniqueId(), player.getLocation(), size);
                AdventureCompat.sendMessage(player,
                    Component.text("Inspect area set to " + size + "x" + size + "x" + size
                        + " centered at your location.")
                        .color(NamedTextColor.GREEN));
            }
            default -> {
                AdventureCompat.sendMessage(player,
                    Component.text("Usage: /inspect [area <size>|clear]")
                        .color(NamedTextColor.YELLOW));
            }
        }
        return true;
    }

    // ---------------------------------------------------------
    // /lb prune <days> [world] [player] [blocktype]
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
                Component.text("Usage: /lb prune <days> [world] [player] [blocktype]")
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
        String world = null;
        UUID player = null;
        String blockType = null;
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if (Bukkit.getWorld(a) != null) {
                world = a;
                continue;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(a);
            if (op.hasPlayedBefore() || op.isOnline()) {
                player = op.getUniqueId();
                continue;
            }
            Player online = Bukkit.getPlayerExact(a);
            if (online != null) {
                player = online.getUniqueId();
                continue;
            }
            blockType = a;
        }
        int removed = database.pruneOlderThan(days, world, player, blockType);
        StringBuilder msg = new StringBuilder("Pruned " + removed
            + " log entries older than " + days + " days");
        boolean hasFilter = world != null || player != null || blockType != null;
        if (hasFilter) {
            msg.append(" (filters:");
            if (world != null) msg.append(" world=").append(world);
            if (player != null) msg.append(" player=").append(player);
            if (blockType != null) msg.append(" block=").append(blockType);
            msg.append(")");
        }
        msg.append(".");
        AdventureCompat.sendMessage(sender,
            Component.text(msg.toString()).color(NamedTextColor.GREEN));
        return true;
    }

    // ---------------------------------------------------------
    // /lookup
    // ---------------------------------------------------------
    private boolean handleLookup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("everything.blocklog.lookup")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to lookup block changes.")
                    .color(NamedTextColor.RED));
            return true;
        }

        int page = 1;
        String[] cmdArgs = args;
        if (args.length > 0) {
            Integer lastAsPage = tryRadius(args[args.length - 1]);
            if (lastAsPage != null && lastAsPage > 0
                && tryTime(args[args.length - 1]) == null) {
                page = lastAsPage;
                cmdArgs = Arrays.copyOf(args, args.length - 1);
            }
        }

        if (cmdArgs.length >= 1 && cmdArgs[0].equalsIgnoreCase("here")) {
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
            if (sender instanceof Player p) {
                lookupCache.store(p.getUniqueId(), changes, "near you");
            }
            printLookupPaginated(sender, changes, "near you", page);
            return true;
        }

        QueryParams params = parseArgs(sender, cmdArgs);
        if (params == null) return true;
        List<BlockChange> changes = database.query(
            params.world, params.cx, params.cy, params.cz,
            params.radius, params.uuid, params.since);

        changes = filterChanges(changes, params);

        if (sender instanceof Player p) {
            lookupCache.store(p.getUniqueId(), changes, params.describe());
        }
        printLookupPaginated(sender, changes, params.describe(), page);
        return true;
    }

    // ---------------------------------------------------------
    // /rollback
    // ---------------------------------------------------------
    private boolean handleRollback(CommandSender sender, String[] args) {
        if (!sender.hasPermission("everything.blocklog.rollback")) {
            AdventureCompat.sendMessage(sender,
                Component.text("You do not have permission to rollback blocks.")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("undo")) {
            if (!(sender instanceof Player player)) {
                AdventureCompat.sendMessage(sender,
                    Component.text("Undo can only be used by a player.")
                        .color(NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("everything.blocklog.rollback.undo")) {
                AdventureCompat.sendMessage(sender,
                    Component.text("You do not have permission to undo rollbacks.")
                        .color(NamedTextColor.RED));
                return true;
            }
            int undone = rollbackManager.undoLastRollback(player.getUniqueId());
            if (undone == 0) {
                AdventureCompat.sendMessage(sender,
                    Component.text("Nothing to undo (no recent rollback within 10 minutes).")
                        .color(NamedTextColor.YELLOW));
            } else {
                AdventureCompat.sendMessage(sender,
                    Component.text("Undid rollback of " + undone + " blocks.")
                        .color(NamedTextColor.GREEN));
            }
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

        changes = filterChanges(changes, params);

        return doRollback(sender, changes, confirm);
    }

    private List<BlockChange> filterChanges(List<BlockChange> changes, QueryParams params) {
        if (params.blockTypeFilter == null && params.actionFilter == null && params.excludePlayer == null) {
            return changes;
        }
        return changes.stream().filter(c -> {
            if (params.blockTypeFilter != null
                && !c.getBlockType().equalsIgnoreCase(params.blockTypeFilter)
                && !c.getNewMaterial().equalsIgnoreCase(params.blockTypeFilter)) {
                return false;
            }
            if (params.actionFilter != null && c.getAction() != params.actionFilter) {
                return false;
            }
            if (params.excludePlayer != null) {
                if (c.getPlayerName() != null && c.getPlayerName().equalsIgnoreCase(params.excludePlayer)) {
                    return false;
                }
                if (c.getBlockType().equalsIgnoreCase(params.excludePlayer)) {
                    return false;
                }
            }
            return true;
        }).toList();
    }

    private boolean doRollback(CommandSender sender,
                                List<BlockChange> changes, boolean confirm) {
        int max = plugin.getConfig().getInt("blocklog.max-rollback-blocks", 10000);
        int maxPerWorld = plugin.getConfig().getInt(
            "blocklog.max-rollback-blocks-per-world", 10000);
        if (changes.isEmpty()) {
            AdventureCompat.sendMessage(sender,
                Component.text("No matching block changes to roll back.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        if (!confirm) {
            Component msg = Component.text("Found " + changes.size()
                + " changes. Click to confirm rollback.")
                .color(NamedTextColor.YELLOW);
            if (sender instanceof Player player) {
                Component clickable = msg.clickEvent(
                    ClickEvent.runCommand("/rollback -y"));
                clickable = clickable.hoverEvent(
                    HoverEvent.showText(
                        Component.text("Click to confirm rollback of "
                            + changes.size() + " blocks")
                            .color(NamedTextColor.GOLD)));
                AdventureCompat.sendInteractiveMessage(player, clickable);
            } else {
                AdventureCompat.sendMessage(sender, msg);
                AdventureCompat.sendMessage(sender,
                    Component.text("Run again with -y to confirm.")
                        .color(NamedTextColor.GRAY));
            }
            return true;
        }
        int queued = rollbackManager.rollback(changes, max, maxPerWorld);
        AdventureCompat.sendMessage(sender,
            Component.text("Queued rollback of " + queued + " blocks...")
                .color(NamedTextColor.GREEN));
        return true;
    }

    // ---------------------------------------------------------
    // Lookup printing (paginated)
    // ---------------------------------------------------------
    private void printLookupPaginated(CommandSender sender,
                                       List<BlockChange> changes,
                                       String scope, int page) {
        if (changes.isEmpty()) {
            AdventureCompat.sendMessage(sender,
                Component.text("No logged changes " + scope + ".")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        int totalPages = (int) Math.ceil((double) changes.size() / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, changes.size());

        AdventureCompat.sendMessage(sender,
            Component.text("----- Block Log (" + changes.size()
                + " found " + scope + ") -----")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        LocalDateTime now = LocalDateTime.now();
        for (int i = start; i < end; i++) {
            BlockChange c = changes.get(i);
            String who = c.getPlayerName() == null ? "Natural" : c.getPlayerName();
            Component line = Component.text("")
                .append(Component.text("#" + (i + 1) + " ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("[" + c.getAction().name() + "] ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(who).color(NamedTextColor.AQUA))
                .append(Component.text(" @ " + c.getX() + "," + c.getY()
                    + "," + c.getZ()).color(NamedTextColor.WHITE))
                .append(Component.text(" | " + c.getOldMaterial()
                    + " -> " + c.getNewMaterial())
                    .color(NamedTextColor.WHITE))
                .append(Component.text(" | " + c.getTimestamp()
                    .format(java.time.format.DateTimeFormatter
                        .ofPattern("MM-dd HH:mm"))).color(NamedTextColor.YELLOW));
            AdventureCompat.sendMessage(sender, line);
        }

        if (totalPages > 1) {
            Component nav = Component.text("");
            if (page > 1) {
                nav = nav.append(
                    Component.text("<< Prev ")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand(
                            "/lookup " + scope + " " + (page - 1)))
                        .hoverEvent(HoverEvent.showText(
                            Component.text("Page " + (page - 1))
                                .color(NamedTextColor.GREEN))));
            }
            nav = nav.append(
                Component.text("[Page " + page + "/" + totalPages + "]")
                    .color(NamedTextColor.GOLD));
            if (page < totalPages) {
                nav = nav.append(
                    Component.text(" Next >>")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand(
                            "/lookup " + scope + " " + (page + 1)))
                        .hoverEvent(HoverEvent.showText(
                            Component.text("Page " + (page + 1))
                                .color(NamedTextColor.GREEN))));
            }
            if (sender instanceof Player player) {
                AdventureCompat.sendInteractiveMessage(player, nav);
            } else {
                AdventureCompat.sendMessage(sender, nav);
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
        String excludePlayer = null;
        String blockTypeFilter = null;
        BlockChange.Action actionFilter = null;

        String describe() {
            StringBuilder sb = new StringBuilder();
            if (uuid != null) sb.append("by player ");
            if (radius >= 0) sb.append(" within ").append(radius).append(" blocks");
            if (since != null) sb.append(" since time");
            if (blockTypeFilter != null) sb.append(" block=").append(blockTypeFilter);
            if (actionFilter != null) sb.append(" action=").append(actionFilter);
            if (excludePlayer != null) sb.append(" excluding ").append(excludePlayer);
            return sb.length() == 0 ? "near you" : sb.toString();
        }
    }

    private QueryParams parseArgs(CommandSender sender, String[] args) {
        QueryParams p = new QueryParams();

        WorldEditIntegration.SelectionBounds weSelection = null;
        if (sender instanceof Player player) {
            weSelection = WorldEditIntegration.getSelection(player);
        }
        if (weSelection != null) {
            p.world = weSelection.world();
            p.cx = weSelection.getCenterX();
            p.cy = weSelection.getCenterY();
            p.cz = weSelection.getCenterZ();
            p.radius = Math.max(weSelection.getRadiusX(),
                Math.max(weSelection.getRadiusY(), weSelection.getRadiusZ()));
        } else if (sender instanceof Player player) {
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

        boolean playerParsed = false;
        for (String a : args) {
            if (a.contains(":")) {
                int idx = a.indexOf(':');
                String key = a.substring(0, idx).toLowerCase();
                String val = a.substring(idx + 1);
                switch (key) {
                    case "player", "p" -> {
                        if (!val.equals("*")) {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(val);
                            if (op.hasPlayedBefore() || op.isOnline()) {
                                p.uuid = op.getUniqueId();
                            } else {
                                Player online = Bukkit.getPlayerExact(val);
                                if (online != null) p.uuid = online.getUniqueId();
                            }
                        }
                    }
                    case "radius", "r" -> {
                        Integer r = tryRadius(val);
                        if (r != null) p.radius = r;
                    }
                    case "time", "since", "t" -> {
                        LocalDateTime dt = tryTime(val);
                        if (dt != null) p.since = dt;
                    }
                    case "exclude", "ex", "e" -> {
                        p.excludePlayer = val;
                    }
                    case "action", "a" -> {
                        p.actionFilter = BlockChange.Action.fromString(val);
                    }
                    case "block", "b" -> {
                        p.blockTypeFilter = val.toUpperCase(java.util.Locale.ROOT);
                    }
                }
                continue;
            }

            if (!playerParsed && (a.equals("*") || Bukkit.getOfflinePlayer(a).hasPlayedBefore() || Bukkit.getOfflinePlayer(a).isOnline() || Bukkit.getPlayerExact(a) != null)) {
                if (!a.equals("*")) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(a);
                    if (op.hasPlayedBefore() || op.isOnline()) {
                        p.uuid = op.getUniqueId();
                    } else {
                        Player online = Bukkit.getPlayerExact(a);
                        if (online != null) p.uuid = online.getUniqueId();
                    }
                }
                playerParsed = true;
                continue;
            }

            Integer r = tryRadius(a);
            if (r != null && p.radius < 0) {
                p.radius = r;
                continue;
            }

            LocalDateTime dt = tryTime(a);
            if (dt != null && p.since == null) {
                p.since = dt;
                continue;
            }

            p.blockTypeFilter = a.toUpperCase(java.util.Locale.ROOT);
        }

        return p;
    }

    // ---------------------------------------------------------
    // "here" area resolution (uses the wand's configured inspect area)
    // ---------------------------------------------------------

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
        if (s.length() < 2) return null;
        LocalDateTime now = LocalDateTime.now();
        long totalSeconds = 0;
        int i = 0;
        while (i < s.length()) {
            int start = i;
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
            }
            if (i == start || i >= s.length()) return null;
            int amount;
            try {
                amount = Integer.parseInt(s.substring(start, i));
            } catch (NumberFormatException e) {
                return null;
            }
            char unit = Character.toLowerCase(s.charAt(i));
            i++;
            long secs = switch (unit) {
                case 's' -> amount;
                case 'm' -> amount * 60L;
                case 'h' -> amount * 3600L;
                case 'd' -> amount * 86400L;
                default -> -1;
            };
            if (secs < 0) return null;
            totalSeconds += secs;
        }
        return totalSeconds > 0 ? now.minusSeconds(totalSeconds) : null;
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
        if (name.equals("inspect")) {
            if (args.length == 1) return List.of("area", "clear");
            if (args.length == 2 && args[0].equalsIgnoreCase("area")) {
                return List.of("10", "20", "50", "100");
            }
            return List.of();
        }

        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("*", "here"));
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
            return List.of("10", "20", "50", "100", "radius:", "time:").stream()
                .filter(o -> o.startsWith(input)).toList();
        }
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            return List.of("10m", "1h", "6h", "1d", "7d", "exclude:").stream()
                .filter(o -> o.startsWith(input)).toList();
        }
        return List.of();
    }
}
