package me.org2.phantomPacts;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

public class ScrollSpawnListener implements Listener {

    private final PhantomPacts plugin;

    public ScrollSpawnListener(PhantomPacts plugin) {
        this.plugin = plugin;
    }

    // Обработка взаимодействия со свитком
    @EventHandler
    public void onScrollPickup(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;

        ArmorStand stand = (ArmorStand) event.getRightClicked();
        Player player = event.getPlayer();

        // Проверяем, является ли это призрачным свитком
        if (!plugin.getScrollManager().isPhantomScroll(stand)) {
            return;
        }

        event.setCancelled(true);

        // Проверка прав
        if (!player.hasPermission("phantompacts.findscroll")) {
            player.sendMessage(Utils.colorize(plugin.getConfig().getString("messages.prefix") +
                    " &cУ вас нет прав подбирать призрачные свитки!"));
            return;
        }

        // Выдаём свиток игроку
        ItemStack scroll = plugin.getScrollManager().createScrollItem();
        player.getInventory().addItem(scroll);

        // Сообщение игроку
        player.sendMessage(Utils.colorize(plugin.getConfig().getString("messages.prefix") + " " +
                plugin.getConfig().getString("messages.scroll-found")));

        // Эффекты подбора
        Location loc = stand.getLocation();
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 50, 0.5, 0.5, 0.5, 1);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 0.3, 0.3, 0.3, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        // Удаляем свиток из мира
        plugin.getScrollManager().despawnScroll(stand);
    }
}