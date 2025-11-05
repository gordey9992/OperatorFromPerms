package com.gordey9992.operatorfromperms;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadConfigs();
    }

    public void loadConfigs() {
        // Создаем папку плагина, если она не существует
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        loadConfig();
        loadMessages();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            logger.info("Создан новый config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Проверяем и обновляем конфиг если нужно
        updateConfig();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            logger.info("Создан новый messages.yml");
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Проверяем и обновляем сообщения если нужно
        updateMessages();
    }

    private void updateConfig() {
        // Здесь можно добавить логику обновления конфига при изменении версий
        try {
            InputStream defaultConfig = plugin.getResource("config.yml");
            if (defaultConfig != null) {
                YamlConfiguration defaultConfigYaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfig, StandardCharsets.UTF_8));
                
                boolean needsSave = false;
                for (String key : defaultConfigYaml.getKeys(true)) {
                    if (!config.contains(key)) {
                        config.set(key, defaultConfigYaml.get(key));
                        needsSave = true;
                    }
                }
                
                if (needsSave) {
                    saveConfig();
                    logger.info("Config.yml обновлен до новой версии");
                }
            }
        } catch (Exception e) {
            logger.warning("Ошибка при обновлении config.yml: " + e.getMessage());
        }
    }

    private void updateMessages() {
        // Аналогично для messages.yml
        try {
            InputStream defaultMessages = plugin.getResource("messages.yml");
            if (defaultMessages != null) {
                YamlConfiguration defaultMessagesYaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultMessages, StandardCharsets.UTF_8));
                
                boolean needsSave = false;
                for (String key : defaultMessagesYaml.getKeys(true)) {
                    if (!messages.contains(key)) {
                        messages.set(key, defaultMessagesYaml.get(key));
                        needsSave = true;
                    }
                }
                
                if (needsSave) {
                    saveMessages();
                    logger.info("Messages.yml обновлен до новой версии");
                }
            }
        } catch (Exception e) {
            logger.warning("Ошибка при обновлении messages.yml: " + e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            logger.severe("Не удалось сохранить config.yml: " + e.getMessage());
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (Exception e) {
            logger.severe("Не удалось сохранить messages.yml: " + e.getMessage());
        }
    }

    public void reloadConfigs() {
        loadConfigs();
        logger.info("Конфигурация перезагружена");
    }

    // Геттеры для config.yml
    public String getOperatorPermission() {
        return config.getString("settings.operator-permission", "operatorfromperms.all");
    }

    public boolean isCheckOnJoin() {
        return config.getBoolean("settings.check-on-join", true);
    }

    public boolean isCheckOnPermissionChange() {
        return config.getBoolean("settings.check-on-permission-change", true);
    }

    public int getCheckDelayTicks() {
        return config.getInt("settings.check-delay-ticks", 20);
    }

    public boolean isLogToConsole() {
        return config.getBoolean("settings.log-to-console", true);
    }

    public boolean isSafeMode() {
        return config.getBoolean("settings.safe-mode", true);
    }

    public List<String> getBlockedUsernames() {
        return config.getStringList("settings.blocked-usernames");
    }

    public boolean updateOnWorldChange() {
        return config.getBoolean("update-settings.update-on-world-change", true);
    }

    public boolean updateOnGroupChange() {
        return config.getBoolean("update-settings.update-on-group-change", true);
    }

    public boolean updateAllOnReload() {
        return config.getBoolean("update-settings.update-all-on-reload", true);
    }

    public boolean isSyncOpWithPermission() {
        return config.getBoolean("experimental.sync-op-with-permission", false);
    }

    public boolean isAutoFixDiscrepancies() {
        return config.getBoolean("experimental.auto-fix-discrepancies", true);
    }

    // Геттеры для messages.yml с поддержкой цветовых кодов
    public String getMessage(String path, String defaultValue) {
        String message = messages.getString(path, defaultValue);
        return message != null ? message.replace('&', '§') : defaultValue;
    }

    public String getPlayerMessage(String key, String defaultValue) {
        return getMessage("player-messages." + key, defaultValue);
    }

    public String getConsoleMessage(String key, String defaultValue) {
        return getMessage("console-messages." + key, defaultValue);
    }

    public String getAdminMessage(String key, String defaultValue) {
        return getMessage("admin-messages." + key, defaultValue);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
