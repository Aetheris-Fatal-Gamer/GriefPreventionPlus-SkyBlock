package br.com.finalcraft.gppskyblock.placeholders;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlaceHolderIntegration extends EZPlaceholderHook {

    public static void initialize(){
        new PlaceHolderIntegration(GPPSkyBlock.getInstance(),"gppskyblock").hook();
    }

    public PlaceHolderIntegration(Plugin plugin, String identifier) {
        super(plugin, identifier);
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder) {

        switch (placeholder){
            case "total_ilhas":
                return totalOfIslands();
        }

        if (player == null){
            return "";
        }

        switch (placeholder){
            case "island_is_public":
                return islandIsPublic(player);
            case "island_is_public_command":
                if (playersIslandIsPublic(player)){
                    return "private";
                }
                return "public";
            case "island_radius":
                return islandRadius(player);
        }

        return null;
    }

    private String islandIsPublic(Player player){
        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null){
            return "&cVocê não possui uma ilha ainda!";
        }

        if (island.getClaim().isPublicEntryTrust()){
            return "Sim";
        }
        return "Não";
    }

    private String islandRadius(Player player){
        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null){
            return "&cVocê não possui uma ilha ainda!";
        }

        return ""+ island.getRadius();
    }

    public static boolean playersIslandIsPublic(Player player){

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island != null){
            if (island.getClaim().isPublicEntryTrust()){
                return true;
            }
        }

        return false;
    }

    private static String totalOfIslands(){
        return "" + GPPSkyBlock.getInstance().getDataStore().getTotalOfIslands();
    }
}
