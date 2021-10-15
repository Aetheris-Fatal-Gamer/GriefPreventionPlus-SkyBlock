package br.com.finalcraft.gppskyblock.config.playerdata;

import br.com.finalcraft.evernifecore.config.playerdata.PDSection;
import br.com.finalcraft.evernifecore.config.playerdata.PlayerData;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;

public class SBPlayerData extends PDSection {

    public Island getIsland(){
        return GPPSkyBlock.getInstance().getDataStore().getIsland(this.getPlayerData().getUniqueId());
    }

    public SBPlayerData(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void savePDSection() {

    }

}
