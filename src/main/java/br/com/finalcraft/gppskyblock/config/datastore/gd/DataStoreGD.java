package br.com.finalcraft.gppskyblock.config.datastore.gd;

import br.com.finalcraft.evernifecore.config.Config;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.config.datastore.DataStore;
import br.com.finalcraft.gppskyblock.integration.wrapper.griefdefender.WrGDClaim;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.claim.GDClaim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class DataStoreGD extends DataStore {

	public Config config;

	public DataStoreGD(GPPSkyBlock instance) throws Exception {
		super(instance);
		config = new Config(GPPSkyBlock.getInstance(), "IslandsData.yml");
		for (String claimUUIDString : config.getKeys("Islands")) {

			UUID ownerUUID = config.getUUID("Islands." + claimUUIDString + ".ownerUUID");
			UUID claimUUID = UUID.fromString(claimUUIDString);
			Location spawnLocation = config.getLocation("Islands." + claimUUIDString + ".spawnLocation");

			Island island = new Island(ownerUUID,new WrGDClaim((GDClaim) GriefDefender.getCore().getClaim(claimUUID)), spawnLocation);
			islands.put(ownerUUID, island);
		}
	}

	@Override
	public Island createIsland(UUID ownerUUID) throws Exception {
		if (instance.config().nextRegion > 1822500) {
			throw new Exception("Max amount of islands reached.");
		}
		int[] xz = instance.config().nextRegion();
		
		int bx = xz[0] << 9;
		int bz = xz[1] << 9;
		
		World world = Bukkit.getWorld(instance.config().worldName);

		GDClaim.ClaimBuilder claimBuilder = new GDClaim.ClaimBuilder();
		claimBuilder.world(world.getUID());
		claimBuilder.owner(ownerUUID);
		claimBuilder.requireClaimBlocks(false);
		claimBuilder.bounds(bx+255+instance.config().radius, bx+255-instance.config().radius, 0, 255, bz+255+instance.config().radius, bz+255-instance.config().radius);
		ClaimResult result = claimBuilder.build();
		if (!result.successful()) {
			throw new Exception(String.valueOf(result));
		}
		
		instance.config().nextRegion++;
		instance.config().saveData();

		Island island = new Island(ownerUUID, new WrGDClaim((GDClaim) result.getClaim().get()));
		try {
			this.addIsland(island);
		} catch (Exception e) {
			e.printStackTrace();
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
