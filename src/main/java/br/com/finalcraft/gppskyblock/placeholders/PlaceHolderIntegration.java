package br.com.finalcraft.gppskyblock.placeholders;

import me.clip.placeholderapi.external.EZPlaceholderHook;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
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

    private static String islandIsPublic(Player player){
        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null){
            return "&cVocê não possui uma ilha ainda!";
        }

        if (island.getClaim().getPermission(GriefPreventionPlus.UUID0) == 16){
            return "Sim";
        }
        return "Não";
    }

    public static boolean playersIslandIsPublic(Player player){

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island != null){
            if (island.getClaim().getPermission(GriefPreventionPlus.UUID0) == 16){
                return true;
            }
        }

        return false;
    }

    private static String islandRadius(Player player){
        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null){
            return "&cVocê não possui uma ilha ainda!";
        }

        return ""+ island.getRadius();
    }

    private static String totalOfIslands(){
        return "" + GPPSkyBlock.getInstance().getDataStore().getTotalOfIslands();
    }
}
