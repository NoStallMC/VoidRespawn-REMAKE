package main.java.org.matejko.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Logger;

public class VoidRespawn extends JavaPlugin implements Listener, CommandExecutor {

    private Logger logger;  // Declare the logger field
    private File configFile;  // File reference for config

    @Override
    public void onEnable() {
        // Manually initialize the logger field with a custom name
        this.logger = Logger.getLogger("log");  // Assign a custom name to the logger

        // Log a message using the custom logger
        this.logger.info("VoidRespawn is now enabled!");

        // Register the event listener
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register the command
        this.getCommand("void").setExecutor(this);

        // Ensure the configuration file exists and load it
        ensureConfigExists();
    }

    @Override
    public void onDisable() {
        // Log a message when the plugin is disabled
        this.logger.info("VoidRespawn is now disabled!");
    }

    // Save a blank config if it doesn't exist
    private void ensureConfigExists() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        configFile = new File(this.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();  // Create the blank config file
                logger.info("Created config.yml as it didn't exist.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Manually read the configuration from the file
    private String readConfig(String worldName, String key) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Updated to check for the new "location" structure
                if (line.startsWith(worldName + ".location." + key)) {
                    return line.split(":")[1].trim();  // Return the value after the colon
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;  // Return null if key is not found
    }

    // Manually write to the configuration file
    private void writeConfig(String worldName, String key, String value) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            StringBuilder fileContent = new StringBuilder();
            String line;

            boolean keyExists = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(worldName + ".location." + key)) {
                    fileContent.append(worldName).append(".location.").append(key).append(": ").append(value).append("\n");
                    keyExists = true;  // Found the line, so replace it
                } else {
                    fileContent.append(line).append("\n");  // Copy other lines as they are
                }
            }
            reader.close();

            // If the key wasn't found, append the new value
            if (!keyExists) {
                fileContent.append(worldName).append(".location.").append(key).append(": ").append(value).append("\n");
            }

            // Write the updated content back to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
            writer.write(fileContent.toString());
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load teleport location from config.yml
    private Location getVoidRespawnLocation(String worldName) {
        // Manually read from the config file
        double x = Double.parseDouble(readConfig(worldName, "x"));
        double y = Double.parseDouble(readConfig(worldName, "y"));
        double z = Double.parseDouble(readConfig(worldName, "z"));
        float yaw = Float.parseFloat(readConfig(worldName, "yaw"));
        float pitch = Float.parseFloat(readConfig(worldName, "pitch"));

        // Return the location object
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    // Event to detect when a player falls into the void (Y < 0)
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player is falling into the void (Y coordinate < 0)
        if (player.getLocation().getY() < 0) {
            String worldName = player.getWorld().getName();
            Location respawnLocation = getVoidRespawnLocation(worldName);

            // If a valid respawn location is found, teleport the player
            if (respawnLocation != null) {
                player.teleport(respawnLocation);
            }
        }
    }

    // Command logic for /void setspawn
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("void")) {
            // Check if the sender is a player
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can execute this command.");
                return false;
            }

            Player player = (Player) sender;

            // Check if the player has the correct permission
            if (!player.hasPermission("voidrespawn.setspawn")) {
                player.sendMessage("You don't have permission to set the void respawn location.");
                return false;
            }

            // Check if the correct sub-command was used
            if (args.length != 1 || !args[0].equalsIgnoreCase("setspawn")) {
                return false;
            }

            // Get the player's location
            Location loc = player.getLocation();
            String worldName = loc.getWorld().getName();

            // Save the location (x, y, z, yaw, pitch) into the config
            writeConfig(worldName, "x", String.valueOf(loc.getX()));
            writeConfig(worldName, "y", String.valueOf(loc.getY()));
            writeConfig(worldName, "z", String.valueOf(loc.getZ()));
            writeConfig(worldName, "yaw", String.valueOf(loc.getYaw()));
            writeConfig(worldName, "pitch", String.valueOf(loc.getPitch()));

            // Provide feedback to the player
            player.sendMessage("Void respawn location set for world '" + worldName + "' at " +
                    "X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ() +
                    " Pitch: " + loc.getPitch() + " Yaw: " + loc.getYaw());

            return true;
        }
        return false;
    }
}
