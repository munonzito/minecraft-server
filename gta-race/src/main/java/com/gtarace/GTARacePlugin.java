package com.gtarace;

import com.gtarace.command.CommandHandler;
import com.gtarace.listener.RaceListener;
import com.gtarace.manager.PowerUpManager;
import com.gtarace.manager.RaceManager;
import com.gtarace.manager.TrackBuilder;
import com.gtarace.manager.TrackManager;
import com.gtarace.manager.VehicleManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GTARacePlugin extends JavaPlugin {

    private TrackManager trackManager;
    private TrackBuilder trackBuilder;
    private VehicleManager vehicleManager;
    private PowerUpManager powerUpManager;
    private RaceManager raceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        trackManager = new TrackManager(this);
        trackBuilder = new TrackBuilder(this, trackManager);
        vehicleManager = new VehicleManager(this);
        powerUpManager = new PowerUpManager(this, vehicleManager);
        raceManager = new RaceManager(this, trackManager, vehicleManager, powerUpManager);

        getServer().getPluginManager().registerEvents(
                new RaceListener(this, raceManager, vehicleManager, powerUpManager), this);

        CommandHandler commandHandler = new CommandHandler(this, raceManager, trackManager, trackBuilder, vehicleManager, powerUpManager);
        getCommand("race").setExecutor(commandHandler);
        getCommand("race").setTabCompleter(commandHandler);

        getLogger().info("GTARace has been enabled!");
    }

    @Override
    public void onDisable() {
        if (raceManager != null && raceManager.isRaceActive()) {
            raceManager.stopRace();
        }
        getLogger().info("GTARace has been disabled!");
    }

    public String getMessagePrefix() {
        return colorize(getConfig().getString("messages.prefix", "&6[GTARace] &r"));
    }

    public String getMessage(String key) {
        return colorize(getConfig().getString("messages." + key, "Missing message: " + key));
    }

    public static String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
