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
            logger.severe(configManager.getConsoleMessage("ошибка-luckperms", 
                "&cLuckPerms не найден! Плагин будет отключен."));
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
            logger.warning(configManager.getConsoleMessage("предупреждение-экспериментальные", 
                "&6Экспериментальные функции включены! Используйте на свой страх и риск."));
        }
        
        String enabledMessage = configManager.getConsoleMessage("плагин-включен", 
            "&aПлагин успешно включен! Версия: {версия}")
            .replace("{версия}", getDescription().getVersion());
        logger.info(enabledMessage);
    }

    @Override
    public void onDisable() {
        String disabledMessage = configManager.getConsoleMessage("плагин-выключен", 
            "&cПлагин выключен");
        logger.info(disabledMessage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getAdminMessage("использование-команды", 
                "&eИспользуйте: /opf reload|check|version"));
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
                sender.sendMessage(configManager.getAdminMessage("использование-команды", 
                    "&eИспользуйте: /opf reload|check|version"));
                return true;
        }
    }

    private boolean reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("ошибка-нет-прав", 
                "&cНет разрешения"));
            return true;
        }

        configManager.reloadConfigs();
        sender.sendMessage(configManager.getAdminMessage("перезагрузка-успех", 
            "&aКонфигурация успешно перезагружена"));
        return true;
    }

    private boolean checkCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("operatorfromperms.admin")) {
            sender.sendMessage(configManager.getPlayerMessage("ошибка-нет-прав", 
                "&cНет разрешения"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("&cИспользование: /opf check <игрок>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("&cИгрок не найден или не в сети");
            return true;
        }

        boolean isOp = target.isOp();
        boolean hasPermission = target.hasPermission(configManager.getOperatorPermission());
        
        String statusMessage = configManager.getAdminMessage("проверка-статуса", 
            "&eСтатус игрока &6{игрок}&e: ОП={естьОП}, Разрешение={естьРазрешение}")
            .replace("{игрок}", target.getName())
            .replace("{естьОП}", String.valueOf(isOp))
            .replace("{естьРазрешение}", String.valueOf(hasPermission));
        
        sender.sendMessage(statusMessage);
        return true;
    }

    private boolean versionCommand(CommandSender sender) {
        sender.sendMessage("§eOperatorFromPerms v" + getDescription().getVersion() + 
            " от gordey25690 & DeepSeek");
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
                String warningMessage = configManager.getConsoleMessage("предупреждение-безопасный-режим", 
                    "&6Безопасный режим: заблокирована выдача ОП игроку &c{игрок}")
                    .replace("{игрок}", player.getName());
                logger.warning(warningMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("заблокированное-имя", 
                "&4Ваше имя заблокировано в безопасном режиме"));
            return;
        }

        boolean hasPermission = player.hasPermission(configManager.getOperatorPermission());
        boolean isOp = player.isOp();
        
        if (hasPermission && !isOp) {
            // Даем права оператора
            player.setOp(true);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("оп-выдано", 
                    "&aВыданы права оператора игроку: &6{игрок}")
                    .replace("{игрок}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("оп-выдано", 
                "&aВам были выданы права оператора через разрешение LuckPerms!"));
            
        } else if (!hasPermission && isOp) {
            // Забираем права оператора
            player.setOp(false);
            if (configManager.isLogToConsole()) {
                String logMessage = configManager.getConsoleMessage("оп-забрано", 
                    "&cЗабраны права оператора у игрока: &6{игрок}")
                    .replace("{игрок}", player.getName());
                logger.info(logMessage);
            }
            player.sendMessage(configManager.getPlayerMessage("оп-забрано", 
                "&cПрава оператора были забраны, так как у вас нет необходимого разрешения"));
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
