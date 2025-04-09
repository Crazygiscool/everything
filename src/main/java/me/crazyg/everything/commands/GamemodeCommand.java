package me.crazyg.everything.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender; // Import for clarity, although not strictly needed for instanceof
import org.bukkit.command.BlockCommandSender; // Import for clarity
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    // Constants for messages
    private static final String NO_PERMISSION = ChatColor.RED + "You do not have permission to use this command.";
    private static final String PLAYER_NOT_FOUND = ChatColor.RED + "Player not found or not online.";
    // private static final String CONSOLE_NEEDS_PLAYER = ChatColor.RED + "Console must specify a player name for this command."; // No longer needed with the new check
    private static final String DISALLOWED_SENDER = ChatColor.RED + "This command can only be executed by players.";
    private static final String GENERIC_USAGE = ChatColor.RED + "Usage: /<command> [player]"; // <command> will be replaced

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // --- Check if sender is a Player ---
        // If not a player (e.g., Console, CommandBlock), deny execution immediately.
        if (!(sender instanceof Player)) {
            sender.sendMessage(DISALLOWED_SENDER);
            return true; // Command handled (by denying it)
        }

        // --- Sender is definitely a Player now ---
        Player playerSender = (Player) sender; // Cast sender to Player for convenience

        GameMode targetMode;
        String modeName;
        String basePermission; // Permission needed specifically for this mode

        // Determine the gamemode based on the command name used
        switch (command.getName().toLowerCase()) {
            case "gmc":
                targetMode = GameMode.CREATIVE;
                modeName = "Creative";
                basePermission = "everything.gamemode.creative";
                break;
            case "gms":
                targetMode = GameMode.SURVIVAL;
                modeName = "Survival";
                basePermission = "everything.gamemode.survival";
                break;
            case "gmsp":
                targetMode = GameMode.SPECTATOR;
                modeName = "Spectator";
                basePermission = "everything.gamemode.spectator";
                break;
            case "gma":
                targetMode = GameMode.ADVENTURE;
                modeName = "Adventure";
                basePermission = "everything.gamemode.adventure";
                break;
            default:
                // Should not happen if plugin.yml is set up correctly
                playerSender.sendMessage(ChatColor.RED + "Unknown gamemode command executed.");
                return true;
        }

        // Now call the shared logic handler, passing the Player sender
        return handleGamemodeChange(playerSender, targetMode, modeName, basePermission, args, command.getName());
    }

    /**
     * Handles the common logic for setting gamemode for a specific command.
     * Assumes the 'sender' is a Player.
     */
    private boolean handleGamemodeChange(Player sender, GameMode targetMode, String modeName, String modePermission, String[] args, String commandName) {

        // --- Determine Target Player ---
        Player targetPlayer;
        boolean targetingSelf = (args.length == 0);

        if (targetingSelf) {
            // No need to check if sender is Player here, already confirmed
            targetPlayer = sender;
            // Check permission for self
            if (!sender.hasPermission(modePermission)) {
                sender.sendMessage(NO_PERMISSION + " (to set your gamemode to " + modeName + ")");
                return true;
            }
        } else { // Targeting another player (args.length == 1 expected now)
            if (args.length > 1) {
                sender.sendMessage(GENERIC_USAGE.replace("<command>", commandName));
                return true; // Too many arguments
            }

            targetPlayer = Bukkit.getPlayerExact(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(PLAYER_NOT_FOUND);
                return true;
            }

            // Check general 'others' permission AND specific mode permission
            if (!sender.hasPermission("everything.gamemode.others")) {
                sender.sendMessage(NO_PERMISSION + " (to change other players' gamemodes)");
                return true;
            }
            // Also check if they have permission for the specific mode they are trying to set *for others*
            if (!sender.hasPermission(modePermission)) {
                sender.sendMessage(NO_PERMISSION + " (to set other players' gamemode to " + modeName + ")");
                return true;
            }
        }

        // --- Apply Gamemode ---
        targetPlayer.setGameMode(targetMode);

        // --- Feedback Messages ---
        String feedbackColor = ChatColor.GREEN.toString();
        String targetName = targetPlayer.getName();
        // String senderName = sender.getName(); // Already know sender is a Player

        // Message to the command sender
        if (targetingSelf) {
            sender.sendMessage(feedbackColor + "Your gamemode has been set to " + modeName + ".");
        } else {
            sender.sendMessage(feedbackColor + "Set " + targetName + "'s gamemode to " + modeName + ".");
        }

        // Message to the target player (if different from sender)
        if (!targetingSelf) { // No need to check targetPlayer != sender, already done by targetingSelf logic
            targetPlayer.sendMessage(feedbackColor + "Your gamemode has been set to " + modeName + " by " + sender.getName() + ".");
        }

        return true; // Command successfully handled
    }
}