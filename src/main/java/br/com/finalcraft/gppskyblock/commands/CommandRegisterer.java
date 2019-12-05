package br.com.finalcraft.gppskyblock.commands;

import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegisterer {

    public static void registerCommands(JavaPlugin pluginInstance) {
        pluginInstance.getCommand("island").setExecutor(new CMDIsland());
    }

}
