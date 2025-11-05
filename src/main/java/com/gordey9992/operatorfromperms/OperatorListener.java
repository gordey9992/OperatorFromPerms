package com.gordey9992.operatorfromperms;

import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OperatorListener implements Listener {

    private final OperatorFromPerms plugin;
    private final String operatorPermission;

    public OperatorListener(OperatorFromPerms plugin) {
        this.plugin = plugin;
        this.operatorPermission = plugin.getOperatorPermission();
        registerLuckPermsEvents();
    }

    private void registerLuckPermsEvents() {
        EventBus eventBus = plugin.luckPerms.getEventBus();
        
        // Слушаем изменения разрешений
        eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
        eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Проверяем статус оператора при входе игрока
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.checkAndUpdateOperatorStatus(player);
        }, 20L); // Задержка 1 секунда для загрузки разрешений
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Можно добавить логику при выходе игрока, если нужно
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        // При перерасчете данных пользователя проверяем статус оператора
        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.checkAndUpdateOperatorStatus(player);
            });
        }
    }

    private void onNodeAdd(NodeAddEvent event) {
        // При добавлении ноды проверяем, не наше ли это разрешение
        checkNodeChange(event.getNode(), event.getTarget().getType().name());
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        // При удалении ноды проверяем, не наше ли это разрешение
        checkNodeChange(event.getNode(), event.getTarget().getType().name());
    }

    private void checkNodeChange(Node node, String targetType) {
        if (node.getKey().equals(operatorPermission)) {
            // Нашли наше разрешение, находим игрока и обновляем статус
            String username = getUsernameFromTarget(targetType, node);
            if (username != null) {
                Player player = Bukkit.getPlayer(username);
                if (player != null && player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.checkAndUpdateOperatorStatus(player);
                    });
                }
            }
        }
    }

    private String getUsernameFromTarget(String targetType, Node node) {
        // Получаем имя пользователя из цели ноды
        // Это упрощенная реализация, в реальности нужно получить UUID из цели
        return null; // В реальной реализации нужно парсить targetType
    }
}
