package com.gordey9992.operatorfromperms;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

public class AuthManager {
    private final OperatorFromPerms plugin;
    private final File authFile;
    private FileConfiguration authConfig;
    private final HashMap<UUID, String> sessions = new HashMap<>();
    
    public AuthManager(OperatorFromPerms plugin) {
        this.plugin = plugin;
        this.authFile = new File(plugin.getDataFolder(), "auth.yml");
        loadAuthData();
    }
    
    private void loadAuthData() {
        if (!authFile.exists()) {
            plugin.saveResource("auth.yml", false);
        }
        authConfig = YamlConfiguration.loadConfiguration(authFile);
    }
    
    public void saveAuthData() {
        try {
            authConfig.save(authFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить auth.yml: " + e.getMessage());
        }
    }
    
    // Хэширование пароля
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // fallback
        }
    }
    
    // Регистрация игрока
    public boolean registerPlayer(Player player, String password, String confirm) {
        UUID playerId = player.getUniqueId();
        String path = "players." + playerId.toString();
        
        // Проверка на уже зарегистрированного
        if (authConfig.contains(path)) {
            player.sendMessage(plugin.getConfigManager().getMessage("авторизация.регистрация.ошибка-уже-зарегистрирован", "&cВы уже зарегистрированы!"));
            return false;
        }
        
        // Проверка совпадения паролей
        if (!password.equals(confirm)) {
            player.sendMessage(plugin.getConfigManager().getMessage("авторизация.регистрация.ошибка-пароли-не-совпадают", "&cПароли не совпадают!"));
            return false;
        }
        
        // Проверка длины пароля
        int minLength = plugin.getConfigManager().getConfig().getInt("авторизация.минимальная-длина-пароля", 4);
        int maxLength = plugin.getConfigManager().getConfig().getInt("авторизация.максимальная-длина-пароля", 20);
        
        if (password.length() < minLength) {
            String message = plugin.getConfigManager().getMessage("авторизация.регистрация.ошибка-короткий-пароль", "&cПароль слишком короткий!")
                .replace("{мин}", String.valueOf(minLength));
            player.sendMessage(message);
            return false;
        }
        
        if (password.length() > maxLength) {
            String message = plugin.getConfigManager().getMessage("авторизация.регистрация.ошибка-длинный-пароль", "&cПароль слишком длинный!")
                .replace("{макс}", String.valueOf(maxLength));
            player.sendMessage(message);
            return false;
        }
        
        // Сохранение пароля
        if (plugin.getConfigManager().getConfig().getBoolean("авторизация.хэшировать-пароли", true)) {
            authConfig.set(path + ".password", hashPassword(password));
        } else {
            authConfig.set(path + ".password", password);
        }
        authConfig.set(path + ".registered", System.currentTimeMillis());
        authConfig.set(path + ".lastIP", player.getAddress().getAddress().getHostAddress());
        
        saveAuthData();
        
        player.sendMessage(plugin.getConfigManager().getMessage("авторизация.регистрация.успех", "&aРегистрация успешна!"));
        plugin.getLogger().info("Новый игрок зарегистрирован: " + player.getName());
        
        return true;
    }
    
    // Логин игрока
    public boolean loginPlayer(Player player, String password) {
        UUID playerId = player.getUniqueId();
        String path = "players." + playerId.toString();
        
        // Проверка на регистрацию
        if (!authConfig.contains(path)) {
            player.sendMessage(plugin.getConfigManager().getMessage("авторизация.логин.ошибка-не-зарегистрирован", "&cВы не зарегистрированы!"));
            return false;
        }
        
        String storedPassword = authConfig.getString(path + ".password");
        String inputPassword = plugin.getConfigManager().getConfig().getBoolean("авторизация.хэшировать-пароли", true) 
            ? hashPassword(password) : password;
        
        // Проверка пароля
        if (!storedPassword.equals(inputPassword)) {
            player.sendMessage(plugin.getConfigManager().getMessage("авторизация.логин.ошибка-неправильный-пароль", "&cНеправильный пароль!"));
            return false;
        }
        
        // Успешный логин
        sessions.put(playerId, player.getAddress().getAddress().getHostAddress());
        authConfig.set(path + ".lastLogin", System.currentTimeMillis());
        authConfig.set(path + ".lastIP", player.getAddress().getAddress().getHostAddress());
        saveAuthData();
        
        player.sendMessage(plugin.getConfigManager().getMessage("авторизация.логин.успех", "&aВход выполнен!"));
        return true;
    }
    
    // Запомнить меня
    public boolean rememberPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!isRegistered(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("авторизация.логин.ошибка-не-зарегистрирован", "&cВы не зарегистрированы!"));
            return false;
        }
        
        // Проверка IP если включено
        if (plugin.getConfigManager().getConfig().getBoolean("авторизация.проверять-ip", true)) {
            String storedIP = authConfig.getString("players." + playerId.toString() + ".lastIP");
            String currentIP = player.getAddress().getAddress().getHostAddress();
            
            if (!currentIP.equals(storedIP)) {
                player.sendMessage(plugin.getConfigManager().getMessage("авторизация.запомнить.ошибка-ip-изменился", "&eОбнаружен новый IP. Требуется вход."));
                return false;
            }
        }
        
        // Создание сессии на 24 часа
        long sessionTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        authConfig.set("players." + playerId.toString() + ".session", sessionTime);
        saveAuthData();
        
        sessions.put(playerId, player.getAddress().getAddress().getHostAddress());
        
        player.sendMessage(plugin.getConfigManager().getMessage("авторизация.запомнить.успех", "&bВы запомнены на 24 часа!"));
        return true;
    }
    
    // Проверка авторизации
    public boolean isAuthenticated(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Проверка активной сессии
        if (sessions.containsKey(playerId)) {
            return true;
        }
        
        // Проверка сессии "запомнить меня"
        String path = "players." + playerId.toString();
        if (authConfig.contains(path + ".session")) {
            long sessionTime = authConfig.getLong(path + ".session");
            if (sessionTime > System.currentTimeMillis()) {
                sessions.put(playerId, player.getAddress().getAddress().getHostAddress());
                return true;
            }
        }
        
        return false;
    }
    
    // Проверка регистрации
    public boolean isRegistered(Player player) {
        return authConfig.contains("players." + player.getUniqueId().toString());
    }
    
    // Выход
    public void logoutPlayer(Player player) {
        sessions.remove(player.getUniqueId());
    }
}
