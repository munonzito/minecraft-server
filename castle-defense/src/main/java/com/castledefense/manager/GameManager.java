package com.castledefense.manager;

import com.castledefense.CastleDefensePlugin;
import com.castledefense.model.GameState;
import com.castledefense.model.Kit;
import com.castledefense.model.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class GameManager {

    private final CastleDefensePlugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;

    private GameState state = GameState.IDLE;
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private final Map<UUID, Kit> playerKits = new HashMap<>();
    private final Set<UUID> gamePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();

    private final List<Horse> spawnedHorses = new ArrayList<>();
    private BukkitTask gameTimerTask;
    private BukkitTask countdownTask;
    private int timeRemaining;

    private static final int HORSES_PER_TEAM = 3;

    public GameManager(CastleDefensePlugin plugin, ArenaManager arenaManager, KitManager kitManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
    }

    public boolean addPlayer(Player player) {
        if (state != GameState.IDLE && state != GameState.COUNTDOWN) {
            return false;
        }
        gamePlayers.add(player.getUniqueId());
        if (!playerKits.containsKey(player.getUniqueId())) {
            playerKits.put(player.getUniqueId(), Kit.KNIGHT);
        }
        return true;
    }

    public void setPlayerKit(Player player, Kit kit) {
        playerKits.put(player.getUniqueId(), kit);
    }

    public boolean startGame() {
        if (state != GameState.IDLE) return false;

        int minPlayers = plugin.getConfig().getInt("game.min-players", 2);
        if (gamePlayers.size() < minPlayers) return false;

        state = GameState.COUNTDOWN;
        int countdownSeconds = plugin.getConfig().getInt("game.countdown-seconds", 10);

        countdownTask = new BukkitRunnable() {
            int seconds = countdownSeconds;

            @Override
            public void run() {
                if (seconds <= 0) {
                    cancel();
                    beginGame();
                    return;
                }

                for (UUID uuid : gamePlayers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.showTitle(Title.title(
                                Component.text(String.valueOf(seconds), NamedTextColor.GOLD, TextDecoration.BOLD),
                                Component.text("Get ready!", NamedTextColor.YELLOW),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                        ));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void beginGame() {
        state = GameState.ACTIVE;
        assignTeams();
        arenaManager.placeTargetBlocks();

        timeRemaining = plugin.getConfig().getInt("game.duration-seconds", 600);

        for (UUID uuid : gamePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Team team = playerTeams.get(uuid);
            Kit kit = playerKits.getOrDefault(uuid, Kit.KNIGHT);

            player.setGameMode(GameMode.SURVIVAL);
            kitManager.applyKit(player, kit, team);
            player.teleport(arenaManager.getSpawn(team));

            NamedTextColor teamColor = team == Team.RED ? NamedTextColor.RED : NamedTextColor.BLUE;
            player.showTitle(Title.title(
                    Component.text("BATTLE!", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("You are " + team.getColoredName(), NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        spawnHorses();
        broadcastToPlayers(plugin.getMessage("game-started"));
        startGameTimer();
    }

    private void assignTeams() {
        List<UUID> shuffled = new ArrayList<>(gamePlayers);
        Collections.shuffle(shuffled);

        int half = shuffled.size() / 2;
        for (int i = 0; i < shuffled.size(); i++) {
            Team team = i < half ? Team.RED : Team.BLUE;
            playerTeams.put(shuffled.get(i), team);
        }
    }

    private void startGameTimer() {
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeRemaining <= 0) {
                    endGame(null);
                    cancel();
                    return;
                }

                if (timeRemaining <= 10 || timeRemaining == 30 || timeRemaining == 60 ||
                        timeRemaining == 120 || timeRemaining == 300) {
                    broadcastToPlayers(plugin.getMessagePrefix() + "§eTime remaining: §b" + formatTime(timeRemaining));
                }

                for (UUID uuid : gamePlayers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        Team team = playerTeams.get(uuid);
                        String teamTag = team != null ? team.getColoredName() : "§7Spectator";
                        p.sendActionBar(Component.text(
                                "§7Team: " + teamTag + " §7| §eTime: §b" + formatTime(timeRemaining)
                        ));
                    }
                }

                timeRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void onTargetBlockBroken(Player breaker, Team bannerTeam) {
        if (state != GameState.ACTIVE) return;

        Team breakerTeam = playerTeams.get(breaker.getUniqueId());
        if (breakerTeam != bannerTeam) {
            broadcastToPlayers(plugin.getMessagePrefix() + "§c§l" + breaker.getName()
                    + " §ehas destroyed the " + bannerTeam.getColoredName() + " §ebanner!");
            endGame(breakerTeam);
        } else {
            breaker.sendMessage(Component.text(plugin.getMessagePrefix() + "§cYou cannot break your own castle's banner!"));
        }
    }

    public void endGame(Team winner) {
        if (state == GameState.IDLE || state == GameState.ENDING) return;
        state = GameState.ENDING;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        boolean draw = winner == null;
        String winMessage;
        if (draw) {
            winMessage = plugin.getMessage("game-ended-draw");
        } else {
            winMessage = plugin.getMessage("game-ended-win")
                    .replace("%team%", winner.getColoredName());
        }

        for (UUID uuid : gamePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            cancelRespawnTask(uuid);

            Team team = playerTeams.get(uuid);
            boolean won = !draw && team == winner;

            if (draw) {
                player.showTitle(Title.title(
                        Component.text("DRAW!", NamedTextColor.YELLOW, TextDecoration.BOLD),
                        Component.text("Time ran out!", NamedTextColor.WHITE),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
            } else {
                player.showTitle(Title.title(
                        Component.text(won ? "VICTORY!" : "DEFEAT!", won ? NamedTextColor.GOLD : NamedTextColor.RED, TextDecoration.BOLD),
                        Component.text(winner.getDisplayName() + " team wins!", NamedTextColor.WHITE),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
                player.playSound(player.getLocation(),
                        won ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_WITHER_DEATH,
                        1.0f, 1.0f);
            }

            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
            player.setHealth(20.0);
            player.setGameMode(GameMode.SPECTATOR);
        }

        broadcastToPlayers(plugin.getMessagePrefix() + winMessage);

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, 100L);
    }

    public void stopGame() {
        if (state == GameState.IDLE) return;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        for (UUID uuid : gamePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                cancelRespawnTask(uuid);
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
                player.setHealth(20.0);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        broadcastToPlayers(plugin.getMessagePrefix() + plugin.getMessage("game-stopped"));
        cleanup();
    }

    private void spawnHorses() {
        for (Team team : Team.values()) {
            Location stable = arenaManager.getStable(team);
            if (stable == null) continue;

            Horse.Color color = team == Team.RED ? Horse.Color.DARK_BROWN : Horse.Color.WHITE;

            for (int i = 0; i < HORSES_PER_TEAM; i++) {
                Location spawnLoc = stable.clone().add(i * 2 - 2, 0, 0);
                Horse horse = stable.getWorld().spawn(spawnLoc, Horse.class, h -> {
                    h.setTamed(true);
                    h.setColor(color);
                    h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                    h.setCustomName(team.getColor() + team.getDisplayName() + " Horse");
                    h.setCustomNameVisible(true);
                    h.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
                    h.setHealth(40.0);
                    h.setAdult();
                });
                spawnedHorses.add(horse);
            }
        }
    }

    private void removeHorses() {
        for (Horse horse : spawnedHorses) {
            if (horse != null && !horse.isDead()) {
                horse.getPassengers().forEach(horse::removePassenger);
                horse.remove();
            }
        }
        spawnedHorses.clear();
    }

    private void cleanup() {
        removeHorses();
        for (UUID uuid : gamePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        playerTeams.clear();
        gamePlayers.clear();
        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();
        state = GameState.IDLE;
    }

    public void handlePlayerDeath(Player player) {
        if (state != GameState.ACTIVE) return;
        if (!gamePlayers.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        Team team = playerTeams.get(uuid);
        Kit kit = playerKits.getOrDefault(uuid, Kit.KNIGHT);
        int respawnDelay = plugin.getConfig().getInt("game.respawn-delay-seconds", 10);

        player.setGameMode(GameMode.SPECTATOR);

        BukkitTask task = new BukkitRunnable() {
            int seconds = respawnDelay;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || state != GameState.ACTIVE) {
                    cancel();
                    respawnTasks.remove(uuid);
                    return;
                }

                if (seconds <= 0) {
                    p.setGameMode(GameMode.SURVIVAL);
                    kitManager.applyKit(p, kit, team);
                    p.teleport(arenaManager.getSpawn(team));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    cancel();
                    respawnTasks.remove(uuid);
                    return;
                }

                p.showTitle(Title.title(
                        Component.text("§cYou died!"),
                        Component.text("§eRespawning in §b" + seconds + "§e seconds"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                ));
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        respawnTasks.put(uuid, task);
    }

    private void cancelRespawnTask(UUID uuid) {
        BukkitTask task = respawnTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelRespawnTask(uuid);
        gamePlayers.remove(uuid);
        playerTeams.remove(uuid);

        if (state == GameState.ACTIVE) {
            long red = countTeamPlayers(Team.RED);
            long blue = countTeamPlayers(Team.BLUE);

            if (red == 0) {
                endGame(Team.BLUE);
            } else if (blue == 0) {
                endGame(Team.RED);
            }
        }
    }

    private long countTeamPlayers(Team team) {
        return gamePlayers.stream()
                .filter(uuid -> playerTeams.get(uuid) == team)
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .count();
    }

    private void broadcastToPlayers(String message) {
        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(Component.text(message));
            }
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public boolean isGameActive() {
        return state == GameState.ACTIVE || state == GameState.COUNTDOWN;
    }

    public boolean isPlayerInGame(UUID uuid) {
        return gamePlayers.contains(uuid);
    }

    public Team getPlayerTeam(UUID uuid) {
        return playerTeams.get(uuid);
    }

    public GameState getState() {
        return state;
    }

    public int getPlayerCount() {
        return gamePlayers.size();
    }

    public long getTeamCount(Team team) {
        return countTeamPlayers(team);
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public Kit getPlayerKit(UUID uuid) {
        return playerKits.get(uuid);
    }
}
