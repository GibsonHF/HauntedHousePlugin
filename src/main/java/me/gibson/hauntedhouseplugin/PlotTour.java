package me.gibson.hauntedhouseplugin;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
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

    private boolean isTourOngoing = false;


    public PlotTour(HauntedHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void startPlotTour() {
        if (isTourOngoing) {
            return;  // Exit if a tour is already ongoing
        }
        isTourOngoing = true;
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

                // Get the bottom and top corners
                com.plotsquared.core.location.Location bottom = currentPlot.getBottom();
                com.plotsquared.core.location.Location top = currentPlot.getTop();

                // Calculate the center along X and the front edge along Z
                double centerX = (bottom.getX() + top.getX()) / 2.0;
                double frontZ = top.getZ() + 1; // One block outside the plot boundary

                Location frontOfPlot = new Location(Bukkit.getWorld(plugin.plotWorldName), centerX, Bukkit.getWorld(plugin.plotWorldName).getHighestBlockYAt((int)centerX, (int)frontZ), frontZ);


                for (Player player : Bukkit.getOnlinePlayers()) {
                    //check if player has a plot meaning in competition to teleport to it
                    PlotPlayer<?> plotPlayer = PlotSquared.platform().playerManager().getPlayer(player.getUniqueId());
                    if (plotPlayer != null && !plotPlayer.getPlots().isEmpty()) {
                        player.teleport(frontOfPlot);
                        player.sendMessage(PREFIX + "Now visiting " + currentPlot.getOwner() + "'s plot!");
                    }
                }

                currentPlotIndex++;
            } else {
                plotTourTask.cancel();
                isTourOngoing = false;  // Reset the flag when the tour ends
                for (Player player : Bukkit.getOnlinePlayers()) {
                    //check if player has a plot meaning in competition to teleport to it
                    PlotPlayer<?> plotPlayer = PlotSquared.platform().playerManager().getPlayer(player.getUniqueId());
                    if (plotPlayer != null && !plotPlayer.getPlots().isEmpty()) {
                        player.sendMessage(PREFIX + "The plot tour has ended! You can now cast your votes.");
                    }
                }
            }
        }, 0L, 20L * 30);  // runs every minute
    }

    public boolean isTourOngoing() {
        return isTourOngoing;
    }
}
