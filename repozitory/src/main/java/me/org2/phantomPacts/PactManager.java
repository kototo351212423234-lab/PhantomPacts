package me.org2.phantomPacts;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PactManager {

    private final PhantomPacts plugin;
    private final Map<String, Pact> activePacts; // key: "uuid1-uuid2"
    private final Map<UUID, PactOffer> pendingOffers; // Ожидающие предложения
    private File pactsFile;
    private FileConfiguration pactsConfig;

    public PactManager(PhantomPacts plugin) {
        this.plugin = plugin;
        this.activePacts = new HashMap<>();
        this.pendingOffers = new HashMap<>();

        setupPactsFile();
        loadPacts();
        startExpirationChecker();

        // Загрузка типов пактов из конфига
        PactType.loadFromConfig(plugin.getConfig().getConfigurationSection("pact-types"));
    }

    private void setupPactsFile() {
        pactsFile = new File(plugin.getDataFolder(), "pacts.yml");
        if (!pactsFile.exists()) {
            try {
                pactsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл pacts.yml!");
                e.printStackTrace();
            }
        }
        pactsConfig = YamlConfiguration.loadConfiguration(pactsFile);
    }

    // Создание ключа для пакта
    private String createPactKey(UUID p1, UUID p2) {
        // Сортируем UUID для единообразия ключа
        String uuid1 = p1.toString();
        String uuid2 = p2.toString();
        return uuid1.compareTo(uuid2) < 0 ? uuid1 + "-" + uuid2 : uuid2 + "-" + uuid1;
    }

    // Предложить пакт
    public void offerPact(Player sender, Player target, PactType type) {
        PactOffer offer = new PactOffer(sender.getUniqueId(), target.getUniqueId(), type);
        pendingOffers.put(target.getUniqueId(), offer);

        // Автоматическое удаление предложения через 60 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingOffers.containsKey(target.getUniqueId())) {
                pendingOffers.remove(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage(Utils.colorize(plugin.getConfig().getString("messages.prefix") + " §cПредложение пакта истекло"));
                }
            }
        }, 1200L); // 60 секунд
    }

    // Принять пакт
    public boolean acceptPact(Player accepter) {
        PactOffer offer = pendingOffers.remove(accepter.getUniqueId());
        if (offer == null) return false;

        Player sender = Bukkit.getPlayer(offer.getSender());
        if (sender == null || !sender.isOnline()) return false;

        createPact(offer.getSender(), accepter.getUniqueId(), offer.getType());
        return true;
    }

    // Отклонить пакт
    public boolean declinePact(Player decliner) {
        PactOffer offer = pendingOffers.remove(decliner.getUniqueId());
        return offer != null;
    }

    // Создать пакт
    public void createPact(UUID player1, UUID player2, PactType type) {
        String key = createPactKey(player1, player2);
        Pact pact = new Pact(player1, player2, type, type.getDuration());
        activePacts.put(key, pact);

        // Уведомление игроков
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);

        String message = Utils.colorize(
                plugin.getConfig().getString("messages.pact-accepted")
                        .replace("%type%", type.getName())
                        .replace("%duration%", pact.getFormattedRemainingTime())
        );

        if (p1 != null) p1.sendMessage(message.replace("%player%", p2.getName()));
        if (p2 != null) p2.sendMessage(message.replace("%player%", p1.getName()));

        // Интеграция с LuckPerms
        if (plugin.getConfig().getBoolean("luckperms.grant-permissions", false)) {
            LuckPermsIntegration.grantPactPermissions(player1);
            LuckPermsIntegration.grantPactPermissions(player2);
        }

        savePacts();
    }

    // Проверка наличия активного пакта
    public boolean hasPact(UUID player1, UUID player2) {
        String key = createPactKey(player1, player2);
        Pact pact = activePacts.get(key);
        return pact != null && pact.isActive() && !pact.isExpired();
    }

    // Получить пакт между игроками
    public Pact getPact(UUID player1, UUID player2) {
        String key = createPactKey(player1, player2);
        return activePacts.get(key);
    }

    // Получить все пакты игрока
    public List<Pact> getPlayerPacts(UUID playerUuid) {
        return activePacts.values().stream()
                .filter(pact -> pact.involves(playerUuid) && pact.isActive() && !pact.isExpired())
                .collect(Collectors.toList());
    }

    // Нарушение пакта
    public void breakPact(UUID breaker, UUID victim) {
        Pact pact = getPact(breaker, victim);
        if (pact == null || !pact.isActive()) return;

        pact.setActive(false);

        // Применение наказаний
        Player breakerPlayer = Bukkit.getPlayer(breaker);
        if (breakerPlayer != null && !breakerPlayer.hasPermission("phantompacts.bypass")) {
            PunishmentManager.applyPunishments(breakerPlayer);
        }

        // Уведомление жертвы
        Player victimPlayer = Bukkit.getPlayer(victim);
        if (victimPlayer != null) {
            String message = Utils.colorize(
                    plugin.getConfig().getString("messages.pact-broken")
                            .replace("%player%", Bukkit.getOfflinePlayer(breaker).getName())
            );
            victimPlayer.sendMessage(message);
        }

        // Удаление прав LuckPerms
        if (plugin.getConfig().getBoolean("luckperms.grant-permissions", false)) {
            LuckPermsIntegration.revokePactPermissions(breaker);
            LuckPermsIntegration.revokePactPermissions(victim);
        }

        savePacts();
    }

    // Проверка истечения пактов
    private void startExpirationChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Pact> expiredPacts = activePacts.values().stream()
                        .filter(Pact::isExpired)
                        .collect(Collectors.toList());

                for (Pact pact : expiredPacts) {
                    pact.setActive(false);

                    Player p1 = Bukkit.getPlayer(pact.getPlayer1());
                    Player p2 = Bukkit.getPlayer(pact.getPlayer2());

                    String message = Utils.colorize(plugin.getConfig().getString("messages.pact-expired"));

                    if (p1 != null) p1.sendMessage(message.replace("%player%", pact.getPartnerName(pact.getPlayer1())));
                    if (p2 != null) p2.sendMessage(message.replace("%player%", pact.getPartnerName(pact.getPlayer2())));

                    // Удаление прав
                    if (plugin.getConfig().getBoolean("luckperms.grant-permissions", false)) {
                        LuckPermsIntegration.revokePactPermissions(pact.getPlayer1());
                        LuckPermsIntegration.revokePactPermissions(pact.getPlayer2());
                    }
                }

                if (!expiredPacts.isEmpty()) {
                    savePacts();
                }
            }
        }.runTaskTimer(plugin, 200L, 200L); // Проверка каждые 10 секунд
    }

    // Сохранение пактов
    public void savePacts() {
        pactsConfig.set("pacts", null);

        int index = 0;
        for (Pact pact : activePacts.values()) {
            String path = "pacts." + index;
            pactsConfig.set(path + ".player1", pact.getPlayer1().toString());
            pactsConfig.set(path + ".player2", pact.getPlayer2().toString());
            pactsConfig.set(path + ".type", pact.getType().getId());
            pactsConfig.set(path + ".created", pact.getCreatedAt());
            pactsConfig.set(path + ".expires", pact.getExpiresAt());
            pactsConfig.set(path + ".active", pact.isActive());
            index++;
        }

        try {
            pactsConfig.save(pactsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить пакты!");
            e.printStackTrace();
        }
    }

    // Загрузка пактов
    private void loadPacts() {
        if (!pactsConfig.contains("pacts")) return;

        for (String key : pactsConfig.getConfigurationSection("pacts").getKeys(false)) {
            String path = "pacts." + key;

            UUID p1 = UUID.fromString(pactsConfig.getString(path + ".player1"));
            UUID p2 = UUID.fromString(pactsConfig.getString(path + ".player2"));
            String typeId = pactsConfig.getString(path + ".type");
            long created = pactsConfig.getLong(path + ".created");
            long expires = pactsConfig.getLong(path + ".expires");
            boolean active = pactsConfig.getBoolean(path + ".active");

            PactType type = PactType.getType(typeId);
            if (type == null) continue;

            Pact pact = new Pact(p1, p2, type, created, expires, active);
            activePacts.put(createPactKey(p1, p2), pact);
        }

        plugin.getLogger().info("Загружено пактов: " + activePacts.size());
    }

    public void saveAllPacts() {
        savePacts();
    }

    public PactOffer getPendingOffer(UUID playerUuid) {
        return pendingOffers.get(playerUuid);
    }
}