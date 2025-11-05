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

    @Override
    public void onEnable() {
        this.logger = getLogger();
        
        // Инициализируем менеджер конфигурации
        this.configManager = new ConfigManager(this);
        
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            logger.severe(configManager.getConsoleMessage("error-luckperms-not-found", 
                "&cLuckPerms not found! Plugin will be disabled."));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Регистрируем обработчик событий
        getServer().getPluginManager().registerEvents(new OperatorListener(this), this);
        
        // Проверяем всех онлайн-игроков при включении плагина
        if (configManager.updateAllOnReload()) {
            checkAllOnlinePlayers();
        }
        
        // Предупреждение о экспериментальных функциях
        if (configManager.isSyncOpWithPermission()) {
            logger.warning(configManager.getConsoleMessage("warning-experimental-enabled", 
                "&6Experimental features enabled! Use at your own risk."));
        }
        
        String enabledMessage = configManager.getConsoleMessage("plugin-enabled", 
            "&aPlugin successfully enabled! Version: {version}")
            .replace("{version}", getDescription().getVersion());
        logger.info(enabledMessage);
    }

    @Override
    public void onDisable() {
        String disabledMessage = configManager.getConsoleMessage("plugin-disabled", 
            "&cPlugin disabled");
        logger.info(disabledMessage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getAdminMessage("command-usage", 
                "&eUse: /opf reload|check|version"));
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
                    "&eUse: /opf reload|check|version"));
                return true;
        }
    }

    private boolean reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("error-no-permission", 
                "&cNo permission"));
            return true;
        }

        configManager.reloadConfigs();
        sender.sendMessage(configManager.getAdminMessage("reload-success", 
            "&aConfiguration reloaded successfully"));
        return true;
    }

    private boolean checkCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("error-no-permission", 
                "&cNo permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("&cUsage: /opf check <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("&cPlayer not found or offline");
            return true;
        }

        boolean isOp = target.isOp();
        boolean hasPermission = target.hasPermission(configManager.getOperatorPermission());
        
        String statusMessage = configManager.getAdminMessage("status-check", 
            "&ePlayer &6{player}&e: OP={isOp}, Permission={hasPermission}")
            .replace("{player}", target.getName())
            .replace("{isOp}", String.valueOf(isOp))
            .replace("{hasPermission}", String.valueOf(hasPermission));
        
        sender.sendMessage(statusMessage);
        return true;
    }

    private boolean versionCommand(CommandSender sender) {
        sender.sendMessage("§eOperatorFromPerms v" + getDescription().getVersion() + 
            " by gordey25690 & DeepSeek");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "check", "version");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            return null; // Предлагает список игроков
        }
        return List.of();
    }

    public void checkAndUpdateOperatorStatus(Player player) {
        // Проверка безопасного режима
        if (configManager.isSafeMode() && configManager.getBlockedUsernames().contains(player.getName())) {
            if (configManager.isLogToConsole()) {
                String warningMessage = configManager.getConsoleMessage("warning-safe-mode-blocked", 
                    "&6Safe mode blocked OP for player &c{player}")
                    .replace("{player}", player.getName());
                logger.warning(warningMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("blocked-username", 
                "&4Your username is blocked in safe mode"));
            return;
        }

        boolean hasPermission = player.hasPermission(configManager.getOperatorPermission());
        boolean isOp = player.isOp();
        
        if (hasPermission && !isOp) {
            // Даем права оператора
            player.setOp(true);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("op-granted", 
                    "&aOperator rights granted to: &6{player}")
                    .replace("{player}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("op-granted", 
                "&aYou have been granted operator rights!"));
            
        } else if (!hasPermission && isOp) {
            // Забираем права оператора
            player.setOp(false);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("op-removed", 
                    "&cOperator rights removed from: &6{player}")
                    .replace("{player}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("op-removed", 
                "&cYour operator rights have been removed"));
        }
    }

    private void checkAllOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            checkAndUpdateOperatorStatus(player);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
