package com.gtarace.manager;

import com.gtarace.GTARacePlugin;
import com.gtarace.model.Checkpoint;
import com.gtarace.model.RaceState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class RaceManager {

    private final GTARacePlugin plugin;
    private final TrackManager trackManager;
    private final VehicleManager vehicleManager;
    private final PowerUpManager powerUpManager;

    private RaceState state = RaceState.IDLE;
    private final Set<UUID> racePlayers = new LinkedHashSet<>();
    private final Map<UUID, Integer> playerCheckpoints = new HashMap<>();
    private final Map<UUID, Integer> playerLaps = new HashMap<>();
    private final List<UUID> finishOrder = new ArrayList<>();
    private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();
    private final Map<UUID, Location> lastCheckpointLocation = new HashMap<>();

    private BukkitTask countdownTask;
    private BukkitTask hudTask;
    private BukkitTask checkpointCheckTask;

    private int totalLaps;
    private int respawnDelaySeconds;

    public RaceManager(GTARacePlugin plugin, TrackManager trackManager,
                       VehicleManager vehicleManager, PowerUpManager powerUpManager) {
        this.plugin = plugin;
        this.trackManager = trackManager;
        this.vehicleManager = vehicleManager;
        this.powerUpManager = powerUpManager;
    }

    public boolean addPlayer(Player player) {
        if (state != RaceState.IDLE && state != RaceState.COUNTDOWN) return false;
        int maxPlayers = plugin.getConfig().getInt("race.max-players", 8);
        if (racePlayers.size() >= maxPlayers) return false;
        return racePlayers.add(player.getUniqueId());
    }

    public boolean removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!racePlayers.contains(uuid)) return false;
        cancelRespawnTask(uuid);
        vehicleManager.removeVehicle(uuid);
        racePlayers.remove(uuid);
        playerCheckpoints.remove(uuid);
        playerLaps.remove(uuid);
        lastCheckpointLocation.remove(uuid);
        return true;
    }

    public boolean startRace() {
        if (state != RaceState.IDLE) return false;

        int minPlayers = plugin.getConfig().getInt("race.min-players", 1);
        if (racePlayers.size() < minPlayers) return false;

        if (trackManager.getStartLocation() == null) return false;
        if (trackManager.getFinishLocation() == null) return false;

        state = RaceState.COUNTDOWN;
        totalLaps = plugin.getConfig().getInt("race.laps", 3);
        respawnDelaySeconds = plugin.getConfig().getInt("race.respawn-delay-seconds", 3);

        int countdownSeconds = plugin.getConfig().getInt("race.countdown-seconds", 5);

        // Teleport players to start and spawn boats (but don't start speed yet)
        Location start = trackManager.getStartLocation();
        float yaw = start.getYaw();
        double perpX = -Math.sin(Math.toRadians(yaw));
        double perpZ = Math.cos(Math.toRadians(yaw));
        int playerCount = 0;
        for (UUID uuid : racePlayers) {
            playerCount++;
        }
        int idx = 0;
        for (UUID uuid : racePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            double offsetFromCenter = (idx - (playerCount - 1) / 2.0) * 2.0;
            Location spawnLoc = start.clone().add(perpX * offsetFromCenter, 0, perpZ * offsetFromCenter);
            p.teleport(spawnLoc);
            vehicleManager.spawnVehicle(p, spawnLoc);
            playerCheckpoints.put(uuid, 0);
            playerLaps.put(uuid, 0);
            lastCheckpointLocation.put(uuid, spawnLoc);
            idx++;
        }

        vehicleManager.startFreezeTask();

        countdownTask = new BukkitRunnable() {
            int seconds = countdownSeconds;

            @Override
            public void run() {
                if (seconds <= 0) {
                    cancel();
                    beginRace();
                    return;
                }

                NamedTextColor color = switch (seconds) {
                    case 3 -> NamedTextColor.RED;
                    case 2 -> NamedTextColor.YELLOW;
                    case 1 -> NamedTextColor.GREEN;
                    default -> NamedTextColor.GOLD;
                };

                for (UUID uuid : racePlayers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.showTitle(Title.title(
                                Component.text(String.valueOf(seconds), color, TextDecoration.BOLD),
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

    private void beginRace() {
        state = RaceState.ACTIVE;

        vehicleManager.stopFreezeTask();
        vehicleManager.startSpeedBoostTask();
        powerUpManager.spawnAllPickups(trackManager.getPowerUpSpawns());
        powerUpManager.startTasks(racePlayers);

        for (UUID uuid : racePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(
                        Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                ));
                p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            }
        }

        broadcastToPlayers(plugin.getMessagePrefix() + plugin.getMessage("race-started"));
        startHudTask();
        startCheckpointCheckTask();
    }

    private void startHudTask() {
        hudTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<UUID> positions = calculatePositions();

                for (int i = 0; i < positions.size(); i++) {
                    UUID uuid = positions.get(i);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;

                    int lap = playerLaps.getOrDefault(uuid, 0) + 1;
                    int position = i + 1;
                    String healthBar = vehicleManager.getHealthBar(uuid);
                    String powerUp = powerUpManager.hasPowerUp(uuid)
                            ? " §7| " + powerUpManager.getHeldPowerUp(uuid).getColoredName()
                            : "";
                    String boost = vehicleManager.hasSpeedBoost(uuid) ? " §b[BOOST]" : "";
                    String shield = vehicleManager.hasShield(uuid) ? " §9[SHIELD]" : "";

                    String hud = "§e" + getPositionString(position) + " §7| §eLap " + Math.min(lap, totalLaps) + "/" + totalLaps
                            + " §7| " + healthBar + powerUp + boost + shield;
                    p.sendActionBar(Component.text(hud));
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void startCheckpointCheckTask() {
        checkpointCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != RaceState.ACTIVE) return;
                List<Checkpoint> checkpoints = trackManager.getCheckpoints();

                for (UUID uuid : new HashSet<>(racePlayers)) {
                    if (finishOrder.contains(uuid)) continue;
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;

                    // Use vehicle location if riding, otherwise player location
                    Location loc;
                    org.bukkit.entity.Boat boat = vehicleManager.getPlayerBoat(uuid);
                    if (boat != null && !boat.isDead()) {
                        loc = boat.getLocation();
                    } else {
                        loc = player.getLocation();
                    }

                    int currentCp = playerCheckpoints.getOrDefault(uuid, 0);
                    int currentLap = playerLaps.getOrDefault(uuid, 0);

                    if (currentCp < checkpoints.size()) {
                        Checkpoint next = checkpoints.get(currentCp);
                        if (next.isInside(loc)) {
                            playerCheckpoints.put(uuid, currentCp + 1);
                            lastCheckpointLocation.put(uuid, loc.clone());

                            String msg = plugin.getMessage("checkpoint-reached")
                                    .replace("%number%", String.valueOf(currentCp + 1))
                                    .replace("%total%", String.valueOf(checkpoints.size()));
                            player.sendMessage(Component.text(plugin.getMessagePrefix() + msg));
                            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
                        }
                    } else if (trackManager.isAtFinish(loc)) {
                        int newLap = currentLap + 1;
                        if (newLap >= totalLaps) {
                            playerFinished(player);
                        } else {
                            playerLaps.put(uuid, newLap);
                            playerCheckpoints.put(uuid, 0);
                            lastCheckpointLocation.put(uuid, trackManager.getStartLocation());

                            String msg = plugin.getMessage("lap-completed")
                                    .replace("%lap%", String.valueOf(newLap))
                                    .replace("%total%", String.valueOf(totalLaps));
                            player.sendMessage(Component.text(plugin.getMessagePrefix() + msg));
                            player.showTitle(Title.title(
                                    Component.text("Lap " + (newLap + 1), NamedTextColor.GOLD, TextDecoration.BOLD),
                                    Component.empty(),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                            ));
                            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playerFinished(Player player) {
        UUID uuid = player.getUniqueId();
        if (finishOrder.contains(uuid)) return;
        finishOrder.add(uuid);

        int position = finishOrder.size();
        String suffix = getOrdinalSuffix(position);

        String msg = plugin.getMessage("race-finished")
                .replace("%player%", player.getName())
                .replace("%position%", String.valueOf(position))
                .replace("%suffix%", suffix);
        broadcastToPlayers(plugin.getMessagePrefix() + msg);

        player.showTitle(Title.title(
                Component.text(getPositionString(position), NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("You finished!", NamedTextColor.GREEN),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        vehicleManager.removeVehicle(uuid);

        int activeRacers = 0;
        for (UUID id : racePlayers) {
            if (!finishOrder.contains(id) && Bukkit.getPlayer(id) != null) {
                activeRacers++;
            }
        }

        if (activeRacers <= 0 || finishOrder.size() >= racePlayers.size()) {
            endRace();
        }
    }

    public void handleCarDestroyed(UUID playerId) {
        if (state != RaceState.ACTIVE) return;
        if (!racePlayers.contains(playerId)) return;
        if (finishOrder.contains(playerId)) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        player.setGameMode(org.bukkit.GameMode.SPECTATOR);

        BukkitTask task = new BukkitRunnable() {
            int seconds = respawnDelaySeconds;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || state != RaceState.ACTIVE) {
                    cancel();
                    respawnTasks.remove(playerId);
                    return;
                }

                if (seconds <= 0) {
                    p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    Location respawnLoc = lastCheckpointLocation.getOrDefault(playerId, trackManager.getStartLocation());
                    vehicleManager.respawnVehicle(p, respawnLoc);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    cancel();
                    respawnTasks.remove(playerId);
                    return;
                }

                p.showTitle(Title.title(
                        Component.text("§c§lWRECKED!"),
                        Component.text("§eRespawning in §b" + seconds + "§e..."),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                ));
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        respawnTasks.put(playerId, task);
    }

    private void endRace() {
        if (state == RaceState.IDLE || state == RaceState.ENDING) return;
        state = RaceState.ENDING;

        if (!finishOrder.isEmpty()) {
            Player winner = Bukkit.getPlayer(finishOrder.get(0));
            String winnerName = winner != null ? winner.getName() : "Unknown";
            String msg = plugin.getMessage("race-ended").replace("%player%", winnerName);
            broadcastToPlayers(plugin.getMessagePrefix() + msg);
        }

        // Show final standings
        broadcastToPlayers("§6§l=== Final Standings ===");
        for (int i = 0; i < finishOrder.size(); i++) {
            Player p = Bukkit.getPlayer(finishOrder.get(i));
            String name = p != null ? p.getName() : "Unknown";
            broadcastToPlayers("§e" + getPositionString(i + 1) + " §7- §f" + name);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, 100L);
    }

    public void stopRace() {
        if (state == RaceState.IDLE) return;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        broadcastToPlayers(plugin.getMessagePrefix() + plugin.getMessage("race-stopped"));
        cleanup();
    }

    private void cleanup() {
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
        if (checkpointCheckTask != null) {
            checkpointCheckTask.cancel();
            checkpointCheckTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();

        vehicleManager.stopFreezeTask();
        vehicleManager.stopSpeedBoostTask();
        vehicleManager.removeAllVehicles();
        powerUpManager.cleanup();

        for (UUID uuid : racePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            }
        }

        racePlayers.clear();
        playerCheckpoints.clear();
        playerLaps.clear();
        finishOrder.clear();
        lastCheckpointLocation.clear();
        state = RaceState.IDLE;
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelRespawnTask(uuid);
        vehicleManager.removeVehicle(uuid);
        racePlayers.remove(uuid);
        playerCheckpoints.remove(uuid);
        playerLaps.remove(uuid);
        lastCheckpointLocation.remove(uuid);

        broadcastToPlayers(plugin.getMessagePrefix() +
                plugin.getMessage("player-left").replace("%player%", player.getName()));
    }

    private void cancelRespawnTask(UUID uuid) {
        BukkitTask task = respawnTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private List<UUID> calculatePositions() {
        List<UUID> active = new ArrayList<>();
        for (UUID uuid : racePlayers) {
            if (!finishOrder.contains(uuid)) active.add(uuid);
        }
        active.sort((a, b) -> {
            int lapA = playerLaps.getOrDefault(a, 0);
            int lapB = playerLaps.getOrDefault(b, 0);
            if (lapA != lapB) return lapB - lapA;
            int cpA = playerCheckpoints.getOrDefault(a, 0);
            int cpB = playerCheckpoints.getOrDefault(b, 0);
            return cpB - cpA;
        });

        List<UUID> result = new ArrayList<>(finishOrder);
        result.addAll(active);
        return result;
    }

    private String getPositionString(int position) {
        return position + getOrdinalSuffix(position);
    }

    private String getOrdinalSuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private void broadcastToPlayers(String message) {
        for (UUID uuid : racePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(Component.text(message));
            }
        }
    }

    public boolean isRaceActive() {
        return state == RaceState.ACTIVE || state == RaceState.COUNTDOWN;
    }

    public boolean isPlayerInRace(UUID uuid) {
        return racePlayers.contains(uuid);
    }

    public RaceState getState() {
        return state;
    }

    public int getPlayerCount() {
        return racePlayers.size();
    }

    public Set<UUID> getRacePlayers() {
        return Collections.unmodifiableSet(racePlayers);
    }
}
