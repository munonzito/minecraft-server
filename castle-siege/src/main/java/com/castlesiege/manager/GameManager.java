package com.castlesiege.manager;

import com.castlesiege.CastleSiegePlugin;
import com.castlesiege.model.GameState;
import com.castlesiege.model.SiegeKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class GameManager {

    private final CastleSiegePlugin plugin;
    private final CastleManager castleManager;
    private final KitManager kitManager;
    private final WaveManager waveManager;
    private final ShopManager shopManager;

    private GameState state = GameState.IDLE;
    private final Set<UUID> gamePlayers = new LinkedHashSet<>();
    private final Map<UUID, SiegeKit> playerKits = new HashMap<>();
    private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();

    private BukkitTask countdownTask;
    private BukkitTask waveCheckTask;
    private BukkitTask hudTask;

    public GameManager(CastleSiegePlugin plugin, CastleManager castleManager,
                       KitManager kitManager, WaveManager waveManager, ShopManager shopManager) {
        this.plugin = plugin;
        this.castleManager = castleManager;
        this.kitManager = kitManager;
        this.waveManager = waveManager;
        this.shopManager = shopManager;
    }

    private long previousTime = -1;
    private boolean previousGameRule;

    public boolean startSiege(Location center) {
        if (state != GameState.IDLE) return false;

        castleManager.buildCastle(center);
        castleManager.spawnVillagers();

        for (Player p : Bukkit.getOnlinePlayers()) {
            gamePlayers.add(p.getUniqueId());
            playerKits.put(p.getUniqueId(), SiegeKit.KNIGHT);
        }

        if (gamePlayers.isEmpty()) return false;

        // Set permanent night
        World world = center.getWorld();
        if (world != null) {
            previousTime = world.getTime();
            previousGameRule = Boolean.TRUE.equals(world.getGameRuleValue(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE));
            world.setTime(18000L);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        state = GameState.COUNTDOWN;
        int countdownSeconds = plugin.getConfig().getInt("game.countdown-seconds", 15);

        Location spawn = castleManager.getSpawnLocation();
        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            sendKitSelection(p);
        }

        countdownTask = new BukkitRunnable() {
            int seconds = countdownSeconds;

            @Override
            public void run() {
                if (seconds <= 0) {
                    cancel();
                    startFirstWave();
                    return;
                }

                if (seconds <= 5 || seconds == 10 || seconds == 15) {
                    for (UUID uuid : gamePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.showTitle(Title.title(
                                    Component.text(String.valueOf(seconds), NamedTextColor.GOLD, TextDecoration.BOLD),
                                    Component.text("Choose your kit!", NamedTextColor.YELLOW),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                            ));
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void sendKitSelection(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + "§e§lSelect your kit:"));

        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[Knight]", NamedTextColor.GOLD, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/siege kit knight")))
                        .append(Component.text(" - Diamond Sword, Iron Armor", NamedTextColor.GRAY))
        );
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[Archer]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/siege kit archer")))
                        .append(Component.text(" - Power II Bow, Leather Armor", NamedTextColor.GRAY))
        );
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[Magician]", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/siege kit magician")))
                        .append(Component.text(" - Splash Potions, Chain Armor", NamedTextColor.GRAY))
        );
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[Tank]", NamedTextColor.GRAY, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/siege kit tank")))
                        .append(Component.text(" - Shield, Diamond Chest, 30 HP", NamedTextColor.GRAY))
        );
        player.sendMessage(Component.text(""));
    }

    public void setPlayerKit(Player player, SiegeKit kit) {
        if (!gamePlayers.contains(player.getUniqueId())) return;
        if (state != GameState.COUNTDOWN && state != GameState.WAVE_BREAK) return;
        playerKits.put(player.getUniqueId(), kit);
        String msg = plugin.getMessage("kit-selected").replace("%kit%", kit.getColoredName());
        player.sendMessage(CastleSiegePlugin.deserialize(plugin.getMessagePrefix() + msg));
    }

    private void startFirstWave() {
        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            SiegeKit kit = playerKits.getOrDefault(uuid, SiegeKit.KNIGHT);
            kitManager.applyKit(p, kit);
            p.teleport(castleManager.getSpawnLocation());
        }
        startNextWave();
    }

    public void startNextWave() {
        state = GameState.WAVE_ACTIVE;

        Location mobSpawn = castleManager.getMobSpawnLocation();
        int mobCount = waveManager.spawnWave(mobSpawn, castleManager.getAldeano());
        int wave = waveManager.getCurrentWave();

        String msg = plugin.getMessage("wave-starting")
                .replace("%wave%", String.valueOf(wave))
                .replace("%mobs%", String.valueOf(mobCount));
        broadcastToPlayers(plugin.getMessagePrefix() + msg);

        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(
                        Component.text("Wave " + wave, NamedTextColor.RED, TextDecoration.BOLD),
                        Component.text(mobCount + " mobs incoming!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
                ));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            }
        }

        startWaveCheckTask();
        startHudTask();
    }

    private void startWaveCheckTask() {
        if (waveCheckTask != null) waveCheckTask.cancel();
        waveCheckTask = new BukkitRunnable() {
            int retargetTicks = 0;
            @Override
            public void run() {
                if (state != GameState.WAVE_ACTIVE) {
                    cancel();
                    return;
                }
                if (waveManager.isWaveCleared()) {
                    cancel();
                    onWaveComplete();
                    return;
                }
                // Kill all Vexes near the Aldeano (large radius covers the throne room)
                Villager aldeano = castleManager.getAldeano();
                if (aldeano != null && !aldeano.isDead()) {
                    for (Entity nearby : aldeano.getNearbyEntities(20, 20, 20)) {
                        if (nearby instanceof Vex vex && !vex.isDead()) {
                            vex.setHealth(0);
                        }
                    }
                }

                retargetTicks++;
                if (retargetTicks % 5 == 0 && castleManager.getCastleCenter() != null) {
                    waveManager.retargetMobs(
                            castleManager.getCastleCenter().getWorld(),
                            castleManager.getAldeano());
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void onWaveComplete() {
        state = GameState.WAVE_BREAK;
        int wave = waveManager.getCurrentWave();
        int breakSeconds = plugin.getConfig().getInt("game.wave-break-seconds", 15);

        // Heal all players
        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getGameMode() != GameMode.SPECTATOR) {
                var maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
                double maxHp = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : 20.0;
                p.setHealth(maxHp);
                p.setFoodLevel(20);
                p.setSaturation(20f);
            }
        }

        String msg = plugin.getMessage("wave-complete")
                .replace("%wave%", String.valueOf(wave))
                .replace("%seconds%", String.valueOf(breakSeconds));
        broadcastToPlayers(plugin.getMessagePrefix() + msg);

        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(
                        Component.text("Wave " + wave + " Complete!", NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.text("Shop is open!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
                ));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        countdownTask = new BukkitRunnable() {
            int seconds = breakSeconds;

            @Override
            public void run() {
                if (state != GameState.WAVE_BREAK) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    cancel();
                    startNextWave();
                    return;
                }
                if (seconds <= 5) {
                    for (UUID uuid : gamePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.showTitle(Title.title(
                                    Component.text(String.valueOf(seconds), NamedTextColor.RED, TextDecoration.BOLD),
                                    Component.text("Next wave incoming!", NamedTextColor.YELLOW),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                            ));
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void onVillagerDeath() {
        if (state == GameState.IDLE || state == GameState.ENDING) return;
        state = GameState.ENDING;

        cancelAllTasks();

        // Cancel all respawn tasks first to prevent them from interfering
        for (UUID uuid : new ArrayList<>(respawnTasks.keySet())) {
            cancelRespawnTask(uuid);
        }

        int wave = waveManager.getCurrentWave();
        String msg = plugin.getMessage("game-over").replace("%waves%", String.valueOf(wave));
        broadcastToPlayers(plugin.getMessagePrefix() + msg);

        for (UUID uuid : gamePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(
                        Component.text("GAME OVER", NamedTextColor.RED, TextDecoration.BOLD),
                        Component.text("Survived " + wave + " waves!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
                ));
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, 100L);
    }

    public void handlePlayerDeath(Player player) {
        if (state != GameState.WAVE_ACTIVE) return;
        if (!gamePlayers.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        SiegeKit kit = playerKits.getOrDefault(uuid, SiegeKit.KNIGHT);
        int respawnDelay = plugin.getConfig().getInt("game.respawn-delay-seconds", 10);

        // Auto-respawn on next tick to skip the death screen, then switch to spectator
        new BukkitRunnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isDead()) {
                    p.spigot().respawn();
                }
                // Set spectator after respawn
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player p2 = Bukkit.getPlayer(uuid);
                        if (p2 != null) {
                            p2.setGameMode(GameMode.SPECTATOR);
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }.runTaskLater(plugin, 1L);

        BukkitTask task = new BukkitRunnable() {
            int seconds = respawnDelay;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || (state != GameState.WAVE_ACTIVE && state != GameState.WAVE_BREAK)) {
                    cancel();
                    respawnTasks.remove(uuid);
                    return;
                }
                if (seconds <= 0) {
                    p.setGameMode(GameMode.SURVIVAL);
                    kitManager.applyKit(p, kit);
                    p.teleport(castleManager.getSpawnLocation());
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    cancel();
                    respawnTasks.remove(uuid);
                    return;
                }
                p.showTitle(Title.title(
                        Component.text("You died!", NamedTextColor.RED),
                        Component.text("Respawning in " + seconds + " seconds", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                ));
                seconds--;
            }
        }.runTaskTimer(plugin, 3L, 20L);

        respawnTasks.put(uuid, task);
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelRespawnTask(uuid);
        gamePlayers.remove(uuid);
        playerKits.remove(uuid);

        if (state != GameState.IDLE && gamePlayers.isEmpty()) {
            stopSiege();
        }
    }

    public void stopSiege() {
        if (state == GameState.IDLE) return;

        cancelAllTasks();
        broadcastToPlayers(plugin.getMessagePrefix() + plugin.getMessage("game-stopped"));
        cleanup();
    }

    private void cleanup() {
        cancelAllTasks();
        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();

        // Restore time before cleaning up castle (which nulls castleCenter)
        if (castleManager.getCastleCenter() != null) {
            World world = castleManager.getCastleCenter().getWorld();
            if (world != null) {
                if (previousTime >= 0) {
                    world.setTime(previousTime);
                }
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, previousGameRule);
            }
            waveManager.removeAllMobs(world);
        }
        previousTime = -1;

        waveManager.reset();
        shopManager.reset();
        castleManager.cleanup();

        for (UUID uuid : gamePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(20.0);
                }
                player.setHealth(20.0);
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }

        gamePlayers.clear();
        playerKits.clear();
        state = GameState.IDLE;
    }

    private void cancelAllTasks() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (waveCheckTask != null) { waveCheckTask.cancel(); waveCheckTask = null; }
        if (hudTask != null) { hudTask.cancel(); hudTask = null; }
    }

    private void startHudTask() {
        if (hudTask != null) hudTask.cancel();
        hudTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.WAVE_ACTIVE) {
                    cancel();
                    return;
                }
                int wave = waveManager.getCurrentWave();
                int alive = waveManager.getAliveCount();
                String villagerHp = "";
                if (castleManager.getAldeano() != null && !castleManager.getAldeano().isDead()) {
                    int hp = (int) castleManager.getAldeano().getHealth();
                    int maxHp = (int) castleManager.getAldeano().getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                    villagerHp = " §7| §6Aldeano: §c" + hp + "/" + maxHp;
                }
                for (UUID uuid : new ArrayList<>(gamePlayers)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        int coins = shopManager.getCoins(uuid);
                        p.sendActionBar(CastleSiegePlugin.deserialize(
                                "§eWave §b" + wave + " §7| §eMobs: §c" + alive
                                        + villagerHp + " §7| §eCoins: §b" + coins
                        ));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void cancelRespawnTask(UUID uuid) {
        BukkitTask task = respawnTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void broadcastToPlayers(String message) {
        Component component = CastleSiegePlugin.deserialize(message);
        for (UUID uuid : new ArrayList<>(gamePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(component);
            }
        }
    }

    public boolean isGameActive() {
        return state != GameState.IDLE;
    }

    public boolean isPlayerInGame(UUID uuid) {
        return gamePlayers.contains(uuid);
    }

    public GameState getState() {
        return state;
    }

    public int getPlayerCount() {
        return gamePlayers.size();
    }
}
