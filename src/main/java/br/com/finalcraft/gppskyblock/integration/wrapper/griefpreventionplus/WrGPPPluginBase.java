package br.com.finalcraft.gppskyblock.integration.wrapper.griefpreventionplus;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.config.datastore.DataStore;
import br.com.finalcraft.gppskyblock.config.datastore.gpp.DataStoreGPPMysql;
import br.com.finalcraft.gppskyblock.config.datastore.gpp.DataStoreGPPYML;
import br.com.finalcraft.gppskyblock.integration.GPPluginBase;
import br.com.finalcraft.gppskyblock.integration.IClaim;
import br.com.finalcraft.gppskyblock.listeners.EventListenerGPP;
import br.com.finalcraft.gppskyblock.tasks.ResetIslandThread;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.DataStoreMySQL;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class WrGPPPluginBase extends GPPluginBase {

    @Override
    public boolean fireClaimDeleteEvent(IClaim iClaim, Player player) {
        return false;
    }

    @Override
    public DataStore setupDataStore() throws Exception{
        if (GriefPreventionPlus.getInstance().getDataStore() instanceof DataStoreMySQL){
            GPPSkyBlock.info("Using DataStoreGPP-MySQL");
            return new DataStoreGPPMysql(GPPSkyBlock.getInstance());
        }else {
            GPPSkyBlock.info("Using DataStoreGPP-YML");
            return new DataStoreGPPYML(GPPSkyBlock.getInstance());
        }
    }

    @Override
    public void registerEventListeners() throws Exception {
        Bukkit.getServer().getPluginManager().registerEvents(new EventListenerGPP(GPPSkyBlock.getInstance()), GPPSkyBlock.getInstance());
    }

    @Override
    public void setRadius(Island island, int radius) {
        Location center = island.getCenter();
        int size = island.getClaim().getArea();
        WrGPPClaim claimWrapper = (WrGPPClaim) island.getClaim();
        Claim claim = claimWrapper.claim;
        GriefPreventionPlus.getInstance().getDataStore().resizeClaim(claim, center.getBlockX()-radius, center.getBlockZ()-radius, center.getBlockX()+radius, center.getBlockZ()+radius, null);
        PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(island.getOwnerId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()+(claim.getArea()-size));
        GriefPreventionPlus.getInstance().getDataStore().savePlayerData(island.getOwnerId(), playerData);
    }

    @Override
    public void assyncRestoreIsland(Island island, File schematicFile) {
        new ResetIslandThread(island, schematicFile);
    }

    @Override
    public void transferIsland(Island island, UUID newOwnerUUID) {
        WrGPPClaim claimWrapper = (WrGPPClaim) island.getClaim();
        Claim claim = claimWrapper.claim;
        try {
            GriefPreventionPlus.getInstance().getDataStore().changeClaimOwner(claim, newOwnerUUID);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
