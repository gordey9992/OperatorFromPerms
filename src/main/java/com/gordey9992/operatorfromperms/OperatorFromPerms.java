package com.gordey9992.operatorfromperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

public class OperatorFromPerms extends JavaPlugin {

    private ConfigManager configManager;
    private LuckPerms luckPerms;
    private Logger logger;
    private AuthManager authManager; // Добавляем поле

    @Override
    public void onEnable() {
        this.logger = getLogger();
        
        // Инициализируем менеджер конфигурации
        this.configManager = new ConfigManager(this);
        
        // Инициализируем менеджер авторизации
        this.authManager = new AuthManager(this);
        
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            logger.severe(configManager.getConsoleMessage("error-luckperms", 
                "LuckPerms not found! Plugin will be disabled."));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Регистрируем обработчик событий
        getServer().getPluginManager().registerEvents(new OperatorListener(this), this);
        
        // Регистрируем команды авторизации
        this.getCommand("register").setExecutor(new AuthCommand(this));
        this.getCommand("login").setExecutor(new AuthCommand(this));
        this.getCommand("remember").setExecutor(new AuthCommand(this));
        
        // Проверяем всех онлайн-игроков при включении плагина
        if (configManager.updateAllOnReload()) {
            checkAllOnlinePlayers();
        }
        
        String enabledMessage = configManager.getConsoleMessage("plugin-enabled", 
            "Plugin successfully enabled! Version: {version}")
            .replace("{version}", getDescription().getVersion());
        logger.info(enabledMessage);
    }

    @Override
    public void onDisable() {
        String disabledMessage = configManager.getConsoleMessage("plugin-disabled", 
            "Plugin disabled");
        logger.info(disabledMessage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getAdminMessage("command-usage", 
                "Use: /opf reload|check|version"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return reloadCommand(sender);
            case "check":
                return checkCommand(sender, args);
            case "version":
                return versionCommand(sender);
            default:
                sender.sendMessage(configManager.getAdminMessage("command-usage", 
                    "Use: /opf reload|check|version"));
                return true;
        }
    }

    private boolean reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("error-no-permission", 
                "No permission"));
            return true;
        }

        configManager.reloadConfigs();
        sender.sendMessage(configManager.getAdminMessage("reload-success", 
            "Configuration reloaded successfully"));
        return true;
    }

    private boolean checkCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("error-no-permission", 
                "No permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /opf check <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("Player not found or offline");
            return true;
        }

        boolean isOp = target.isOp();
        boolean hasPermission = target.hasPermission(configManager.getOperatorPermission());
        
        String statusMessage = configManager.getAdminMessage("status-check", 
            "Player {player}: OP={isOp}, Permission={hasPermission}")
            .replace("{player}", target.getName())
            .replace("{isOp}", String.valueOf(isOp))
            .replace("{hasPermission}", String.valueOf(hasPermission));
        
        sender.sendMessage(statusMessage);
        return true;
    }

    private boolean versionCommand(CommandSender sender) {
        sender.sendMessage("OperatorFromPerms v" + getDescription().getVersion() + 
            " by gordey25690 & DeepSeek");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "check", "version");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            return null;
        }
        return List.of();
    }

    public void checkAndUpdateOperatorStatus(Player player) {
        boolean hasPermission = player.hasPermission(configManager.getOperatorPermission());
        boolean isOp = player.isOp();
        
        if (hasPermission && !isOp) {
            player.setOp(true);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("op-granted", 
                    "Operator rights granted to: {player}")
                    .replace("{player}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("op-granted", 
                "You have been granted operator rights!"));
            
        } else if (!hasPermission && isOp) {
            player.setOp(false);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("op-removed", 
                    "Operator rights removed from: {player}")
                    .replace("{player}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("op-removed", 
                "Your operator rights have been removed"));
        }
    }

    private void checkAllOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            checkAndUpdateOperatorStatus(player);
        }
    }

    // ГЕТТЕРЫ
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    // ДОБАВЛЯЕМ ГЕТТЕР ДЛЯ AUTH MANAGER
    public AuthManager getAuthManager() {
        return authManager;
    }
}
