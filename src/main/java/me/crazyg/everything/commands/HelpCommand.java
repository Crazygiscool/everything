package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.gui.menu.DynamicMenuGUI;
import me.crazyg.everything.gui.help.*;
import me.crazyg.everything.utils.AdventureCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {

    private final HelpManager manager;

    public HelpCommand(Everything plugin) {
        this.manager = new HelpManager(plugin);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command cmd,
        String label,
        String[] args
    ) {
        if (!(sender instanceof Player p)) {
            AdventureCompat.sendMessage(sender,
                Component.text(
                    "This command can only be used by players!"
                ).color(NamedTextColor.RED)
            );
            return true;
        }

        Everything plugin = manager.getPlugin();
        String mode;
        if (plugin.getPlayerSettingsManager() != null) {
            mode = plugin.getPlayerSettingsManager().getHelpMode(p.getUniqueId());
        } else {
            mode = plugin.getConfig().getString("gui.help.mode", "everything");
        }
        if (mode == null) {
            mode = "everything";
        }

        switch (mode.toLowerCase()) {
            case "help":
                new HelpMainGUI(p, manager).open();
                return true;
            case "disabled":
                AdventureCompat.sendMessage(p,
                    Component.text("Help is disabled by your settings.")
                        .color(NamedTextColor.GRAY));
                return true;
            case "everything":
            default:
                new DynamicMenuGUI(p,
                    Everything.getInstance(),
                    Everything.getInstance().getConfig()
                        .getString("gui.primary-menu", "primary")).open();
                return true;
        }
    }
}
