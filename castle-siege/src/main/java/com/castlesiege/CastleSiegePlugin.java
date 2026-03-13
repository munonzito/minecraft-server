package com.castlesiege;

import com.castlesiege.command.CommandHandler;
import com.castlesiege.listener.GameListener;
import com.castlesiege.manager.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public class CastleSiegePlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private CastleManager castleManager;
    private KitManager kitManager;
    private WaveManager waveManager;
    private ShopManager shopManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        castleManager = new CastleManager(this);
        kitManager = new KitManager();
        waveManager = new WaveManager(this);
        shopManager = new ShopManager(this);
        shopManager.setCastleManager(castleManager);
        gameManager = new GameManager(this, castleManager, kitManager, waveManager, shopManager);

        getServer().getPluginManager().registerEvents(
                new GameListener(this, gameManager, castleManager, waveManager, shopManager), this);

        CommandHandler commandHandler = new CommandHandler(this, gameManager);
        getCommand("siege").setExecutor(commandHandler);
        getCommand("siege").setTabCompleter(commandHandler);

        getLogger().info("CastleSiege has been enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.stopSiege();
        }
        getLogger().info("CastleSiege has been disabled!");
    }

    public String getMessagePrefix() {
        return colorize(getConfig().getString("messages.prefix", "&6[CastleSiege] &r"));
    }

    public String getMessage(String key) {
        return colorize(getConfig().getString("messages." + key, "Missing message: " + key));
    }

    public static String colorize(String message) {
        return message.replace("&", "\u00a7");
    }

    public static Component deserialize(String legacyText) {
        return LEGACY.deserialize(legacyText);
    }
}
