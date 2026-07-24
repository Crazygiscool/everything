package me.crazyg.everything.commands;

import me.crazyg.everything.Everything;
import me.crazyg.everything.utils.AdventureCompat;
import me.crazyg.everything.utils.particle.ParticleEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {
    private final Everything plugin;

    public GodCommand(Everything plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (args.length == 0) {
                if (p.isInvulnerable()) {
                    p.setInvulnerable(false);
                    if (plugin.getParticleManager().isEnabled("god")) {
                        plugin.getParticleManager().playEffect(p, ParticleEffect.GOD_DISABLE);
                    }
                    AdventureCompat.sendMessage(p, Component.text("GOD MODE disabled")
                            .color(NamedTextColor.DARK_RED));
                } else {
                    p.setInvulnerable(true);
                    if (plugin.getParticleManager().isEnabled("god")) {
                        plugin.getParticleManager().playEffect(p, ParticleEffect.GOD_ENABLE);
                    }
                    AdventureCompat.sendMessage(p, Component.text("GOD MODE Enabled")
                            .color(NamedTextColor.GOLD));
                }
            } else {
                String playername = args[0];
                Player target = Bukkit.getServer().getPlayerExact(playername);

                if (target == null) {
                    AdventureCompat.sendMessage(p, Component.text("This Player is not online")
                            .color(NamedTextColor.RED));
                } else {
                    if (p.isInvulnerable()) {
                        target.setInvulnerable(false);
                        if (plugin.getParticleManager().isEnabled("god")) {
                            plugin.getParticleManager().playEffect(target, ParticleEffect.GOD_DISABLE);
                        }
                        AdventureCompat.sendMessage(target, Component.text("")
                                .append(Component.text("GOD MODE disabled by ").color(NamedTextColor.DARK_RED))
                                .append(Component.text(p.getDisplayName()).color(NamedTextColor.DARK_RED)));
                        AdventureCompat.sendMessage(p, Component.text("MADE HIM NO LONGER GOD")
                                .color(NamedTextColor.BLUE));
                    } else {
                        target.setInvulnerable(true);
                        if (plugin.getParticleManager().isEnabled("god")) {
                            plugin.getParticleManager().playEffect(target, ParticleEffect.GOD_ENABLE);
                        }
                        AdventureCompat.sendMessage(target, Component.text("")
                                .append(Component.text("GOD MODE Enabled by ").color(NamedTextColor.GOLD))
                                .append(Component.text(p.getDisplayName()).color(NamedTextColor.GOLD)));
                        AdventureCompat.sendMessage(p, Component.text("MADE HIM GOD")
                                .color(NamedTextColor.BLUE));
                    }
                }
            }
        } else if (sender instanceof ConsoleCommandSender p) {
            AdventureCompat.sendMessage(p, Component.text("Command Cannot be runned by console, Silly")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        } else if (sender instanceof BlockCommandSender p) {
            AdventureCompat.sendMessage(p, Component.text("Command Cannot be runned by command block, L")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
        }
        return true;
    }
}