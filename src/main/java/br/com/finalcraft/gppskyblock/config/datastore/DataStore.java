package br.com.finalcraft.gppskyblock.config.datastore;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class DataStore {
	public GPPSkyBlock instance;
	public Connection db = null;
	public Map<UUID, Island> islands = new HashMap<>();

	public int getTotalOfIslands(){
		return this.islands.size();
	}

	public ExecutorService executor = Executors.newSingleThreadExecutor();

	public DataStore(GPPSkyBlock instance){
		this.instance=instance;
	}

	public Island getIsland(UUID playerId) {
		return this.islands.get(playerId);
	}

	public abstract Island createIsland(UUID uuid) throws Exception;

    public abstract void addIsland(Island island) throws Exception;

	public abstract void removeIsland(Island island) throws Exception;

    public abstract void updateIsland(Island island) throws Exception;
}
