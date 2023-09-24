package me.gibson.hauntedhouseplugin;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.gibson.hauntedhouseplugin.HauntedHousePlugin.PREFIX;

public class PlotTour {
    public List<Plot> allPlots = new ArrayList<>();
    private int currentPlotIndex = 0;
    private BukkitTask plotTourTask;

    private HauntedHousePlugin plugin;

    public PlotTour(HauntedHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void startPlotTour() {
        for (PlotArea plotArea : PlotSquared.get().getPlotAreaManager().getAllPlotAreas()) {
            if (plotArea.getWorldName().equalsIgnoreCase(plugin.plotWorldName)) {
                allPlots.addAll(plotArea.getPlots());
            }
        }
        currentPlotIndex = 0;
        //TODO: somewhere in this add plot voting
        plotTourTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentPlotIndex < allPlots.size()) {
                Plot currentPlot = allPlots.get(currentPlotIndex);
                //TODO: get Plot center?
                Location plotCenter = currentPlot.getCenter(location -> location.getWorld().getName().equalsIgnoreCase(plugin.plotWorldName));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleport(plotCenter);
                    player.sendMessage(PREFIX + "Now visiting " + currentPlot.getOwner() + "'s plot!");
                }

                currentPlotIndex++;
            } else {
                plotTourTask.cancel();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(PREFIX + "Plot tour has ended!");
                }
            }
        }, 0L, 20L * 30);  // runs every minute
    }

}
