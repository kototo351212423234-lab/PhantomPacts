package me.org2.phantomPacts;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PactListener implements Listener {

    private final PhantomPacts plugin;

    public PactListener(PhantomPacts plugin) {
        this.plugin = plugin;
    }

    // Обработка атаки между игроками
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Проверяем, есть ли пакт между игроками
        Pact pact = plugin.getPactManager().getPact(attacker.getUniqueId(), victim.getUniqueId());

        if (pact != null && pact.isActive() && !pact.isExpired()) {
            // Проверяем тип пакта
            String typeId = pact.getType().getId();

            // Пакт мира или альянса запрещает атаки
            if (typeId.equals("peace") || typeId.equals("alliance")) {
                event.setCancelled(true);

                attacker.sendMessage(Utils.colorize(plugin.getConfig().getString("messages.prefix") +
                        " &c&lВНИМАНИЕ! &cВы попытались атаковать союзника!"));
                attacker.sendMessage(Utils.colorize("&eЕсли вы действительно хотите нарушить пакт, используйте &c/pact break " + victim.getName()));

                // Предупреждение жертве
                victim.sendMessage(Utils.colorize(plugin.getConfig().getString("messages.prefix") +
                        " &e" + attacker.getName() + " &eпопытался вас атаковать, но пакт защитил вас!"));
            }
        }
    }

    // Обработка смерти игрока
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;

        // Проверяем пакт
        Pact pact = plugin.getPactManager().getPact(killer.getUniqueId(), victim.getUniqueId());

        if (pact != null && pact.isActive() && !pact.isExpired()) {
            String typeId = pact.getType().getId();

            // Убийство союзника - нарушение пакта
            if (typeId.equals("peace") || typeId.equals("alliance") || typeId.equals("protection")) {
                // Нарушаем пакт и применяем наказания
                plugin.getPactManager().breakPact(killer.getUniqueId(), victim.getUniqueId());
            }
        }
    }

    // Очистка данных при выходе игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Удаляем ожидающие предложения, отправленные этому игроку
        // (они автоматически истекают через 60 секунд)
    }
}