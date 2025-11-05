package com.gordey9992.operatorfromperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class OperatorFromPerms extends JavaPlugin {

    private static final String OPERATOR_PERMISSION = "operatorfromperms.all";
    private LuckPerms luckPerms;
    private Logger logger;

    @Override
    public void onEnable() {
        this.logger = getLogger();
        this.luckPerms = LuckPermsProvider.get();
        
        // Регистрируем обработчик событий
        getServer().getPluginManager().registerEvents(new OperatorListener(this), this);
        
        // Проверяем всех онлайн-игроков при включении плагина
        checkAllOnlinePlayers();
        
        logger.info("OperatorFromPerms успешно включен!");
        logger.info("Разрешение: " + OPERATOR_PERMISSION);
    }

    @Override
    public void onDisable() {
        logger.info("OperatorFromPerms выключен");
    }

    public void checkAndUpdateOperatorStatus(Player player) {
        boolean hasPermission = player.hasPermission(OPERATOR_PERMISSION);
        boolean isOp = player.isOp();
        
        if (hasPermission && !isOp) {
            // Даем права оператора
            player.setOp(true);
            logger.info("Выданы права оператора игроку: " + player.getName());
            
            // Отправляем сообщение игроку
            player.sendMessage("§aВам были выданы права оператора через разрешение LuckPerms!");
            
        } else if (!hasPermission && isOp) {
            // Забираем права оператора, если разрешение было удалено
            player.setOp(false);
            logger.info("Забраны права оператора у игрока: " + player.getName());
            
            // Отправляем сообщение игроку
            player.sendMessage("§cПрава оператора были забраны, так как у вас нет разрешения " + OPERATOR_PERMISSION);
        }
    }

    private void checkAllOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            checkAndUpdateOperatorStatus(player);
        }
    }

    public String getOperatorPermission() {
        return OPERATOR_PERMISSION;
    }
}
