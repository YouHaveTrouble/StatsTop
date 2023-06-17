package me.youhavetrouble.statstop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class StatsData {

    private final LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
    private final long timestamp;
    private final Statistic stat;
    private final Object arg;

    protected StatsData(LinkedHashMap<String, Integer> data, Statistic stat, Object arg) {
        this.data.putAll(data);
        this.stat = stat;
        this.arg = arg;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Component getTop(int amount, CommandSender sender) {
        Component component = Component.text("Top", NamedTextColor.GOLD)
                .append(Component.space())
                .append(Component.translatable("stat.minecraft." + stat.toString().toLowerCase(Locale.ENGLISH), NamedTextColor.GOLD));

        if (arg != null) {
            if (arg instanceof EntityType entityType) {
                component = component
                        .append(Component.text(" - "))
                        .append(Component.translatable("entity.minecraft." + entityType.toString().toLowerCase(Locale.ENGLISH)));
            } else if (arg instanceof Material material) {
                component = component.append(Component.text(" - ", NamedTextColor.GOLD));

                if (material.isBlock()) {
                    component = component.append(Component.translatable("block.minecraft." + material.toString().toLowerCase(Locale.ENGLISH), NamedTextColor.GOLD));
                } else if (material.isItem()) {
                    component = component.append(Component.translatable("item.minecraft." + material.toString().toLowerCase(Locale.ENGLISH), NamedTextColor.GOLD));
                }

            }
        }

        String senderName = null;

        if (sender instanceof Player player) {
            senderName = player.getName();
        }

        HashSet<String> names = new HashSet<>(amount);
        Component senderData = null;
        int i = 0;
        for (Map.Entry<String, Integer> key : data.entrySet()) {
            if (key.getKey().equals(senderName)) {
                senderData = getLine(i + 1, key.getKey(), key.getValue(), NamedTextColor.YELLOW);
            }
            if (i >= amount && senderData != null) continue;
            names.add(key.getKey());
            component = component.append(Component.newline());
            component = component.append(key.getKey().equals(senderName) ? senderData : getLine(i + 1, key.getKey(), key.getValue(), NamedTextColor.WHITE));
            i++;
        }

        if (sender instanceof Player player && !names.contains(player.getName()) && senderData != null) {
            component = component.append(senderData);
        }
        return component;
    }

    private Component getLine(int place, String name, int value, TextColor textColor) {
        return Component.text(place + ". " + name + " - " + value, textColor);
    }
}
