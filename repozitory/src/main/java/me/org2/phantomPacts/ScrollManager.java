package me.org2.phantomPacts;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ScrollManager {

    private final PhantomPacts plugin;
    private final Set<UUID> activeScrolls;
    private final Map<UUID, Long> scrollSpawnTimes;
    private BukkitTask spawnTask;

    public ScrollManager(PhantomPacts plugin) {
        this.plugin = plugin;
        this.activeScrolls = new HashSet<>();
        this.scrollSpawnTimes = new HashMap<>();
    }

    // Создание призрачного свитка (предмета)
    public ItemStack createScrollItem() {
        ItemStack scroll = new ItemStack(Material.PAPER);
        ItemMeta meta = scroll.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Призрачный Свиток");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_PURPLE + "Древний контракт духов");
            lore.add(ChatColor.GRAY + "Используйте для заключения пакта");
            lore.add("");
            lore.add(ChatColor.YELLOW + "/pact offer <игрок> <тип>");

            meta.setLore(lore);

            // Добавляем свечение
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            scroll.setItemMeta(meta);
        }

        return scroll;
    }

    // Спавн свитка в мире
    public void spawnScroll(Location location) {
        if (location == null || location.getWorld() == null) return;

        // Проверка максимального количества свитков
        int maxScrolls = plugin.getConfig().getInt("scrolls.max-scrolls", 10);
        if (activeScrolls.size() >= maxScrolls) {
            return;
        }

        World world = location.getWorld();

        // Создаём стойку для брони (невидимую) для отображения свитка
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Призрачный Свиток");
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setSmall(true);

        // Даём свиток в руку стойки
        stand.getEquipment().setHelmet(createScrollItem());

        UUID scrollId = stand.getUniqueId();
        activeScrolls.add(scrollId);
        scrollSpawnTimes.put(scrollId, System.currentTimeMillis());

        // Визуальные эффекты спавна
        spawnScrollEffects(location);

        // Удаление свитка через определённое время
        int lifetime = plugin.getConfig().getInt("scrolls.lifetime", 300);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (stand.isValid()) {
                despawnScroll(stand);
            }
        }, lifetime * 20L);

        // Анимация парения свитка
        startHoverAnimation(stand);
    }

    // Эффекты спавна свитка
    private void spawnScrollEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.PORTAL, location, 100, 0.5, 0.5, 0.5, 1);
        world.spawnParticle(Particle.ENCHANTMENT_TABLE, location, 50, 1, 1, 1, 1);
        world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }

    // Анимация парения
    private void startHoverAnimation(ArmorStand stand) {
        new BukkitRunnable() {
            double time = 0;

            @Override
            public void run() {
                if (!stand.isValid() || stand.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc = stand.getLocation();

                // Вращение
                loc.setYaw(loc.getYaw() + 2);

                // Парение вверх-вниз
                double offset = Math.sin(time) * 0.03;
                loc.add(0, offset, 0);

                stand.teleport(loc);

                // Частицы
                stand.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,
                        loc.clone().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);

                time += 0.1;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // Удаление свитка
    public void despawnScroll(ArmorStand stand) {
        UUID scrollId = stand.getUniqueId();
        activeScrolls.remove(scrollId);
        scrollSpawnTimes.remove(scrollId);

        Location loc = stand.getLocation();
        World world = loc.getWorld();

        if (world != null) {
            world.spawnParticle(Particle.SMOKE_LARGE, loc, 20, 0.3, 0.3, 0.3, 0.05);
            world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.5f, 1.5f);
        }

        stand.remove();
    }

    // Запуск задачи спавна свитков
    public void startScrollSpawnTask() {
        long interval = plugin.getConfig().getLong("scrolls.spawn-check-interval", 6000);

        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                attemptScrollSpawn();
            }
        }.runTaskTimer(plugin, 100L, interval);
    }

    // Попытка спавна свитка
    private void attemptScrollSpawn() {
        double chance = plugin.getConfig().getDouble("scrolls.spawn-chance", 0.3);

        if (!Utils.chance(chance)) {
            return;
        }

        List<String> allowedWorlds = plugin.getConfig().getStringList("scrolls.allowed-worlds");
        if (allowedWorlds.isEmpty()) {
            allowedWorlds = Arrays.asList("world");
        }

        // Выбираем случайный мир
        String worldName = allowedWorlds.get(new Random().nextInt(allowedWorlds.size()));
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return;
        }

        // Генерируем безопасную локацию
        Location spawnLoc = Utils.getRandomSafeLocation(world, 100, 1000);

        if (spawnLoc != null) {
            spawnScroll(spawnLoc);
            plugin.getLogger().info("Призрачный свиток появился в " + world.getName() +
                    " на координатах: " + spawnLoc.getBlockX() + ", " +
                    spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());
        }
    }

    // Остановка задачи спавна
    public void stopScrollSpawnTask() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }

        // Удаляем все активные свитки
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (activeScrolls.contains(stand.getUniqueId())) {
                    stand.remove();
                }
            }
        }

        activeScrolls.clear();
        scrollSpawnTimes.clear();
    }

    // Проверка, является ли объект призрачным свитком
    public boolean isPhantomScroll(ArmorStand stand) {
        return activeScrolls.contains(stand.getUniqueId());
    }

    // Получить количество активных свитков
    public int getActiveScrollCount() {
        return activeScrolls.size();
    }
}