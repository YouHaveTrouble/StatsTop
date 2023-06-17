package me.youhavetrouble.statstop;

import org.bukkit.plugin.java.JavaPlugin;

public final class StatsTop extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getCommandMap().register("top", new StatsTopCommand());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
