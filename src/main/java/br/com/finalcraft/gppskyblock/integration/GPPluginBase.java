package br.com.finalcraft.gppskyblock.integration;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.config.datastore.DataStore;
import br.com.finalcraft.gppskyblock.integration.wrapper.griefdefender.WrGDPluginBase;
import br.com.finalcraft.gppskyblock.integration.wrapper.griefpreventionplus.WrGPPPluginBase;
import org.bukkit.entity.Player;

import java.io.File;

public abstract class GPPluginBase {

    private static GPPluginBase instance;

    public static GPPluginBase getInstance() {
        return instance;
    }

    public static GPPluginBase intialize(){
        try {
            Class.forName("net.kaikk.mc.gpp.Claim");
            GPPSkyBlock.info("Hokking to GriefPreventionPlus");
            return (instance = new WrGPPPluginBase());
        }catch (Exception ignored){
        }
        try {
            Class.forName("com.griefdefender.api.claim.Claim");
            GPPSkyBlock.info("Hokking to GriefDefender");
            return (instance = new WrGDPluginBase());
        }catch (Exception ignored){
        }
        throw new RuntimeException("Nor GriefPreventionPlus or GriefDenfenrder were found!");
    }

    public abstract DataStore setupDataStore() throws Exception;

    public abstract void registerEventListeners() throws Exception;

    public abstract boolean fireClaimDeleteEvent(IClaim iClaim, Player player);

    public abstract void setRadius(Island island, int radius);

    public abstract void assyncRestoreIsland(Island island, File schematicFile);
}
