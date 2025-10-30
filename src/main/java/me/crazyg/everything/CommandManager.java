package me.crazyg.everything;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;


public class CommandManager implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> commands = new HashMap<>();
    private final Map<String, TabCompleter> tabCompleters = new HashMap<>();

    public void registerCommand(String name, CommandExecutor executor) {
        commands.put(name.toLowerCase(), executor);
        if (executor instanceof TabCompleter tabCompleter) {
            tabCompleters.put(name.toLowerCase(), tabCompleter);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandExecutor executor = commands.get(command.getName().toLowerCase());
        if (executor != null) {
            return executor.onCommand(sender, command, label, args);
        }
        sender.sendMessage("Unknown command: " + command.getName());
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        TabCompleter completer = tabCompleters.get(command.getName().toLowerCase());
        if (completer != null) {
            return completer.onTabComplete(sender, command, alias, args);
        }
        return null;
    }
}