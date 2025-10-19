package me.org2.phantomPacts;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LuckPermsIntegration {

    private static LuckPerms luckPerms;
    private static boolean enabled = false;

    // Инициализация LuckPerms
    public static void init() {
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                luckPerms = LuckPermsProvider.get();
                enabled = true;
                PhantomPacts.getInstance().getLogger().info("LuckPerms интеграция активирована!");
            }
        } catch (Exception e) {
            PhantomPacts.getInstance().getLogger().warning("Не удалось подключиться к LuckPerms: " + e.getMessage());
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // Выдача прав при заключении пакта
    public static void grantPactPermissions(UUID playerUuid) {
        if (!enabled || luckPerms == null) return;

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) return;

            // Добавляем права из конфига
            String group = PhantomPacts.getInstance().getConfig().getString("luckperms.pact-group", "pact_member");

            // Добавляем в группу
            Node groupNode = Node.builder("group." + group).build();
            user.data().add(groupNode);

            // Добавляем специальные права для пактов
            Node pactPermission = Node.builder("phantompacts.active").build();
            user.data().add(pactPermission);

            // Сохраняем изменения
            luckPerms.getUserManager().saveUser(user);

        } catch (Exception e) {
            PhantomPacts.getInstance().getLogger().warning("Ошибка при выдаче прав LuckPerms: " + e.getMessage());
        }
    }

    // Удаление прав при окончании/нарушении пакта
    public static void revokePactPermissions(UUID playerUuid) {
        if (!enabled || luckPerms == null) return;

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) return;

            String group = PhantomPacts.getInstance().getConfig().getString("luckperms.pact-group", "pact_member");

            // Удаляем из группы
            Node groupNode = Node.builder("group." + group).build();
            user.data().remove(groupNode);

            // Удаляем специальные права
            Node pactPermission = Node.builder("phantompacts.active").build();
            user.data().remove(pactPermission);

            // Сохраняем изменения
            luckPerms.getUserManager().saveUser(user);

        } catch (Exception e) {
            PhantomPacts.getInstance().getLogger().warning("Ошибка при удалении прав LuckPerms: " + e.getMessage());
        }
    }

    // Проверка, состоит ли игрок в группе пакта
    public static boolean hasActivePactGroup(UUID playerUuid) {
        if (!enabled || luckPerms == null) return false;

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) return false;

            String group = PhantomPacts.getInstance().getConfig().getString("luckperms.pact-group", "pact_member");
            return user.getInheritedGroups(user.getQueryOptions()).stream()
                    .anyMatch(g -> g.getName().equalsIgnoreCase(group));
        } catch (Exception e) {
            return false;
        }
    }

    // Временная выдача прав на определённый срок
    public static void grantTemporaryPermission(UUID playerUuid, String permission, long durationSeconds) {
        if (!enabled || luckPerms == null) return;

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) return;

            Node tempNode = Node.builder(permission)
                    .expiry(durationSeconds, TimeUnit.SECONDS)
                    .build();

            user.data().add(tempNode);
            luckPerms.getUserManager().saveUser(user);

        } catch (Exception e) {
            PhantomPacts.getInstance().getLogger().warning("Ошибка при выдаче временных прав: " + e.getMessage());
        }
    }
}