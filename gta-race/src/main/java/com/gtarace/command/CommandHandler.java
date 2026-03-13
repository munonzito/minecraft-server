package com.gtarace.command;

import com.gtarace.GTARacePlugin;
import com.gtarace.manager.PowerUpManager;
import com.gtarace.manager.RaceManager;
import com.gtarace.manager.TrackBuilder;
import com.gtarace.manager.TrackManager;
import com.gtarace.manager.VehicleManager;
import com.gtarace.model.PowerUpType;
import com.gtarace.model.RaceState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final GTARacePlugin plugin;
    private final RaceManager raceManager;
    private final TrackManager trackManager;
    private final TrackBuilder trackBuilder;
    private final VehicleManager vehicleManager;
    private final PowerUpManager powerUpManager;

    public CommandHandler(GTARacePlugin plugin, RaceManager raceManager,
                          TrackManager trackManager, TrackBuilder trackBuilder,
                          VehicleManager vehicleManager, PowerUpManager powerUpManager) {
        this.plugin = plugin;
        this.raceManager = raceManager;
        this.trackManager = trackManager;
        this.trackBuilder = trackBuilder;
        this.vehicleManager = vehicleManager;
        this.powerUpManager = powerUpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(player);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "stop" -> handleStop(player);
            case "setstart" -> handleSetStart(player);
            case "setfinish" -> handleSetFinish(player);
            case "addcheckpoint" -> handleAddCheckpoint(player);
            case "setcheckpoint" -> handleSetCheckpoint(player, args);
            case "removecheckpoint" -> handleRemoveCheckpoint(player, args);
            case "addpowerup" -> handleAddPowerUp(player, args);
            case "removepowerup" -> handleRemovePowerUp(player, args);
            case "status" -> handleStatus(player);
            case "use" -> handleUsePowerUp(player);
            case "build" -> handleBuild(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleJoin(Player player) {
        if (raceManager.isPlayerInRace(player.getUniqueId())) {
            msg(player, "§cYou are already in the race!");
            return;
        }

        if (raceManager.getState() == RaceState.ACTIVE) {
            msg(player, plugin.getMessage("race-already-running"));
            return;
        }

        if (raceManager.addPlayer(player)) {
            String joinMsg = plugin.getMessage("player-joined").replace("%player%", player.getName());
            msg(player, joinMsg);
        } else {
            msg(player, "§cCannot join the race right now. It may be full.");
        }
    }

    private void handleLeave(Player player) {
        if (!raceManager.isPlayerInRace(player.getUniqueId())) {
            msg(player, "§cYou are not in a race!");
            return;
        }

        raceManager.removePlayer(player);
        msg(player, "§eYou left the race.");
    }

    private void handleStart(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to start the race.");
            return;
        }

        if (raceManager.isRaceActive()) {
            msg(player, plugin.getMessage("race-already-running"));
            return;
        }

        int minPlayers = plugin.getConfig().getInt("race.min-players", 1);
        if (raceManager.getPlayerCount() < minPlayers) {
            String notEnough = plugin.getMessage("not-enough-players")
                    .replace("%min%", String.valueOf(minPlayers));
            msg(player, notEnough);
            return;
        }

        if (trackManager.getStartLocation() == null) {
            msg(player, "§cNo start location set! Use §b/race setstart");
            return;
        }

        if (trackManager.getFinishLocation() == null) {
            msg(player, "§cNo finish location set! Use §b/race setfinish");
            return;
        }

        if (raceManager.startRace()) {
            msg(player, "§aRace starting...");
        } else {
            msg(player, "§cFailed to start the race.");
        }
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to stop the race.");
            return;
        }

        if (raceManager.getState() == RaceState.IDLE) {
            msg(player, plugin.getMessage("no-race-running"));
            return;
        }

        raceManager.stopRace();
    }

    private void handleSetStart(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        trackManager.setStart(player.getLocation());
        msg(player, "§aStart location set to your current position!");
    }

    private void handleSetFinish(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        trackManager.setFinish(player.getLocation());
        msg(player, "§aFinish location set to your current position!");
    }

    private void handleAddCheckpoint(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        int index = trackManager.addCheckpoint(player.getLocation());
        msg(player, "§aCheckpoint §b#" + (index + 1) + " §aadded at your location! Total: §b" + trackManager.getCheckpointCount());
    }

    private void handleSetCheckpoint(Player player, String[] args) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/race setcheckpoint <number>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            msg(player, "§cInvalid number!");
            return;
        }

        if (trackManager.setCheckpoint(index, player.getLocation())) {
            msg(player, "§aCheckpoint §b#" + (index + 1) + " §amoved to your location!");
        } else {
            msg(player, "§cCheckpoint #" + (index + 1) + " does not exist! Total: " + trackManager.getCheckpointCount());
        }
    }

    private void handleRemoveCheckpoint(Player player, String[] args) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/race removecheckpoint <number>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            msg(player, "§cInvalid number!");
            return;
        }

        if (trackManager.removeCheckpoint(index)) {
            msg(player, "§aCheckpoint §b#" + (index + 1) + " §aremoved! Total: §b" + trackManager.getCheckpointCount());
        } else {
            msg(player, "§cCheckpoint #" + (index + 1) + " does not exist!");
        }
    }

    private void handleAddPowerUp(Player player, String[] args) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/race addpowerup <speed_boost|shield|missile|oil_slick>");
            return;
        }

        PowerUpType type;
        try {
            type = PowerUpType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            msg(player, "§cInvalid power-up type! Options: speed_boost, shield, missile, oil_slick");
            return;
        }

        int index = trackManager.addPowerUpSpawn(player.getLocation(), type);
        msg(player, "§aPower-up spawn §b#" + (index + 1) + " §a(" + type.getColoredName() + "§a) added!");
    }

    private void handleRemovePowerUp(Player player, String[] args) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/race removepowerup <number>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            msg(player, "§cInvalid number!");
            return;
        }

        if (trackManager.removePowerUpSpawn(index)) {
            msg(player, "§aPower-up spawn §b#" + (index + 1) + " §aremoved!");
        } else {
            msg(player, "§cPower-up spawn #" + (index + 1) + " does not exist!");
        }
    }

    private void handleUsePowerUp(Player player) {
        if (!raceManager.isPlayerInRace(player.getUniqueId())) {
            msg(player, "§cYou are not in a race!");
            return;
        }

        if (!powerUpManager.hasPowerUp(player.getUniqueId())) {
            msg(player, "§cYou don't have a power-up!");
            return;
        }

        powerUpManager.usePowerUp(player);
    }

    private void handleBuild(Player player) {
        if (!player.hasPermission("gtarace.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (raceManager.isRaceActive()) {
            msg(player, "§cCan't build a track while a race is running!");
            return;
        }

        msg(player, "§eBuilding race track at your location... This may take a moment.");
        trackBuilder.buildTrack(player.getLocation());
        msg(player, "§aRace track built! Start, finish, 7 checkpoints, and 4 power-up spawns set automatically.");
        msg(player, "§7The track is an oval with §bblue ice §7surface for maximum boat speed.");
        msg(player, "§7Players use §b/race join §7then an admin runs §b/race start§7.");
    }

    private void handleStatus(Player player) {
        RaceState state = raceManager.getState();
        player.sendMessage(Component.text("=== GTA Race Status ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("State: " + state.name(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Players: " + raceManager.getPlayerCount(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Checkpoints: " + trackManager.getCheckpointCount(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Laps: " + plugin.getConfig().getInt("race.laps", 3), NamedTextColor.YELLOW));

        if (raceManager.isPlayerInRace(player.getUniqueId()) && state == RaceState.ACTIVE) {
            player.sendMessage(Component.text("Car Health: " + vehicleManager.getHealthBar(player.getUniqueId())));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== GTA Race Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/race join", NamedTextColor.AQUA)
                .append(Component.text(" - Join the next race", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/race leave", NamedTextColor.AQUA)
                .append(Component.text(" - Leave the race", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/race use", NamedTextColor.AQUA)
                .append(Component.text(" - Use held power-up", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/race status", NamedTextColor.AQUA)
                .append(Component.text(" - View race status", NamedTextColor.GRAY)));

        if (player.hasPermission("gtarace.admin")) {
            player.sendMessage(Component.text("/race build", NamedTextColor.GREEN)
                    .append(Component.text(" - Build a premade track at your location", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race start", NamedTextColor.GREEN)
                    .append(Component.text(" - Start the race", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race stop", NamedTextColor.GREEN)
                    .append(Component.text(" - Stop the race", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race setstart", NamedTextColor.GREEN)
                    .append(Component.text(" - Set race start location", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race setfinish", NamedTextColor.GREEN)
                    .append(Component.text(" - Set finish line location", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race addcheckpoint", NamedTextColor.GREEN)
                    .append(Component.text(" - Add checkpoint at your location", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race setcheckpoint <#>", NamedTextColor.GREEN)
                    .append(Component.text(" - Move a checkpoint", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race removecheckpoint <#>", NamedTextColor.GREEN)
                    .append(Component.text(" - Remove a checkpoint", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race addpowerup <type>", NamedTextColor.GREEN)
                    .append(Component.text(" - Add power-up spawn", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/race removepowerup <#>", NamedTextColor.GREEN)
                    .append(Component.text(" - Remove power-up spawn", NamedTextColor.GRAY)));
        }
    }

    private void msg(Player player, String message) {
        player.sendMessage(Component.text(plugin.getMessagePrefix() + message));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "leave", "use", "status"));
            if (sender.hasPermission("gtarace.admin")) {
                subs.addAll(Arrays.asList("build", "start", "stop", "setstart", "setfinish",
                        "addcheckpoint", "setcheckpoint", "removecheckpoint",
                        "addpowerup", "removepowerup"));
            }
            completions = subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("addpowerup")) {
                completions = Arrays.stream(PowerUpType.values())
                        .map(t -> t.name().toLowerCase())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("setcheckpoint") || args[0].equalsIgnoreCase("removecheckpoint")) {
                for (int i = 1; i <= trackManager.getCheckpointCount(); i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(args[1])) completions.add(num);
                }
            } else if (args[0].equalsIgnoreCase("removepowerup")) {
                for (int i = 1; i <= trackManager.getPowerUpSpawns().size(); i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(args[1])) completions.add(num);
                }
            }
        }

        return completions;
    }
}
