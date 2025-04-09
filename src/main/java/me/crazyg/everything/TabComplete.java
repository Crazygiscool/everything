package me.crazyg.everything; // Make sure this is in your correct package

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class TabComplete implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 'sender': Who is typing the command (Player, Console, Command Block).
        // 'command': The Command object itself (e.g., the /yourcommand Command instance).
        // 'alias': The command alias used (e.g., "yourcommand" or any aliases defined).
        // 'args': An array of strings representing what the player has typed so far, split by spaces.
        //         args[0] will be the first word after the command, args[1] the second, and so on.

        if (command.getName().equalsIgnoreCase("yourcommand")) { // Replace "yourcommand" with your actual command name
            if (args.length == 1) { // Tab completing the first argument
                List<String> completions = new ArrayList<>();
                completions.add("option1");
                completions.add("option2");
                completions.add("anotheroption");

                // Get online player names for completion as well (common use case)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }

                // Filter completions based on what the player is currently typing (args[0])
                if (!args[0].isEmpty()) {
                    completions.removeIf(completion -> !completion.toLowerCase().startsWith(args[0].toLowerCase()));
                }
                return completions;

            } else if (args.length == 2) { // Tab completing the second argument (example, depends on your command)
                if (args[0].equalsIgnoreCase("option1")) { // Example: If the first argument is "option1"
                    List<String> completions = new ArrayList<>();
                    completions.add("suboptionA");
                    completions.add("suboptionB");
                    // No need to filter much here in this example because often second args are more specific
                    if (!args[1].isEmpty()) {
                        completions.removeIf(completion -> !completion.toLowerCase().startsWith(args[1].toLowerCase()));
                    }
                    return completions;
                }
            }
        }

        return null; // No tab completions for this command or argument length
    }
}