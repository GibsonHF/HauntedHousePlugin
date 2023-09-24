package me.gibson.hauntedhouseplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;


public class HauntedHousePlugin extends JavaPlugin {
    public static final String PREFIX = ChatColor.translateAlternateColorCodes('&',"&6&l[HauntedHouse] &r");
    public String plotWorldName;
   public static List<String> rewardCommands;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        rewardCommands = getConfig().getStringList("reward-commands");
        plotWorldName = getConfig().getString("plot-world-name");
        this.getLogger().info("HauntedHousePlugin has been enabled!");
        HauntedHouseCommand hauntedHouseCommandInstance = new HauntedHouseCommand(this);
        this.getCommand("hauntedhouse").setExecutor(hauntedHouseCommandInstance);
        this.getCommand("hauntedhouse").setTabCompleter(new HauntedHouseCommand(this));
        Bukkit.getServer().getPluginManager().registerEvents(hauntedHouseCommandInstance, this);
        boolean wasChallengeOngoing = this.getConfig().getBoolean("challengeOngoing", false);
        hauntedHouseCommandInstance.challengeOngoing = wasChallengeOngoing;

    }



    @Override
    public void onDisable() {
        this.getLogger().info("HauntedHousePlugin has been disabled!");
    }
}