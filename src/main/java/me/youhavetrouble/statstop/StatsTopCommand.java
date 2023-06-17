package me.youhavetrouble.statstop;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.StringUtil;
import org.bukkit.util.permissions.DefaultPermissions;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StatsTopCommand extends Command {

    private final HashMap<String, StatsData> cachedData = new HashMap<>();

    protected StatsTopCommand() {
        super("top", "Display top for given stat", "/top <stat> [arg]", new ArrayList<>());
        setPermission("statstop.top");
        DefaultPermissions.registerPermission("statstop.top", "Allows you to use /top", PermissionDefault.TRUE);
    }

    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Statistic stat : Statistic.values()) {
                if (StringUtil.startsWithIgnoreCase(stat.toString(), args[0])) {
                    completions.add(stat.toString().toLowerCase(Locale.ENGLISH));
                }
            }
        }
        if (args.length == 2) {
            Statistic statistic;
            try {
                statistic = Statistic.valueOf(args[0].toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                return completions;
            }

            switch (statistic.getType()) {
                case ITEM -> {
                    for (Material material : Material.values()) {
                        if (!material.isItem()) continue;
                        if (StringUtil.startsWithIgnoreCase(material.toString(), args[1])) {
                            completions.add(material.toString().toLowerCase(Locale.ENGLISH));
                        }
                    }
                }
                case BLOCK -> {
                    for (Material material : Material.values()) {
                        if (!material.isBlock()) continue;
                        if (StringUtil.startsWithIgnoreCase(material.toString(), args[1])) {
                            completions.add(material.toString().toLowerCase(Locale.ENGLISH));
                        }
                    }
                }
                case ENTITY -> {
                    for (EntityType entityType : EntityType.values()) {
                        if (StringUtil.startsWithIgnoreCase(entityType.toString(), args[1])) {
                            completions.add(entityType.toString().toLowerCase(Locale.ENGLISH));
                        }
                    }
                }
            }

        }

        return completions;
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can use this command"));
            return true;
        }

        if (strings.length < 1) return false;
        String statName = strings[0];
        Statistic stat = null;

        try {
            stat = Statistic.valueOf(statName.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            commandSender.sendMessage(Component.text(getUsage()));
            return false;
        }

        Object arg = null;

        if (strings.length == 2) {
            try {
                arg = Material.valueOf(strings[1].toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException ignored) {
            }
            if (arg == null) {
                try {
                    arg = EntityType.valueOf(strings[1].toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (arg == null) {
                commandSender.sendMessage(Component.text(getUsage()));
                return false;
            }
        }

        StatsData statsData = null;

        switch (stat.getType()) {
            case UNTYPED -> {
                statsData = cachedData.get(getStatId(stat, null));
                if (statsData == null || statsData.getTimestamp() + 60000 < System.currentTimeMillis()) {
                    statsData = createData(stat);
                    cachedData.put(getStatId(stat, null), statsData);
                }
            }
            case ITEM, BLOCK -> {
                if (!(arg instanceof Material material)) return false;
                statsData = cachedData.get(getStatId(stat, arg));
                if (statsData == null || statsData.getTimestamp() + 60000 < System.currentTimeMillis()) {
                    statsData = createData(stat, material);
                    cachedData.put(getStatId(stat, arg), statsData);
                }
            }
            case ENTITY -> {
                if (!(arg instanceof EntityType entityType)) return false;
                statsData = cachedData.get(getStatId(stat, arg));
                if (statsData == null || statsData.getTimestamp() + 60000 < System.currentTimeMillis()) {
                    statsData = createData(stat, entityType);
                    cachedData.put(getStatId(stat, arg), statsData);
                }
            }
        }

        if (statsData == null) {
            commandSender.sendMessage(Component.text("Error getting data"));
            return false;
        }

        Component topComponent = statsData.getTop(10, commandSender);
        commandSender.sendMessage(topComponent);

        return false;
    }

    private String getStatId(Statistic stat, Object arg) {

        String statKey = stat.getKey().getKey();

        return switch (stat.getType()) {
            case UNTYPED -> statKey;
            case ITEM, BLOCK -> {
                if (arg instanceof Material item) {
                    yield statKey + "." + item;
                } else {
                    yield null;
                }
            }
            case ENTITY -> {
                if (arg instanceof EntityType entityType) {
                    yield statKey + "." + entityType;
                } else {
                    yield null;
                }
            }
        };
    }

    /**
     * Creates a map of player names to their stat value. This will return null for stats that are not UNTYPED.
     *
     * @param stat statistic to get data for
     * @return map of player names to their stat value
     */
    private StatsData createData(Statistic stat) {
        if (stat.getType() != Statistic.Type.UNTYPED) return null;
        HashMap<String, Integer> data = new HashMap<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            int statValue = offlinePlayer.getStatistic(stat);
            String playerName = offlinePlayer.getName();
            data.put(playerName, statValue);
        }

        LinkedHashMap<String, Integer> sortedData = new LinkedHashMap<>();
        data.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedData.put(x.getKey(), x.getValue()));

        return new StatsData(sortedData, stat, null);
    }

    /**
     * Creates a map of player names to their stat value. This will return null for stats that are not ENTITY.
     *
     * @param stat statistic to get data for
     * @return map of player names to their stat value
     */
    private StatsData createData(Statistic stat, EntityType entityType) {
        if (stat.getType() != Statistic.Type.ENTITY) return null;
        HashMap<String, Integer> data = new HashMap<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            int statValue = offlinePlayer.getStatistic(stat, entityType);
            String playerName = offlinePlayer.getName();
            data.put(playerName, statValue);
        }

        LinkedHashMap<String, Integer> sortedData = new LinkedHashMap<>();
        data.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedData.put(x.getKey(), x.getValue()));

        return new StatsData(sortedData, stat, entityType);
    }

    /**
     * Creates a map of player names to their stat value. This will return null for stats that are not BLOCK or ITEM.
     *
     * @param stat statistic to get data for
     * @return map of player names to their stat value
     */
    private StatsData createData(Statistic stat, Material item) {
        if (stat.getType() != Statistic.Type.ITEM && stat.getType() != Statistic.Type.BLOCK) return null;
        HashMap<String, Integer> data = new HashMap<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            int statValue = offlinePlayer.getStatistic(stat, item);
            String playerName = offlinePlayer.getName();
            data.put(playerName, statValue);
        }

        LinkedHashMap<String, Integer> sortedData = new LinkedHashMap<>();
        data.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedData.put(x.getKey(), x.getValue()));

        return new StatsData(sortedData, stat, item);
    }
}
