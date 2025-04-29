package me.crazyg.everything.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    // Constants for messages using Adventure API
    private static final Component PLAYER_NOT_FOUND = Component.text("Player not found or not online.")
            .color(NamedTextColor.RED);
    private static final Component DISALLOWED_SENDER = Component.text("This command can only be executed by players.")
            .color(NamedTextColor.RED);
    private static final String GENERIC_USAGE = "Usage: /<command> [player]"; // Keep as String for replacement

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
                playerSender.sendMessage(Component.text("Unknown gamemode command executed.")
                        .color(NamedTextColor.RED));
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
                sender.sendMessage(Component.text("You do not have permission to set your gamemode to " + modeName)
                        .color(NamedTextColor.RED));
                return true;
            }
        } else { // Targeting another player (args.length == 1 expected now)
            if (args.length > 1) {
                sender.sendMessage(Component.text(GENERIC_USAGE.replace("<command>", commandName))
                        .color(NamedTextColor.RED));
                return true; // Too many arguments
            }

            targetPlayer = Bukkit.getPlayerExact(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(PLAYER_NOT_FOUND);
                return true;
            }

            // Check general 'others' permission AND specific mode permission
            if (!sender.hasPermission("everything.gamemode.others")) {
                sender.sendMessage(Component.text("You do not have permission to change other players' gamemodes")
                        .color(NamedTextColor.RED));
                return true;
            }
            // Also check if they have permission for the specific mode they are trying to set *for others*
            if (!sender.hasPermission(modePermission)) {
                sender.sendMessage(Component.text("You do not have permission to set other players' gamemode to " + modeName)
                        .color(NamedTextColor.RED));
                return true;
            }
        }

        // --- Apply Gamemode ---
        targetPlayer.setGameMode(targetMode);

        // --- Feedback Messages ---
        if (targetingSelf) {
            sender.sendMessage(Component.text("Your gamemode has been set to " + modeName + ".")
                    .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Set " + targetPlayer.getName() + "'s gamemode to " + modeName + ".")
                    .color(NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text("Your gamemode has been set to " + modeName + " by " + sender.getName() + ".")
                    .color(NamedTextColor.GREEN));
        }

        return true; // Command successfully handled
    }
}