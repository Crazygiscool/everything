package me.crazyg.everything.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RepeatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player p){

            if(args.length == 0){

                p.sendMessage("Wrong Usage, /<command> <word>");

            } else if (args.length == 1){

                String word = args[0];

                p.sendMessage("BOT: " + word);

            }else{

                StringBuilder builder = new StringBuilder();

                for(int i = 0; i < args.length; i++){

                    builder.append(args[i]);

                    builder.append(" ");

                }

                String finalmsg = builder.toString();
                finalmsg.stripTrailing();

                p.sendMessage("BOT: " + finalmsg);

            }

        }
        return true;
    }
}
