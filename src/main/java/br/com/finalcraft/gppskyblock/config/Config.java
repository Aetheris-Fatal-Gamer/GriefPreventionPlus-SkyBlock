package br.com.finalcraft.gppskyblock.config;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
	public String dbHostname, dbUsername, dbPassword, dbDatabase, worldName, schematic;
	public int nextRegion, radius, yLevel, tpCountdown;
	public boolean deleteRegion;
	public Biome defaultBiome;
	public List<Biome> allowedBiomes = new ArrayList<Biome>();
	
	private JavaPlugin instance;
	
	public Config(JavaPlugin instance) {
		this.instance = instance;
		instance.saveDefaultConfig();
		instance.reloadConfig();
		
		this.worldName=instance.getConfig().getString("WorldName");
		this.schematic=instance.getConfig().getString("Schematic");
		saveResource("default.schematic");
		File schematicFile = new File(instance.getDataFolder(), schematic+".schematic");
		if (!schematicFile.exists()) {
			instance.getLogger().severe("Island schematic file \""+schematic+".schematic\" doesn't exist!");
		}
		
		this.radius = instance.getConfig().getInt("Radius");
		if (this.radius>255) {
			this.radius=255;
		} else if (this.radius<10) {
			this.radius=10;
		}
		
		this.yLevel=instance.getConfig().getInt("YLevel");
		if (this.yLevel<1 || this.yLevel>255) {
			this.yLevel=64;
		}
		
		String biomeName = instance.getConfig().getString("DefaultBiome", "UNCHANGED");
		if (!biomeName.equals("UNCHANGED")) {
			defaultBiome = Biome.valueOf(biomeName);
			if (defaultBiome == null) {
				instance.getLogger().warning("Unknown default biome \""+biomeName+"\"");
			}
		}
		
		this.dbHostname=instance.getConfig().getString("MySQL.Hostname");
		this.dbUsername=instance.getConfig().getString("MySQL.Username");
		this.dbPassword=instance.getConfig().getString("MySQL.Password");
		this.dbDatabase=instance.getConfig().getString("MySQL.Database");
		
		this.deleteRegion=instance.getConfig().getBoolean("DeleteRegion", true);
		
		allowedBiomes.clear();
		for (String biomeString : instance.getConfig().getStringList("AllowedBiomes")) {
			try {
				Biome biome = Biome.valueOf(biomeString);
				allowedBiomes.add(biome);
			}catch (IllegalArgumentException e){
				instance.getLogger().warning("Skipping unknown allowed biome \""+biomeString+"\"");
			}
		}
		
		this.tpCountdown=instance.getConfig().getInt("TPCountdown", 5);
		
		// Data
		FileConfiguration data = YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data.yml"));
		if (data==null) {
			instance.getLogger().severe("There was an error while loading data.yml!");
		} else {
			int nextRegionX=data.getInt("NextRegion.X", -1);
			if (nextRegionX != -1) {
				int nextRegionZ=data.getInt("NextRegion.Z", -1);
				this.nextRegion = nextRegionCalc(nextRegionX, nextRegionZ) + 1;
				this.saveData();
			} else {
				this.nextRegion=data.getInt("NextRegion", 0);
			}
		}
	}
	
	public void saveData() {
		File file = new File(instance.getDataFolder(), "data.yml");
		FileConfiguration data = YamlConfiguration.loadConfiguration(file);
		if (data==null) {
			instance.getLogger().severe("There was an error while saving data.yml!");
		} else {
			data.set("NextRegion", this.nextRegion);
			try {
				data.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	void saveResource(String name) {
		if (!new File(instance.getDataFolder(), name).exists()) {
			instance.saveResource(name, false);
		}
	}
	
	public static int[] nextRegionCalc(int nextRegion) {
		return new int[] { 1 + ((nextRegion * 3) % 1350), 1 + (((nextRegion * 3) / 1350) * 3) };
	}

	public static int nextRegionCalc(int x, int z) {
		return ((x - 1) / 3) + (((z - 1) * 1350) / 9);
	}
	
	public int[] nextRegion() {
		return nextRegionCalc(this.nextRegion);
	}
}
