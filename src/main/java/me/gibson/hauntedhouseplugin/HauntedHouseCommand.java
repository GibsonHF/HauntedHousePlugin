package me.gibson.hauntedhouseplugin;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static me.gibson.hauntedhouseplugin.HauntedHousePlugin.PREFIX;
import static me.gibson.hauntedhouseplugin.HauntedHousePlugin.rewardCommands;

public class HauntedHouseCommand implements CommandExecutor, Listener, TabCompleter {


    public HauntedHousePlugin plugin;
    public boolean challengeOngoing = false;

    public final Map<Location, UUID> claimedPlots = new HashMap<>();

    public final Map<UUID, UUID> playerVotes = new HashMap<>();

    public World plotWorld;

    public HauntedHouseCommand(HauntedHousePlugin plugin) {
        this.plugin = plugin;
        plotWorld = Bukkit.getWorld(plugin.plotWorldName);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Please specify an action.");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (command.getName().toLowerCase()) {
            case "hauntedhouse":
                handleHauntedHouseCommand(sender, action, args);
                break;
        }

        return true;
    }

    private void handleHauntedHouseCommand(CommandSender sender, String action, String[] args) {
        switch (action) {
            case "start":
                if (!challengeOngoing) {
                    challengeOngoing = true;
                    saveChallengeState(true);  // Save that the challenge is now ongoing
                    Bukkit.broadcastMessage(PREFIX + "The Haunted House Challenge has started! Use "+ChatColor.GREEN+"/hh join "+ChatColor.WHITE+"to Join the Event!");
                } else {
                    sender.sendMessage("The challenge is already ongoing!");
                }
                break;
            case "teleport":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Only players can use this command.");
                    return;
                }
                if(!challengeOngoing) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "The challenge is not ongoing!");
                    return;
                }
                Player player1 = (Player) sender;
                PlotPlayer<?> plotPlayer1 = PlotPlayer.from(player1);
                Set<Plot> plots1 = plotPlayer1.getPlots();  // Fetch the player's plots

                if (plots1.isEmpty()) {
                    player1.sendMessage(PREFIX + ChatColor.RED + "You don't have any plots!");
                    return;
                }

                Plot plot = plots1.iterator().next();

                // Fetch the home location for the plot
                com.plotsquared.core.location.Location[] psCenter1 = new com.plotsquared.core.location.Location[1];
                plot.getCenter(location -> psCenter1[0] = location);
                player1.teleport(new Location(plotWorld, psCenter1[0].getX(), psCenter1[0].getY(), psCenter1[0].getZ()));
                player1.sendMessage(PREFIX + ChatColor.GREEN + "Teleported to your plot!");

                break;
            case "join":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Only players can join!");
                    return;
                }
                if(!challengeOngoing) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "The challenge is not ongoing!");
                    return;
                }
                Player player = (Player) sender;
                PlotPlayer<?> plotPlayer = PlotPlayer.from(player);
                Set<Plot> playerPlots = plotPlayer.getPlots();
                if (playerPlots.isEmpty()) {
                    PlotArea plotArea = PlotSquared.get().getPlotAreaManager().getAllPlotAreas()[0];
                    Plot newPlot = plotArea.getNextFreePlot(plotPlayer, null);
                    if (newPlot != null) {
                        newPlot.claim(plotPlayer, false, null, true, true);
                        player.sendMessage(PREFIX + ChatColor.GREEN + "You have been assigned a plot!");
                        com.plotsquared.core.location.Location[] psCenter = new com.plotsquared.core.location.Location[1];
                        newPlot.getCenter(location -> psCenter[0] = location);
                        player.teleport(new Location(plotWorld, psCenter[0].getX(), psCenter[0].getY() + 3, psCenter[0].getZ()));
                    } else {
                        player.sendMessage(PREFIX + ChatColor.RED + "No free plots available!");
                    }
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "You already have a plot!");
                }
                break;
            case "reload":
                if (!sender.hasPermission("hauntedhouse.reload")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to reload the configuration.");
                    return;
                }

                plugin.reloadConfig();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuration reloaded successfully!");
                break;

            case "vote":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Only players can vote!");
                    return;
                }

                Player voter = (Player) sender;
                if (args.length < 2) {
                    voter.sendMessage(PREFIX + ChatColor.RED + "Usage: /hauntedhouse vote <player_name>");
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    voter.sendMessage(PREFIX + ChatColor.RED + "That player is not online or does not exist.");
                    return;
                }

                if (target.getUniqueId().equals(voter.getUniqueId())) {
                    voter.sendMessage(PREFIX + ChatColor.RED + "You cannot vote for your own plot!");
                    return;
                }

                PlotPlayer<?> targetPlotPlayer = PlotPlayer.from(target);
                Set<Plot> targetPlots = targetPlotPlayer.getPlots();

                if (targetPlots.isEmpty()) {
                    voter.sendMessage(PREFIX + ChatColor.RED + "That player has not claimed a plot.");
                    return;
                }

                if (playerVotes.containsKey(voter.getUniqueId())) {
                    voter.sendMessage(PREFIX + ChatColor.RED + "You've already voted!");
                    return;
                }

                playerVotes.put(voter.getUniqueId(), target.getUniqueId());
                voter.sendMessage(PREFIX + ChatColor.GREEN + "You've successfully voted for " + target.getName() + "'s haunted house!");
                break;
            case "end":
                if (!challengeOngoing) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "The challenge is not ongoing!");
                    return;
                }
                if (!sender.hasPermission("hauntedhouse.end")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to end the challenge.");
                    return;
                }

                if (playerVotes.isEmpty()) {
                    Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "The Haunted House Challenge has ended with no votes.");
                    resetChallenge();  // Ensure everything is reset even if there were no votes
                    return;
                }

                // Tallying votes
                Map<UUID, Integer> voteTally = new HashMap<>();
                for (UUID votedPlayerUUID : playerVotes.values()) {
                    voteTally.put(votedPlayerUUID, voteTally.getOrDefault(votedPlayerUUID, 0) + 1);
                }

                // Finding the player with the most votes
                UUID winnerUUID = Collections.max(voteTally.entrySet(), Map.Entry.comparingByValue()).getKey();
                Player winner = Bukkit.getPlayer(winnerUUID);
                int maxVotes = voteTally.get(winnerUUID);

                    Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + winner.getName() + " has won the Haunted House Challenge with " + maxVotes + " votes!");
                    // Give rewards to the winner
                    rewardPlayer(winner);

                // Reset the challenge
                resetChallenge();
                break;

            default:
                sender.sendMessage("Invalid action!");
                break;
        }
    }

    public void rewardPlayer(Player player) {
        for (String command : rewardCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        player.sendMessage(PREFIX + ChatColor.GREEN + "You've received your rewards!");
    }

    private void resetChallenge() {
        claimedPlots.clear();
        playerVotes.clear();
        saveChallengeState(false);
        challengeOngoing = false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {  // If the user has only typed "/hauntedhouse " and then hits tab
            // List of possible sub-commands for "/hh"
            return Arrays.asList("start", "join", "vote", "end", "reload", "teleport");
        } else if (args.length == 2) {  // If the user has typed "/hauntedhouse <sub-command> " and then hits tab
            switch (args[0]) {
                case "vote":
                    // List of online players
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                default:
                    return null;
            }
        }

        return null;  // Return null or an empty list to indicate no suggestions.
    }


    public void saveChallengeState(boolean ongoing) {
        plugin.getConfig().set("challengeOngoing", ongoing);
        plugin.saveConfig();
    }
}