package com.gordey9992.operatorfromperms;

import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OperatorListener implements Listener {

    private final OperatorFromPerms plugin;
    private final ConfigManager configManager;

    public OperatorListener(OperatorFromPerms plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        registerLuckPermsEvents();
    }

    private void registerLuckPermsEvents() {
        if (configManager.isCheckOnPermissionChange()) {
            EventBus eventBus = plugin.getLuckPerms().getEventBus();
            eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
            eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
            eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем авторизацию
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            player.sendMessage(configManager.getPlayerMessage("авторизация-требуется", "Для доступа к серверу требуется авторизация!"));
            player.sendMessage(configManager.getPlayerMessage("используйте-регистрацию", "Используйте: /регистрация <пароль> <подтверждение>"));
            player.sendMessage(configManager.getPlayerMessage("используйте-логин", "Используйте: /логин <пароль>"));
        } else {
            player.sendMessage(configManager.getPlayerMessage("доступ-разрешен", "Доступ разрешен! Приятной игры!"));
        }
        
        // Проверяем права оператора
        if (configManager.isCheckOnJoin()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.checkAndUpdateOperatorStatus(player);
            }, configManager.getCheckDelayTicks());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Можно добавить логику при выходе игрока
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (configManager.updateOnWorldChange()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.checkAndUpdateOperatorStatus(event.getPlayer());
            }, 5L);
        }
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.checkAndUpdateOperatorStatus(player);
            });
        }
    }

    private void onNodeAdd(NodeAddEvent event) {
        checkNodeChange(event.getNode().getKey());
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        checkNodeChange(event.getNode().getKey());
    }

    private void checkNodeChange(String nodeKey) {
        if (nodeKey.equals(configManager.getOperatorPermission())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.checkAndUpdateOperatorStatus(player);
                }
            });
        }
    }
}
