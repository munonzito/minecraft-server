package com.castlesiege.command;

import com.castlesiege.CastleSiegePlugin;
import com.castlesiege.manager.GameManager;
import com.castlesiege.model.SiegeKit;
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

    private final CastleSiegePlugin plugin;
    private final GameManager gameManager;

    public CommandHandler(CastleSiegePlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            return handleStart(player);
        }

        switch (args[0].toLowerCase()) {
            case "stop" -> handleStop(player);
            case "kit" -> handleKit(player, args);
            default -> {
                msg(player, "§7Usage: §b/siege §7to start, §b/siege stop §7to end, §b/siege kit <class>");
            }
        }

        return true;
    }

    private boolean handleStart(Player player) {
        if (!player.hasPermission("castlesiege.admin")) {
            msg(player, "§cYou don't have permission to start a siege.");
            return true;
        }

        if (gameManager.isGameActive()) {
            msg(player, "§cA siege is already in progress! Use §b/siege stop §cto end it.");
            return true;
        }

        msg(player, "§eBuilding castle and starting siege... This may take a moment.");
        if (gameManager.startSiege(player.getLocation())) {
            msg(player, "§aSiege started! All players have been joined. Choose your kit!");
        } else {
            msg(player, "§cFailed to start the siege. Are there any players online?");
        }

        return true;
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("castlesiege.admin")) {
            msg(player, "§cYou don't have permission to stop the siege.");
            return;
        }

        if (!gameManager.isGameActive()) {
            msg(player, "§cNo siege is currently running.");
            return;
        }

        gameManager.stopSiege();
    }

    private void handleKit(Player player, String[] args) {
        if (!gameManager.isPlayerInGame(player.getUniqueId())) {
            msg(player, "§cYou are not in a siege!");
            return;
        }

        if (args.length < 2) {
            msg(player, "§7Usage: §b/siege kit <knight|archer|magician|tank>");
            return;
        }

        SiegeKit kit;
        try {
            kit = SiegeKit.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            msg(player, "§cInvalid kit! Choose: knight, archer, magician, or tank");
            return;
        }

        gameManager.setPlayerKit(player, kit);
    }

    private void msg(Player player, String message) {
        player.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + message));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("kit"));
            if (sender.hasPermission("castlesiege.admin")) {
                subs.add("stop");
            }
            completions = subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("kit")) {
            completions = Arrays.stream(SiegeKit.values())
                    .map(k -> k.name().toLowerCase())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
