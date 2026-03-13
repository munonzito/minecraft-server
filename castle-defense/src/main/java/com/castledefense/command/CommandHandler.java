package com.castledefense.command;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.manager.ArenaManager;
import com.castledefense.manager.BlueprintManager;
import com.castledefense.manager.CastleBuilder;
import com.castledefense.manager.GameManager;
import com.castledefense.manager.KitManager;
import com.castledefense.model.GameState;
import com.castledefense.model.Kit;
import com.castledefense.model.Team;
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

    private final CastleDefensePlugin plugin;
    private final GameManager gameManager;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final CastleBuilder castleBuilder;
    private final BlueprintManager blueprintManager;

    public CommandHandler(CastleDefensePlugin plugin, GameManager gameManager,
                          ArenaManager arenaManager, KitManager kitManager,
                          CastleBuilder castleBuilder, BlueprintManager blueprintManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.castleBuilder = castleBuilder;
        this.blueprintManager = blueprintManager;
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
            case "kit" -> handleKit(player, args);
            case "start" -> handleStart(player);
            case "stop" -> handleStop(player);
            case "setspawn" -> handleSetSpawn(player, args);
            case "build" -> handleBuild(player);
            case "cannon" -> handleCannon(player);
            case "status" -> handleStatus(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleJoin(Player player) {
        if (gameManager.isPlayerInGame(player.getUniqueId())) {
            msg(player, "§cYou are already in the game!");
            return;
        }

        if (gameManager.getState() == GameState.ACTIVE) {
            msg(player, plugin.getMessage("game-already-running"));
            return;
        }

        if (gameManager.addPlayer(player)) {
            String joinMsg = plugin.getMessage("player-joined").replace("%player%", player.getName());
            msg(player, joinMsg);
            msg(player, "§7Use §b/castle kit <knight|archer|tank> §7to choose your class.");
        } else {
            msg(player, "§cCannot join the game right now.");
        }
    }

    private void handleKit(Player player, String[] args) {
        if (!gameManager.isPlayerInGame(player.getUniqueId())) {
            msg(player, "§cYou must join the game first! Use §b/castle join");
            return;
        }

        if (gameManager.getState() == GameState.ACTIVE) {
            msg(player, "§cYou can't change your kit during a game!");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Available kits: §6Knight§7, §aArcher§7, §7Tank");
            msg(player, "§7Usage: §b/castle kit <knight|archer|tank>");
            return;
        }

        Kit kit;
        try {
            kit = Kit.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            msg(player, "§cInvalid kit! Choose: knight, archer, or tank");
            return;
        }

        gameManager.setPlayerKit(player, kit);
        String kitMsg = plugin.getMessage("kit-selected").replace("%kit%", kit.getColoredName());
        msg(player, kitMsg);
    }

    private void handleStart(Player player) {
        if (!player.hasPermission("castledefense.admin")) {
            msg(player, "§cYou don't have permission to start the game.");
            return;
        }

        if (gameManager.isGameActive()) {
            msg(player, plugin.getMessage("game-already-running"));
            return;
        }

        int minPlayers = plugin.getConfig().getInt("game.min-players", 2);
        if (gameManager.getPlayerCount() < minPlayers) {
            String notEnough = plugin.getMessage("not-enough-players")
                    .replace("%min%", String.valueOf(minPlayers));
            msg(player, notEnough);
            return;
        }

        if (gameManager.startGame()) {
            msg(player, "§aGame starting...");
        } else {
            msg(player, "§cFailed to start the game.");
        }
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("castledefense.admin")) {
            msg(player, "§cYou don't have permission to stop the game.");
            return;
        }

        if (gameManager.getState() == GameState.IDLE) {
            msg(player, plugin.getMessage("no-game-running"));
            return;
        }

        gameManager.stopGame();
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (!player.hasPermission("castledefense.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/castle setspawn <red|blue>");
            return;
        }

        Team team;
        switch (args[1].toLowerCase()) {
            case "red", "r" -> team = Team.RED;
            case "blue", "b" -> team = Team.BLUE;
            default -> {
                msg(player, "§cInvalid team! Use: red or blue");
                return;
            }
        }

        arenaManager.setSpawn(team, player.getLocation());
        msg(player, "§a" + team.getDisplayName() + " spawn set to your current location!");
    }

    private void handleCannon(Player player) {
        if (!player.hasPermission("castledefense.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        String facing = getPlayerFacing(player);
        msg(player, "§ePlacing TNT cannon facing §b" + facing + "§e...");

        if (blueprintManager.placeBlueprint("tnt_cannon", player.getLocation(), facing)) {
            msg(player, "§aTNT cannon placed! Press the button to fire.");
        } else {
            msg(player, "§cFailed to place cannon blueprint.");
        }
    }

    private String getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return "south";
        if (yaw >= 45 && yaw < 135) return "west";
        if (yaw >= 135 && yaw < 225) return "north";
        return "east";
    }

    private void handleBuild(Player player) {
        if (!player.hasPermission("castledefense.admin")) {
            msg(player, "§cYou don't have permission to do this.");
            return;
        }

        if (gameManager.isGameActive()) {
            msg(player, "§cCan't build while a game is running!");
            return;
        }

        msg(player, "§eBuilding two castles at your location... This may take a moment.");
        castleBuilder.buildArena(player.getLocation());
        msg(player, "§aArena built! Two castles face each other with banners inside each throne room.");
        msg(player, "§c Red castle §7has a §cRed Banner§7. §9Blue castle §7has a §9Blue Banner§7.");
        msg(player, "§7Destroy the enemy banner to win!");
    }

    private void handleStatus(Player player) {
        GameState state = gameManager.getState();
        player.sendMessage(Component.text("=== Castle Defense Status ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("State: " + state.name(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Players: " + gameManager.getPlayerCount(), NamedTextColor.YELLOW));

        if (state == GameState.ACTIVE) {
            player.sendMessage(Component.text("Red: " + gameManager.getTeamCount(Team.RED), NamedTextColor.RED));
            player.sendMessage(Component.text("Blue: " + gameManager.getTeamCount(Team.BLUE), NamedTextColor.BLUE));
            player.sendMessage(Component.text("Time remaining: " + formatTime(gameManager.getTimeRemaining()), NamedTextColor.AQUA));
        }

        if (gameManager.isPlayerInGame(player.getUniqueId())) {
            Team team = gameManager.getPlayerTeam(player.getUniqueId());
            Kit kit = gameManager.getPlayerKit(player.getUniqueId());
            String teamStr = team != null ? team.getColoredName() : "§7Not assigned";
            String kitStr = kit != null ? kit.getColoredName() : "§7Not selected";
            player.sendMessage(Component.text("Your team: " + teamStr));
            player.sendMessage(Component.text("Your kit: " + kitStr));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== Castle Defense Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/castle join", NamedTextColor.AQUA)
                .append(Component.text(" - Join the next game", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/castle kit <class>", NamedTextColor.AQUA)
                .append(Component.text(" - Choose knight/archer/tank", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/castle status", NamedTextColor.AQUA)
                .append(Component.text(" - View game status", NamedTextColor.GRAY)));

        if (player.hasPermission("castledefense.admin")) {
            player.sendMessage(Component.text("/castle cannon", NamedTextColor.GREEN)
                    .append(Component.text(" - Place a TNT cannon", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/castle build", NamedTextColor.GREEN)
                    .append(Component.text(" - Build the two-castle arena", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/castle start", NamedTextColor.GREEN)
                    .append(Component.text(" - Start the game", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/castle stop", NamedTextColor.GREEN)
                    .append(Component.text(" - Stop the game", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/castle setspawn <team>", NamedTextColor.GREEN)
                    .append(Component.text(" - Set team spawn (red/blue)", NamedTextColor.GRAY)));
        }
    }

    private void msg(Player player, String message) {
        player.sendMessage(Component.text(plugin.getMessagePrefix() + message));
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "kit", "status"));
            if (sender.hasPermission("castledefense.admin")) {
                subs.addAll(Arrays.asList("build", "cannon", "start", "stop", "setspawn"));
            }
            completions = subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("kit")) {
                completions = Arrays.stream(Kit.values())
                        .map(k -> k.name().toLowerCase())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("setspawn")) {
                completions = List.of("red", "blue").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
