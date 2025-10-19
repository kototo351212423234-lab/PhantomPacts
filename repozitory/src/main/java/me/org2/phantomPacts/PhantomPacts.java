package me.org2.phantomPacts;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class PhantomPacts extends JavaPlugin {

    private static PhantomPacts instance;
    private PactManager pactManager;
    private ScrollManager scrollManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;

        // Сохраняем конфигурацию по умолчанию
        saveDefaultConfig();
        config = getConfig();

        // Инициализация менеджеров
        pactManager = new PactManager(this);
        scrollManager = new ScrollManager(this);

        // Регистрация команд
        getCommand("pact").setExecutor(new PactCommand(this));
        getCommand("pactadmin").setExecutor(new PactAdminCommand(this));

        // Регистрация событий
        getServer().getPluginManager().registerEvents(new PactListener(this), this);
        getServer().getPluginManager().registerEvents(new ScrollSpawnListener(this), this);

        // Запуск задачи спавна свитков
        scrollManager.startScrollSpawnTask();

        // Проверка PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PactPlaceholders(this).register();
            getLogger().info("PlaceholderAPI подключен!");
        }

        // Проверка LuckPerms
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            LuckPermsIntegration.init();
            getLogger().info("LuckPerms обнаружен!");
        }

        getLogger().info("PhantomPacts успешно запущен!");
    }

    @Override
    public void onDisable() {
        // Сохранение всех активных пактов
        if (pactManager != null) {
            pactManager.saveAllPacts();
        }

        // Остановка задач
        if (scrollManager != null) {
            scrollManager.stopScrollSpawnTask();
        }

        getLogger().info("PhantomPacts отключен!");
    }

    public static PhantomPacts getInstance() {
        return instance;
    }

    public PactManager getPactManager() {
        return pactManager;
    }

    public ScrollManager getScrollManager() {
        return scrollManager;
    }
}