package me.crazyg.everything.blocklog;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.Permissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dispatches the WorldEdit-style selection commands (//wand, //pos1, //pos2,
 * //hpos1, //hpos2, //expand, //contract, //outset, //inset, //shift, //sel,
 * //desel, //size, //count). All operate on the built-in {@link SelectionManager}.
 */
public class SelectionCommand implements CommandExecutor, TabCompleter {

    private static final long COUNT_VOLUME_LIMIT = 1_000_000L;

    private final Everything plugin;
    private final SelectionManager selectionManager;

    public SelectionCommand(Everything plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            everything(sender, "This command can only be used by a player.", NamedTextColor.RED);
            return true;
        }
        if (!player.hasPermission(Permissions.BLOCKLOG_SELECT)) {
            everything(sender, "You do not have permission to use selection commands.", NamedTextColor.RED);
            return true;
        }

        String name = command.getName().replaceFirst("^/+", "").toLowerCase(Locale.ROOT);
        switch (name) {
            case "wand" -> selectionManager.toggleWand(player);
            case "pos1" -> setPos(player, Args.pos(args, player), true);
            case "pos2" -> setPos(player, Args.pos(args, player), false);
            case "hpos1" -> setPos(player, Args.hpos(player), true);
            case "hpos2" -> setPos(player, Args.hpos(player), false);
            case "sel", "desel" -> selectionManager.clear(player);
            case "size" -> printSize(player);
            case "expand" -> handleAmount(player, args, "expand");
            case "contract" -> handleAmount(player, args, "contract");
            case "shift" -> handleAmount(player, args, "shift");
            case "outset" -> handleAmount(player, args, "outset");
            case "inset" -> handleAmount(player, args, "inset");
            case "count" -> count(player, args);
            default -> everything(sender, "Unknown selection command.", NamedTextColor.RED);
        }
        return true;
    }

    // ---------------------------------------------------------
    // Pos
    // ---------------------------------------------------------

    private void setPos(Player player, int[] xyz, boolean first) {
        if (xyz == null) {
            everything(player, "Could not determine a position. "
                + (first ? "//pos1" : "//pos2") + " takes explicit [x y z] coordinates.",
                NamedTextColor.RED);
            return;
        }
        if (first) {
            selectionManager.setPos1(player, xyz[0], xyz[1], xyz[2]);
        } else {
            selectionManager.setPos2(player, xyz[0], xyz[1], xyz[2]);
        }
    }

    // ---------------------------------------------------------
    // Expand / contract / shift / outset / inset
    // ---------------------------------------------------------

    private void handleAmount(Player player, String[] args, String op) {
        if (args.length < 1) {
            usage(player, op);
            return;
        }

        if (op.equals("expand") && args[0].equalsIgnoreCase("vert")) {
            selectionManager.expandVert(player);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            everything(player, "Invalid amount: " + args[0], NamedTextColor.RED);
            return;
        }
        if (amount <= 0) {
            everything(player, "Amount must be a positive number.", NamedTextColor.RED);
            return;
        }

        BlockFace dir = null;
        if (args.length >= 2) {
            dir = parseDirection(player, args[1]);
            if (dir == null) {
                everything(player, "Unknown direction: " + args[1]
                    + " (up, down, north, south, east, west, forward, back, left, right).",
                    NamedTextColor.RED);
                return;
            }
        }

        switch (op) {
            case "expand" -> {
                if (dir == null) dir = player.getFacing();
                selectionManager.expand(player, amount, dir);
            }
            case "contract" -> {
                if (dir == null) dir = player.getFacing().getOppositeFace();
                selectionManager.contract(player, amount, dir);
            }
            case "shift" -> {
                if (dir == null) dir = player.getFacing();
                selectionManager.shift(player, amount, dir);
            }
            case "outset" -> selectionManager.outset(player, amount);
            case "inset" -> selectionManager.inset(player, amount);
            default -> {}
        }
    }

    // ---------------------------------------------------------
    // Size / count
    // ---------------------------------------------------------

    private void printSize(Player player) {
        SelectionManager.Selection sel = selectionManager.getSelection(player);
        if (sel == null) {
            everything(player, "Make a selection first (//wand, //pos1, //pos2).", NamedTextColor.YELLOW);
            return;
        }
        everything(player, Component.text("Selection ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(sel.getWidth() + "x" + sel.getHeight() + "x" + sel.getLength())
                .color(NamedTextColor.AQUA))
            .append(Component.text(" blocks (").color(NamedTextColor.GOLD))
            .append(Component.text("min (" + sel.minX() + ", " + sel.minY() + ", " + sel.minZ() + "), ")
                .color(NamedTextColor.WHITE))
            .append(Component.text("max (" + sel.maxX() + ", " + sel.maxY() + ", " + sel.maxZ() + "), ")
                .color(NamedTextColor.WHITE))
            .append(Component.text("volume " + sel.getVolume()).color(NamedTextColor.WHITE))
            .append(Component.text(").").color(NamedTextColor.GOLD)));
    }

    private void count(Player player, String[] args) {
        SelectionManager.Selection sel = selectionManager.getSelection(player);
        if (sel == null) {
            everything(player, "Make a selection first (//wand, //pos1, //pos2).", NamedTextColor.YELLOW);
            return;
        }
        if (args.length == 0) {
            everything(player, "Selection volume: " + sel.getWidth() + "x"
                + sel.getHeight() + "x" + sel.getLength() + " = " + sel.getVolume()
                + " blocks.");
            return;
        }

        Material mat = Material.matchMaterial(args[0].toUpperCase(Locale.ROOT));
        if (mat == null && !args[0].contains(":")) {
            mat = Material.matchMaterial("minecraft:" + args[0].toLowerCase(Locale.ROOT));
        }
        if (mat == null) {
            everything(player, "Unknown block: " + args[0], NamedTextColor.RED);
            return;
        }
        if (sel.getVolume() > COUNT_VOLUME_LIMIT) {
            everything(player, "Selection is too large to count precisely (over 1,000,000 blocks).",
                NamedTextColor.RED);
            return;
        }

        World world = sel.world();
        long count = 0;
        for (int x = sel.minX(); x <= sel.maxX(); x++) {
            for (int y = sel.minY(); y <= sel.maxY(); y++) {
                for (int z = sel.minZ(); z <= sel.maxZ(); z++) {
                    if (world.getBlockAt(x, y, z).getType() == mat) {
                        count++;
                    }
                }
            }
        }
        everything(player, Component.text("Counted ")
            .color(NamedTextColor.GREEN)
            .append(Component.text(count).color(NamedTextColor.AQUA))
            .append(Component.text(" " + mat.name().toLowerCase(Locale.ROOT) + ".")
                .color(NamedTextColor.GREEN)));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private BlockFace parseDirection(Player player, String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "up", "u" -> BlockFace.UP;
            case "down", "d" -> BlockFace.DOWN;
            case "north", "n" -> BlockFace.NORTH;
            case "south", "s" -> BlockFace.SOUTH;
            case "east", "e" -> BlockFace.EAST;
            case "west", "w" -> BlockFace.WEST;
            case "forward", "fwd", "f" -> player.getFacing();
            case "back", "backward", "b" -> player.getFacing().getOppositeFace();
            case "left", "l" -> rotate(player.getFacing(), true);
            case "right", "r" -> rotate(player.getFacing(), false);
            default -> null;
        };
    }

    private BlockFace rotate(BlockFace facing, boolean counterClockwise) {
        if (facing != BlockFace.NORTH && facing != BlockFace.SOUTH
            && facing != BlockFace.EAST && facing != BlockFace.WEST) {
            return facing;
        }
        if (counterClockwise) {
            return switch (facing) {
                case NORTH -> BlockFace.WEST;
                case WEST -> BlockFace.SOUTH;
                case SOUTH -> BlockFace.EAST;
                case EAST -> BlockFace.NORTH;
                default -> facing;
            };
        }
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> facing;
        };
    }

    private void usage(Player player, String op) {
        everything(player, "Usage: //" + op
            + (op.equals("count") ? " [block]" : " <amount>"
                + (op.equals("expand") ? " [direction|vert]" : " [direction]")));
    }

    private void everything(CommandSender sender, Component message) {
        Everything.sendFancy(sender, message);
    }

    private void everything(CommandSender sender, String message) {
        Everything.sendFancy(sender, Component.text(message));
    }

    private void everything(CommandSender sender, String message, NamedTextColor color) {
        Everything.sendFancy(sender, Component.text(message).color(color));
    }

    // ---------------------------------------------------------
    // Tab completion
    // ---------------------------------------------------------

    private static final Set<String> DIRECTIONS = Set.of(
        "up", "down", "north", "south", "east", "west",
        "forward", "back", "left", "right");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        String name = command.getName().replaceFirst("^/+", "").toLowerCase(Locale.ROOT);
        String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);

        if (name.equals("expand")) {
            if (args.length == 1) {
                return startsWith(List.of("10", "50", "100", "vert"), input);
            }
            if (args.length == 2) {
                return startsWith(new ArrayList<>(DIRECTIONS), input);
            }
        }
        if (name.equals("contract") || name.equals("shift")) {
            if (args.length == 1) {
                return startsWith(List.of("10", "50", "100"), input);
            }
            if (args.length == 2) {
                return startsWith(new ArrayList<>(DIRECTIONS), input);
            }
        }
        if (name.equals("outset") || name.equals("inset")) {
            if (args.length == 1) {
                return startsWith(List.of("10", "50", "100"), input);
            }
        }
        if (name.equals("count") && args.length == 1) {
            return materialSuggestions(input);
        }
        return List.of();
    }

    private List<String> startsWith(List<String> options, String input) {
        return options.stream()
            .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(input))
            .toList();
    }

    private List<String> materialSuggestions(String input) {
        List<String> result = new ArrayList<>();
        String needle = input.startsWith("minecraft:")
            ? input.substring(10) : input;
        for (Material mat : Material.values()) {
            String name = mat.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(needle)) {
                result.add(name);
                if (result.size() >= 50) break;
            }
        }
        return result;
    }

    /** Tries to resolve [x y z] coordinates; with no args uses the player's standing block. */
    private static final class Args {
        static int[] pos(String[] args, Player player) {
            if (args.length == 0) {
                Location loc = player.getLocation();
                return new int[] {loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()};
            }
            if (args.length >= 3) {
                try {
                    return new int[] {
                        Integer.parseInt(args[0]),
                        Integer.parseInt(args[1]),
                        Integer.parseInt(args[2])
                    };
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        static int[] hpos(Player player) {
            Block target = null;
            try {
                target = player.getTargetBlockExact(300);
            } catch (Exception ignored) {
            }
            if (target == null) {
                try {
                    target = player.getTargetBlock(null, 300);
                } catch (Exception ignored) {
                }
            }
            if (target == null) return null;
            return new int[] {target.getX(), target.getY(), target.getZ()};
        }
    }
}