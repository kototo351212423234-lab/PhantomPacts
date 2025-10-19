package me.org2.phantomPacts;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class PactType {

    private static final Map<String, PactType> types = new HashMap<>();

    private final String id;
    private final String name;
    private final String description;
    private final long duration;
    private final boolean enabled;

    public PactType(String id, String name, String description, long duration, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Загрузка типов из конфига
    public static void loadFromConfig(ConfigurationSection config) {
        types.clear();

        if (config == null) {
            // Создаём типы по умолчанию
            registerDefault();
            return;
        }

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String name = section.getString("name", key);
            String description = section.getString("description", "");
            long duration = section.getLong("duration", 86400);
            boolean enabled = section.getBoolean("enabled", true);

            PactType type = new PactType(key, name, description, duration, enabled);
            types.put(key.toLowerCase(), type);
        }

        // Если типы не загрузились, создаём дефолтные
        if (types.isEmpty()) {
            registerDefault();
        }
    }

    // Регистрация типов по умолчанию
    private static void registerDefault() {
        types.put("peace", new PactType("peace", "Пакт Мира", "Не атаковать друг друга", 86400, true));
        types.put("alliance", new PactType("alliance", "Пакт Альянса", "Помогать друг другу в бою", 43200, true));
        types.put("trade", new PactType("trade", "Торговый Пакт", "Делиться добычей 50/50", 21600, true));
        types.put("protection", new PactType("protection", "Пакт Защиты", "Защищать территорию союзника", 172800, true));
    }

    // Получить тип по ID
    public static PactType getType(String id) {
        return types.get(id.toLowerCase());
    }

    // Получить все типы
    public static Map<String, PactType> getAllTypes() {
        return new HashMap<>(types);
    }

    // Получить все активные типы
    public static Map<String, PactType> getEnabledTypes() {
        Map<String, PactType> enabled = new HashMap<>();
        for (Map.Entry<String, PactType> entry : types.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabled.put(entry.getKey(), entry.getValue());
            }
        }
        return enabled;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s [%ds]", name, id, description, duration);
    }
}