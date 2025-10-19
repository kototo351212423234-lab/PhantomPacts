package me.org2.phantomPacts;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

public class Utils {

    private static final Random random = new Random();

    // Раскраска текста
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Форматирование времени из секунд
    public static String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("с");

        return sb.toString().trim();
    }

    // Генерация случайной безопасной локации для спавна свитка
    public static Location getRandomSafeLocation(World world, int minRadius, int maxRadius) {
        if (world == null) return null;

        // Получаем точку спавна мира
        Location spawnLoc = world.getSpawnLocation();

        for (int attempts = 0; attempts < 50; attempts++) {
            // Генерируем случайное расстояние и угол
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
            double angle = random.nextDouble() * 2 * Math.PI;

            int x = (int) (spawnLoc.getX() + distance * Math.cos(angle));
            int z = (int) (spawnLoc.getZ() + distance * Math.sin(angle));

            // Находим безопасную высоту
            Location loc = findSafeY(world, x, z);
            if (loc != null) {
                return loc;
            }
        }

        return null;
    }

    // Поиск безопасной Y координаты
    private static Location findSafeY(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Block below = world.getBlockAt(x, y - 1, z);
            Block above = world.getBlockAt(x, y + 1, z);

            // Проверяем, что блок ниже твёрдый, а текущий и выше - воздух
            if (isSolidAndSafe(below) &&
                    block.getType() == Material.AIR &&
                    above.getType() == Material.AIR) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    // Проверка, является ли блок твёрдым и безопасным
    private static boolean isSolidAndSafe(Block block) {
        Material type = block.getType();

        // Исключаем опасные блоки
        if (type == Material.LAVA || type == Material.MAGMA_BLOCK ||
                type == Material.FIRE || type == Material.SOUL_FIRE ||
                type == Material.CACTUS || type == Material.SWEET_BERRY_BUSH) {
            return false;
        }

        return block.getType().isSolid();
    }

    // Генерация случайного числа в диапазоне
    public static int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    // Генерация случайного double в диапазоне
    public static double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    // Проверка шанса (0.0 - 1.0)
    public static boolean chance(double probability) {
        return random.nextDouble() < probability;
    }
}