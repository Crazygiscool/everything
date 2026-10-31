package me.crazyg.everything.listeners;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class SlashPrefixListener implements Listener {

    private static final Map<String, String> COMMANDS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        COMMANDS.put("wand", "wand");
        COMMANDS.put("pos1", "pos1");
        COMMANDS.put("pos2", "pos2");
        COMMANDS.put("hpos1", "hpos1");
        COMMANDS.put("hpos2", "hpos2");
        COMMANDS.put("expand", "expand");
        COMMANDS.put("contract", "contract");
        COMMANDS.put("outset", "outset");
        COMMANDS.put("inset", "inset");
        COMMANDS.put("shift", "shift");
        COMMANDS.put("sel", "sel");
        COMMANDS.put("desel", "sel");
        COMMANDS.put("size", "size");
        COMMANDS.put("count", "count");
    }

    private final me.crazyg.everything.Everything plugin;
    private final me.crazyg.everything.blocklog.SelectionCommand selectionCommand;

    public SlashPrefixListener(me.crazyg.everything.Everything plugin,
                               me.crazyg.everything.blocklog.SelectionCommand selectionCommand) {
        this.plugin = plugin;
        this.selectionCommand = selectionCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith("//")) {
            return;
        }

        String content = message.substring(2).trim();
        if (content.isEmpty()) {
            return;
        }

        String[] parts = content.split("\\s+", -1);
        String requested = parts[0].toLowerCase(Locale.ROOT);
        if (!COMMANDS.containsKey(requested)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("everything.blocklog.select")) {
            player.sendMessage("You do not have permission to use selection tools.");
            return;
        }

        String[] args = new String[Math.max(0, parts.length - 1)];
        System.arraycopy(parts, 1, args, 0, args.length);

        String commandName = requested.equals("desel") ? "sel" : requested;
        selectionCommand.onCommand(player, new Command(commandName) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return true;
            }
        }, commandName, args);
    }
}
