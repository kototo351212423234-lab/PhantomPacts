package me.org2.phantomPacts;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PactPlaceholders extends PlaceholderExpansion {

    private final PhantomPacts plugin;

    public PactPlaceholders(PhantomPacts plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "phantompacts";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "YourName";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %phantompacts_pacts_count% - количество активных пактов
        if (identifier.equals("pacts_count")) {
            List<Pact> pacts = plugin.getPactManager().getPlayerPacts(player.getUniqueId());
            return String.valueOf(pacts.size());
        }

        // %phantompacts_has_pact% - есть ли активные пакты (true/false)
        if (identifier.equals("has_pact")) {
            List<Pact> pacts = plugin.getPactManager().getPlayerPacts(player.getUniqueId());
            return String.valueOf(!pacts.isEmpty());
        }

        // %phantompacts_partners% - список партнёров
        if (identifier.equals("partners")) {
            List<Pact> pacts = plugin.getPactManager().getPlayerPacts(player.getUniqueId());
            if (pacts.isEmpty()) return "Нет";

            StringBuilder partners = new StringBuilder();
            for (int i = 0; i < pacts.size(); i++) {
                partners.append(pacts.get(i).getPartnerName(player.getUniqueId()));
                if (i < pacts.size() - 1) {
                    partners.append(", ");
                }
            }
            return partners.toString();
        }

        // %phantompacts_scrolls_active% - количество активных свитков в мире
        if (identifier.equals("scrolls_active")) {
            return String.valueOf(plugin.getScrollManager().getActiveScrollCount());
        }

        // %phantompacts_pact_with_<player>% - есть ли пакт с конкретным игроком
        if (identifier.startsWith("pact_with_")) {
            String targetName = identifier.substring("pact_with_".length());
            Player target = plugin.getServer().getPlayerExact(targetName);

            if (target == null) return "false";

            boolean hasPact = plugin.getPactManager().hasPact(player.getUniqueId(), target.getUniqueId());
            return String.valueOf(hasPact);
        }

        // %phantompacts_pact_type_<player>% - тип пакта с конкретным игроком
        if (identifier.startsWith("pact_type_")) {
            String targetName = identifier.substring("pact_type_".length());
            Player target = plugin.getServer().getPlayerExact(targetName);

            if (target == null) return "Нет";

            Pact pact = plugin.getPactManager().getPact(player.getUniqueId(), target.getUniqueId());
            if (pact == null || !pact.isActive()) return "Нет";

            return pact.getType().getName();
        }

        // %phantompacts_pact_time_<player>% - оставшееся время пакта
        if (identifier.startsWith("pact_time_")) {
            String targetName = identifier.substring("pact_time_".length());
            Player target = plugin.getServer().getPlayerExact(targetName);

            if (target == null) return "Нет";

            Pact pact = plugin.getPactManager().getPact(player.getUniqueId(), target.getUniqueId());
            if (pact == null || !pact.isActive()) return "Нет";

            return pact.getFormattedRemainingTime();
        }

        // %phantompacts_in_pact_group% - состоит ли в LuckPerms группе пактов
        if (identifier.equals("in_pact_group")) {
            if (!LuckPermsIntegration.isEnabled()) return "false";
            return String.valueOf(LuckPermsIntegration.hasActivePactGroup(player.getUniqueId()));
        }

        return null;
    }
}