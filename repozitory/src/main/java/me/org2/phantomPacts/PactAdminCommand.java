package me.org2.phantomPacts;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PactAdminCommand implements CommandExecutor, TabCompleter {

    private final PhantomPacts plugin;

    public PactAdminCommand(PhantomPacts plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("phantompacts.admin")) {
            sender.sendMessage(Utils.colorize("&cУ вас нет прав на использование этой команды!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "force":
                handleForce(sender, args);
                break;

            case "clear":
                handleClear(sender, args);
                break;

            case "give":
                handleGive(sender, args);
                break;

            case "spawn":
                handleSpawn(sender, args);
                break;

            case "list":
                handleListAll(sender);
                break;

            case "info":
                handleAdminInfo(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    // Перезагрузка конфига
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        PactType.loadFromConfig(plugin.getConfig().getConfigurationSection("pact-types"));

        sender.sendMessage(Utils.colorize("&a&lPhantomPacts перезагружен!"));
    }

    // Принудительное создание пакта
    private void handleForce(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Utils.colorize("&cИспользование: /pactadmin force <игрок1> <игрок2> <тип>"));
            return;
        }

        Player p1 = Bukkit.getPlayerExact(args[1]);
        Player p2 = Bukkit.getPlayerExact(args[2]);

        if (p1 == null || p2 == null) {
            sender.sendMessage(Utils.colorize("&cОдин из игроков не найден!"));
            return;
        }

        PactType type = PactType.getType(args[3]);
        if (type == null) {
            sender.sendMessage(Utils.colorize("&cНеизвестный тип пакта!"));
            return;
        }

        plugin.getPactManager().createPact(p1.getUniqueId(), p2.getUniqueId(), type);
        sender.sendMessage(Utils.colorize("&aПакт &e" + type.getName() + " &aпринудительно создан между &e" +
                p1.getName() + " &aи &e" + p2.getName()));
    }

    // Очистка пактов
    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.colorize("&cИспользование: /pactadmin clear <игрок|all>"));
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            // Очистить все пакты
            plugin.getPactManager().saveAllPacts();
            sender.sendMessage(Utils.colorize("&aВсе пакты очищены!"));
        } else {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Utils.colorize("&cИгрок не найден!"));
                return;
            }

            List<Pact> pacts = plugin.getPactManager().getPlayerPacts(target.getUniqueId());
            for (Pact pact : pacts) {
                pact.setActive(false);
            }

            plugin.getPactManager().saveAllPacts();
            sender.sendMessage(Utils.colorize("&aПакты игрока &e" + target.getName() + " &aочищены! (" + pacts.size() + ")"));
        }
    }

    // Выдать свиток
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.colorize("&cИспользование: /pactadmin give <игрок> [количество]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.colorize("&cИгрок не найден!"));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Utils.colorize("&cНеверное количество!"));
                return;
            }
        }

        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(plugin.getScrollManager().createScrollItem());
        }

        sender.sendMessage(Utils.colorize("&aВыдано &e" + amount + " &aПризрачных Свитков игроку &e" + target.getName()));
        target.sendMessage(Utils.colorize("&aВы получили &e" + amount + " &aПризрачных Свитков!"));
    }

    // Заспавнить свиток в мире
    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize("&cЭту команду могут использовать только игроки!"));
            return;
        }

        Player player = (Player) sender;
        plugin.getScrollManager().spawnScroll(player.getLocation());
        sender.sendMessage(Utils.colorize("&aПризрачный Свиток заспавнен на вашей позиции!"));
    }

    // Список всех пактов
    private void handleListAll(CommandSender sender) {
        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(Utils.colorize("&5&lВсе Активные Пакты:"));
        sender.sendMessage("");

        // TODO: Реализовать получение всех пактов
        sender.sendMessage(Utils.colorize("&eФункция в разработке..."));

        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Информация о плагине
    private void handleAdminInfo(CommandSender sender) {
        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(Utils.colorize("&5&lPhantomPacts &7v1.0.0"));
        sender.sendMessage("");
        sender.sendMessage(Utils.colorize("  &7Активных свитков: &e" + plugin.getScrollManager().getActiveScrollCount()));
        sender.sendMessage(Utils.colorize("  &7PlaceholderAPI: &e" + (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null ? "✓" : "✗")));
        sender.sendMessage(Utils.colorize("  &7LuckPerms: &e" + (Bukkit.getPluginManager().getPlugin("LuckPerms") != null ? "✓" : "✗")));
        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // Помощь
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(Utils.colorize("&5&lPhantomPacts &7- Админ Команды"));
        sender.sendMessage("");
        sender.sendMessage(Utils.colorize("  &e/pactadmin reload &7- Перезагрузить конфиг"));
        sender.sendMessage(Utils.colorize("  &e/pactadmin force <p1> <p2> <тип> &7- Создать пакт"));
        sender.sendMessage(Utils.colorize("  &e/pactadmin clear <игрок|all> &7- Очистить пакты"));
        sender.sendMessage(Utils.colorize("  &e/pactadmin give <игрок> [кол-во] &7- Выдать свиток"));
        sender.sendMessage(Utils.colorize("  &e/pactadmin spawn &7- Заспавнить свиток"));
        sender.sendMessage(Utils.colorize("  &e/pactadmin info &7- Информация о плагине"));
        sender.sendMessage(Utils.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "force", "clear", "give", "spawn", "list", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("clear")) {
                completions.add("all");
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("force")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("force")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("force")) {
            return new ArrayList<>(PactType.getAllTypes().keySet());
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}