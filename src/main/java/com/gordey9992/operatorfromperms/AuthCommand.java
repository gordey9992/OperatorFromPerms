package com.gordey9992.operatorfromperms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuthCommand implements CommandExecutor {
    private final OperatorFromPerms plugin;
    private final AuthManager authManager;

    public AuthCommand(OperatorFromPerms plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("register") || 
            command.getName().equalsIgnoreCase("reg") ||
            command.getName().equalsIgnoreCase("регистрация") ||
            command.getName().equalsIgnoreCase("рег") ||
            command.getName().equalsIgnoreCase("регистр")) {
            
            if (args.length < 2) {
                player.sendMessage("§cИспользование: /" + label + " <пароль> <подтверждение>");
                return true;
            }
            
            String password = args[0];
            String confirm = args[1];
            return authManager.registerPlayer(player, password, confirm);
        }

        if (command.getName().equalsIgnoreCase("login") || 
            command.getName().equalsIgnoreCase("log") ||
            command.getName().equalsIgnoreCase("l") ||
            command.getName().equalsIgnoreCase("логин") ||
            command.getName().equalsIgnoreCase("л") ||
            command.getName().equalsIgnoreCase("auth")) {
            
            if (args.length < 1) {
                player.sendMessage("§cИспользование: /" + label + " <пароль>");
                return true;
            }
            
            String password = args[0];
            return authManager.loginPlayer(player, password);
        }

        if (command.getName().equalsIgnoreCase("remember") || 
            command.getName().equalsIgnoreCase("запомнить") ||
            command.getName().equalsIgnoreCase("rememberme")) {
            
            return authManager.rememberPlayer(player);
        }

        return false;
    }
}
