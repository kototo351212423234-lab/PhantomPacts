package me.org2.phantomPacts;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PactCommand implements CommandExecutor, TabCompleter {

    private final PhantomPacts plugin;

    public PactCommand(PhantomPacts plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize("&cЭту команду могут использовать только игроки!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("phantompacts.use")) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cУ вас нет прав на использование пактов!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "offer":
                handleOffer(player, args);
                break;

            case "accept":
                handleAccept(player);
                break;

            case "decline":
            case "deny":
                handleDecline(player);
                break;

            case "list":
                handleList(player);
                break;

            case "break":
            case "cancel":
                handleBreak(player, args);
                break;

            case "info":
                handleInfo(player, args);
                break;

            case "types":
                handleTypes(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    // Предложить пакт
    private void handleOffer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cИспользование: /pact offer <игрок> <тип>"));
            return;
        }

        // Проверка наличия свитка
        if (!hasPhantomScroll(player)) {
            player.sendMessage(Utils.colorize(getPrefix() + " " +
                    plugin.getConfig().getString("messages.no-scroll")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Utils.colorize(getPrefix() + " " +
                    plugin.getConfig().getString("messages.player-offline")));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cВы не можете заключить пакт с самим собой!"));
            return;
        }

        String typeId = args[2].toLowerCase();
        PactType type = PactType.getType(typeId);

        if (type == null || !type.isEnabled()) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cНеизвестный тип пакта! Используйте /pact types"));
            return;
        }

        // Проверка существующего пакта
        if (plugin.getPactManager().hasPact(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(Utils.colorize(getPrefix() + " " +
                    plugin.getConfig().getString("messages.already-pact")));
            return;
        }

        // Удаляем свиток
        removePhantomScroll(player);

        // Отправляем предложение
        plugin.getPactManager().offerPact(player, target, type);

        player.sendMessage(Utils.colorize(getPrefix() + " " +
                plugin.getConfig().getString("messages.pact-offered")
                        .replace("%player%", target.getName())
                        .replace("%type%", type.getName())));

        target.sendMessage(Utils.colorize(getPrefix() + " " +
                plugin.getConfig().getString("messages.pact-received")
                        .replace("%player%", player.getName())
                        .replace("%type%", type.getName())));
    }

    // Принять пакт
    private void handleAccept(Player player) {
        if (plugin.getPactManager().acceptPact(player)) {
            // Сообщение отправляется в PactManager
        } else {
            player.sendMessage(Utils.colorize(getPrefix() + " &cУ вас нет ожидающих предложений пакта!"));
        }
    }

    // Отклонить пакт
    private void handleDecline(Player player) {
        PactOffer offer = plugin.getPactManager().getPendingOffer(player.getUniqueId());

        if (offer == null) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cУ вас нет ожидающих предложений пакта!"));
            return;
        }

        if (plugin.getPactManager().declinePact(player)) {
            player.sendMessage(Utils.colorize(getPrefix() + " " +
                    plugin.getConfig().getString("messages.pact-declined")));

            Player sender = Bukkit.getPlayer(offer.getSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(Utils.colorize(getPrefix() + " &e" + player.getName() + " &cотклонил ваше предложение пакта"));
            }
        }
    }

    // Список активных пактов
    private void handleList(Player player) {
        List<Pact> pacts = plugin.getPactManager().getPlayerPacts(player.getUniqueId());

        if (pacts.isEmpty()) {
            player.sendMessage(Utils.colorize(getPrefix() + " &eУ вас нет активных пактов"));
            return;
        }

        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Utils.colorize("&5&lВаши Активные Пакты:"));
        player.sendMessage("");

        for (Pact pact : pacts) {
            String partner = pact.getPartnerName(player.getUniqueId());
            player.sendMessage(Utils.colorize("  &7● &e" + partner + " &7- &d" + pact.getType().getName()));
            player.sendMessage(Utils.colorize("    &8Осталось: &f" + pact.getFormattedRemainingTime()));
        }

        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Разорвать пакт
    private void handleBreak(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cИспользование: /pact break <игрок>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
        }

        Pact pact = plugin.getPactManager().getPact(player.getUniqueId(), targetUuid);

        if (pact == null || !pact.isActive()) {
            player.sendMessage(Utils.colorize(getPrefix() + " " +
                    plugin.getConfig().getString("messages.no-pact")));
            return;
        }

        player.sendMessage(Utils.colorize(getPrefix() + " &c&lВНИМАНИЕ! &cВы уверены, что хотите разорвать пакт?"));
        player.sendMessage(Utils.colorize("&cЭто вызовет наказание! Используйте &e/pact confirm &cдля подтверждения"));

        // TODO: Добавить систему подтверждения
    }

    // Информация о пакте
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cИспользование: /pact info <игрок>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid;
        String targetName = args[1];

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
        }

        Pact pact = plugin.getPactManager().getPact(player.getUniqueId(), targetUuid);

        if (pact == null || !pact.isActive()) {
            player.sendMessage(Utils.colorize(getPrefix() + " &cУ вас нет активного пакта с этим игроком"));
            return;
        }

        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Utils.colorize("&5&lИнформация о Пакте"));
        player.sendMessage("");
        player.sendMessage(Utils.colorize("  &7Партнёр: &e" + targetName));
        player.sendMessage(Utils.colorize("  &7Тип: &d" + pact.getType().getName()));
        player.sendMessage(Utils.colorize("  &7Описание: &f" + pact.getType().getDescription()));
        player.sendMessage(Utils.colorize("  &7Осталось времени: &a" + pact.getFormattedRemainingTime()));
        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Список типов пактов
    private void handleTypes(Player player) {
        Map<String, PactType> types = PactType.getEnabledTypes();

        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Utils.colorize("&5&lДоступные Типы Пактов:"));
        player.sendMessage("");

        for (PactType type : types.values()) {
            player.sendMessage(Utils.colorize("  &d" + type.getId() + " &7- &e" + type.getName()));
            player.sendMessage(Utils.colorize("    &f" + type.getDescription()));
            player.sendMessage(Utils.colorize("    &8Длительность: &7" + Utils.formatTime(type.getDuration())));
            player.sendMessage("");
        }

        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Помощь
    private void sendHelp(Player player) {
        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Utils.colorize("&5&lPhantomPacts &7- Помощь"));
        player.sendMessage("");
        player.sendMessage(Utils.colorize("  &e/pact offer <игрок> <тип> &7- Предложить пакт"));
        player.sendMessage(Utils.colorize("  &e/pact accept &7- Принять предложение"));
        player.sendMessage(Utils.colorize("  &e/pact decline &7- Отклонить предложение"));
        player.sendMessage(Utils.colorize("  &e/pact list &7- Список ваших пактов"));
        player.sendMessage(Utils.colorize("  &e/pact info <игрок> &7- Информация о пакте"));
        player.sendMessage(Utils.colorize("  &e/pact types &7- Доступные типы пактов"));
        player.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Проверка наличия свитка
    private boolean hasPhantomScroll(Player player) {
        return player.getInventory().contains(plugin.getScrollManager().createScrollItem().getType());
    }

    // Удаление свитка
    private void removePhantomScroll(Player player) {
        player.getInventory().removeItem(plugin.getScrollManager().createScrollItem());
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&5PhantomPacts&8]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("offer", "accept", "decline", "list", "info", "types", "break"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("offer") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("break")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("offer")) {
            return new ArrayList<>(PactType.getEnabledTypes().keySet());
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}