package com.castledefense;

import com.castledefense.command.CommandHandler;
import com.castledefense.listener.GameListener;
import com.castledefense.manager.ArenaManager;
import com.castledefense.manager.GameManager;
import com.castledefense.manager.KitManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CastleDefensePlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arenaManager = new ArenaManager(this);
        kitManager = new KitManager();
        gameManager = new GameManager(this, arenaManager, kitManager);

        getServer().getPluginManager().registerEvents(new GameListener(this, gameManager, arenaManager), this);

        CommandHandler commandHandler = new CommandHandler(this, gameManager, arenaManager, kitManager);
        getCommand("castle").setExecutor(commandHandler);
        getCommand("castle").setTabCompleter(commandHandler);

        getLogger().info("CastleDefense has been enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.stopGame();
        }
        getLogger().info("CastleDefense has been disabled!");
    }

    public String getMessagePrefix() {
        return colorize(getConfig().getString("messages.prefix", "&6[CastleDefense] &r"));
    }

    public String getMessage(String key) {
        return colorize(getConfig().getString("messages." + key, "Missing message: " + key));
    }

    public static String colorize(String message) {
        return message.replace("&", "§");
    }
}
