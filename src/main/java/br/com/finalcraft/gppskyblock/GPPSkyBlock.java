package br.com.finalcraft.gppskyblock;

import br.com.finalcraft.gppskyblock.commands.CommandRegisterer;
import br.com.finalcraft.gppskyblock.config.Config;
import br.com.finalcraft.gppskyblock.config.datastore.DataStore;
import br.com.finalcraft.gppskyblock.integration.GPPluginBase;
import br.com.finalcraft.gppskyblock.placeholders.PlaceHolderIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GPPSkyBlock extends JavaPlugin {
	private static GPPSkyBlock instance;
	private Config config;
	private DataStore dataStore;
	private GPPluginBase pluginBase;

	public static void debug(String msg){
		instance.getLogger().info("[Debug] " + msg.replace("&","§"));
	}

	public static void info(String msg){
		instance.getLogger().info("[Info] " + msg);
	}

	@Override
	public void onEnable() {
		instance = this;
		config = new Config(this);

		this.pluginBase = GPPluginBase.intialize();

		try {
			initializeDataStore();

			pluginBase.registerEventListeners();

			CommandRegisterer.registerCommands(this);
			if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
				PlaceHolderIntegration.initialize();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initializeDataStore() throws Exception {
		dataStore = this.pluginBase.setupDataStore();
	}

	public static GPPSkyBlock getInstance() {
		return instance;
	}

	public Config config() {
		return config;
	}

	public DataStore getDataStore() {
		return dataStore;
	}
	
	public Location getSpawn() {
		return Bukkit.getWorld(this.config().worldName).getSpawnLocation();
	}

	public Config getPluginConfig() {
		return config;
	}

	public Island getIsland(UUID playerId){
		return dataStore.getIsland(playerId);
	}
}
