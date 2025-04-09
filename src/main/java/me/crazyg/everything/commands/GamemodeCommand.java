package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    // Using constants for messages makes them easier to manage/change later
    private static final String NO_PERMISSION = ChatColor.RED + "You do not have permission to use this command.";
    private static final String INVALID_MODE = ChatColor.RED + "Invalid gamemode specified. Use 'c', 's', 'sp', or 'a'.";
    private static final String PLAYER_NOT_FOUND = ChatColor.RED + "Player not found.";
    private static final String CONSOLE_NEEDS_PLAYER = ChatColor.RED + "Console must specify a player name.";
    private static final String USAGE = ChatColor.RED + "Usage: /gm <c|s|sp|a> [player]";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // --- Basic Argument Checks ---
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(USAGE);
            return true; // Indicate command was handled (usage error)
        }

        // --- Determine Target Player ---
        Player targetPlayer;
        boolean targetingSelf = (args.length == 1);

        if (targetingSelf) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CONSOLE_NEEDS_PLAYER);
                return true; // Handled
            }
            targetPlayer = (Player) sender;
        } else { // Targeting another player (args.length == 2)
            targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(PLAYER_NOT_FOUND);
                return true; // Handled
            }
            // Check permission to target others
            if (!sender.hasPermission("everything.gamemode.others")) {
                sender.sendMessage(NO_PERMISSION + " (to change other players)");
                return true; // Handled (permission denied)
            }
        }

        // --- Determine Gamemode ---
        String modeArg = args[0].toLowerCase(); // Case-insensitive
        GameMode selectedMode;
        String modeName; // For user feedback messages
        String modePermission; // Specific permission for this mode

        switch (modeArg) {
            case "c":
            case "creative":
            case "1": // Optional: Allow numbers too
                selectedMode = GameMode.CREATIVE;
                modeName = "Creative";
                modePermission = "everything.gamemode.creative";
                break;
            case "s":
            case "survival":
            case "0": // Optional
                selectedMode = GameMode.SURVIVAL;
                modeName = "Survival";
                modePermission = "everything.gamemode.survival";
                break;
            case "sp":
            case "spectator":
            case "3": // Optional
                selectedMode = GameMode.SPECTATOR;
                modeName = "Spectator";
                modePermission = "everything.gamemode.spectator";
                break;
            case "a":
            case "adventure":
            case "2": // Optional
                selectedMode = GameMode.ADVENTURE;
                modeName = "Adventure";
                modePermission = "everything.gamemode.adventure";
                break;
            default:
                sender.sendMessage(INVALID_MODE);
                return true; // Handled
        }

        // --- Check Permissions for Specific Mode ---
        // Check base permission first (applies to self targeting)
        if (!sender.hasPermission(modePermission)) {
            sender.sendMessage(NO_PERMISSION + " (to set gamemode to " + modeName + ")");
            return true; // Handled (permission denied)
        }
        // (Permission for 'others' was checked earlier if args.length == 2)

        // --- Apply Gamemode ---
        targetPlayer.setGameMode(selectedMode);

        // --- Feedback Messages ---
        String feedbackColor = ChatColor.GREEN.toString(); // Use green for success
        String targetName = targetPlayer.getName();
        String senderName = (sender instanceof Player) ? ((Player)sender).getName() : "Console";

        // Message to the command sender
        if (targetingSelf) {
            sender.sendMessage(feedbackColor + "Your gamemode has been set to " + modeName + ".");
        } else {
            sender.sendMessage(feedbackColor + "Set " + targetName + "'s gamemode to " + modeName + ".");
        }

        // Message to the target player (if different from sender)
        if (!targetingSelf && targetPlayer != sender) { // Check if targetPlayer is actually different
            targetPlayer.sendMessage(feedbackColor + "Your gamemode has been set to " + modeName + " by " + senderName + ".");
        }

        return true; // Command successfully handled
    }
}