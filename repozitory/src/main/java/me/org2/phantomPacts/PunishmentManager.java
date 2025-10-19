package me.org2.phantomPacts;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PunishmentManager {

    private static final Random random = new Random();

    public static void applyPunishments(Player player) {
        PhantomPacts plugin = PhantomPacts.getInstance();
        ConfigurationSection punishments = plugin.getConfig().getConfigurationSection("punishments");

        if (punishments == null) return;

        // Спавн фантомов
        if (punishments.getBoolean("spawn-phantoms.enabled", true)) {
            spawnPhantoms(player, punishments.getInt("spawn-phantoms.amount", 5));
        }

        // Потеря предметов
        if (punishments.getBoolean("drop-items.enabled", true)) {
            dropRandomItems(player, punishments.getInt("drop-items.percentage", 30));
        }

        // Негативные эффекты
        if (punishments.getBoolean("effects.enabled", true)) {
            applyEffects(player, punishments.getStringList("effects.effects-list"));
        }

        // Потеря опыта
        if (punishments.getBoolean("remove-exp.enabled", true)) {
            removeExperience(player, punishments.getInt("remove-exp.levels", 10));
        }

        // Временный бан
        if (punishments.getBoolean("temp-ban.enabled", false)) {
            tempBan(player, punishments.getLong("temp-ban.duration", 3600));
        }

        // Звуковой эффект
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 2.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Визуальные эффекты
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 50, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.SOUL, loc, 30, 1, 1, 1, 0.05);
    }

    // Спавн фантомов вокруг игрока
    private static void spawnPhantoms(Player player, int amount) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        for (int i = 0; i < amount; i++) {
            // Спавним фантомов в радиусе 10 блоков на высоте 20 блоков
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 5 + random.nextDouble() * 5;

            double x = loc.getX() + radius * Math.cos(angle);
            double y = loc.getY() + 20 + random.nextDouble() * 10;
            double z = loc.getZ() + radius * Math.sin(angle);

            Location spawnLoc = new Location(world, x, y, z);
            Phantom phantom = (Phantom) world.spawnEntity(spawnLoc, EntityType.PHANTOM);
            phantom.setSize(2 + random.nextInt(3)); // Размер 2-4
            phantom.setTarget(player);
            phantom.setCustomName(ChatColor.DARK_PURPLE + "Призрак Возмездия");
            phantom.setCustomNameVisible(true);
        }
    }

    // Потеря случайных предметов
    private static void dropRandomItems(Player player, int percentage) {
        ItemStack[] contents = player.getInventory().getContents();
        List<Integer> slots = new ArrayList<>();

        // Собираем слоты с предметами
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                slots.add(i);
            }
        }

        if (slots.isEmpty()) return;

        // Вычисляем количество предметов для дропа
        int itemsToDrop = Math.max(1, (slots.size() * percentage) / 100);

        Location dropLoc = player.getLocation();

        for (int i = 0; i < itemsToDrop && !slots.isEmpty(); i++) {
            int randomIndex = random.nextInt(slots.size());
            int slot = slots.remove(randomIndex);

            ItemStack item = contents[slot];
            if (item != null && item.getType() != Material.AIR) {
                player.getWorld().dropItemNaturally(dropLoc, item.clone());
                player.getInventory().setItem(slot, null);
            }
        }

        player.sendMessage(Utils.colorize("&c&lВаши предметы рассыпались из-за нарушения пакта!"));
    }

    // Применение негативных эффектов
    private static void applyEffects(Player player, List<String> effectsList) {
        for (String effectStr : effectsList) {
            try {
                String[] parts = effectStr.split(":");
                if (parts.length != 3) continue;

                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                if (type == null) continue;

                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);

                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            } catch (Exception e) {
                PhantomPacts.getInstance().getLogger().warning("Неверный формат эффекта: " + effectStr);
            }
        }
    }

    // Удаление опыта
    private static void removeExperience(Player player, int levels) {
        int currentLevel = player.getLevel();
        int newLevel = Math.max(0, currentLevel - levels);
        player.setLevel(newLevel);

        if (newLevel == 0) {
            player.setExp(0);
        }

        player.sendMessage(Utils.colorize("&c&lВы потеряли " + (currentLevel - newLevel) + " уровней опыта!"));
    }

    // Временный бан
    private static void tempBan(Player player, long durationSeconds) {
        String reason = ChatColor.DARK_PURPLE + "Нарушение Призрачного Пакта";
        long until = System.currentTimeMillis() + (durationSeconds * 1000);

        // Используем BanList для временного бана
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        banList.addBan(player.getName(), reason, new java.util.Date(until), "PhantomPacts");

        player.kickPlayer(ChatColor.RED + "Вы временно забанены за нарушение пакта!\n" +
                ChatColor.YELLOW + "Причина: " + reason + "\n" +
                ChatColor.YELLOW + "Разбан через: " + Utils.formatTime(durationSeconds));
    }
}