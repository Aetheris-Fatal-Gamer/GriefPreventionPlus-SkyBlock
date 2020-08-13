package br.com.finalcraft.gppskyblock.config.datastore.gpp;

import br.com.finalcraft.evernifecore.config.Config;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.config.datastore.DataStore;
import br.com.finalcraft.gppskyblock.integration.wrapper.griefpreventionplus.WrGPPClaim;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimResult;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.UUID;

public class DataStoreGPPYML extends DataStore {

	public Config config;

	public DataStoreGPPYML(GPPSkyBlock instance) throws Exception {
		super(instance);
		config = new Config(GPPSkyBlock.getInstance(), "IslandsData.yml");
		for (String claimID : config.getKeys("Islands")) {

			UUID ownerUUID = config.getUUID("Islands." + claimID + ".ownerUUID");
			Integer claimIDInt = Integer.parseInt(claimID);
			Location spawnLocation = config.getLocation("Islands." + claimID + ".spawnLocation");

			Claim claim = GriefPreventionPlus.getInstance().getDataStore().getClaim(claimIDInt);
			islands.put(ownerUUID, new Island(ownerUUID, new WrGPPClaim(claim), spawnLocation));
		}
	}

	@Override
	public Island createIsland(UUID uuid) throws Exception {
		if (instance.config().nextRegion > 1822500) {
			throw new Exception("Max amount of islands reached.");
		}
		int[] xz = instance.config().nextRegion();

		int bx = xz[0] << 9;
		int bz = xz[1] << 9;

		World world = Bukkit.getWorld(instance.config().worldName);
		PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(uuid);
		playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()+(((instance.config().radius*2)+1)*2));
		ClaimResult result = GriefPreventionPlus.getInstance().getDataStore().newClaim(world.getUID(), bx+255-instance.config().radius, bz+255-instance.config().radius, bx+255+instance.config().radius, bz+255+instance.config().radius, uuid, null, null, null);
		GriefPreventionPlus.getInstance().getDataStore().savePlayerData(uuid, playerData);
		if (result.getResult()!= ClaimResult.Result.SUCCESS) {
			playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()-(((instance.config().radius*2)+1)*2));
			GriefPreventionPlus.getInstance().getDataStore().savePlayerData(uuid, playerData);
			throw new Exception(result.getReason());
		}

		instance.config().nextRegion++;
		instance.config().saveData();

		Island island = new Island(uuid, new WrGPPClaim(result.getClaim()));
		try {
			this.addIsland(island);
		} catch (Exception e) {
			e.printStackTrace();
			GriefPreventionPlus.getInstance().getDataStore().deleteClaim(result.getClaim());
			throw new Exception("data store issue.");
		}

		island.reset();

		return island;
	}

	@Override
	public void addIsland(Island island) throws Exception {
		this.islands.put(island.getOwnerId(), island);
		config.setValue("Islands." + island.getClaim().getID() + ".ownerUUID", island.getOwnerId());
		config.setValue("Islands." + island.getClaim().getID() + ".spawnLocation", island.getSpawn());
		config.save();
	}

	@Override
	public void removeIsland(Island island) throws Exception {
		this.islands.remove(island.getOwnerId());
		config.setValue("Islands." + island.getClaim().getID(), null);
		config.save();
	}

	@Override
	public void updateIsland(Island island) throws Exception {
		config.setValue("Islands." + island.getClaim().getID() + ".ownerUUID", island.getOwnerId());
		config.setValue("Islands." + island.getClaim().getID() + ".spawnLocation", island.getSpawn());
		config.save();
	}
}
