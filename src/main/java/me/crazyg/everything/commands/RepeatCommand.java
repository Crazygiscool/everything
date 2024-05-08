package me.crazyg.everything.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class RepeatCommand implements CommandExecutor {
    //long means the last time they have run the command
    private final HashMap<UUID, Long> cooldown;

    public RepeatCommand(){
        this.cooldown = new HashMap<>();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player p){
            //10seconds of cooldown for the command
            if (!this.cooldown.containsKey(p.getUniqueId()) || System.currentTimeMillis() - cooldown.get(p.getUniqueId())>= 10000){
                this.cooldown.put(p.getUniqueId(), System.currentTimeMillis());
                if(args.length == 0){

                    p.sendMessage("Wrong Usage, /<command> <word>");

                } else if (args.length == 1){

                    String word = args[0];

                    p.sendMessage("BOT: " + word);

                }else{

                    StringBuilder builder = new StringBuilder();

                    for (String arg : args) {

                        builder.append(arg);

                        builder.append(" ");

                    }

                    String finalmsg = builder.toString();
                    finalmsg.stripTrailing();

                    p.sendMessage("BOT: " + finalmsg);

                }

            }else{
                p.sendMessage("You can't execute this command for another "+ (10000-System.currentTimeMillis() - cooldown.get(p.getUniqueId()))+"ms");
            }
            }else if (sender instanceof ConsoleCommandSender p) {
                p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by console");
            } else if (sender instanceof BlockCommandSender p) {
                p.sendMessage(ChatColor.DARK_RED+""+ChatColor.BOLD+"Command Cannot be runned by command block");
            }
        return true;
    }
}
