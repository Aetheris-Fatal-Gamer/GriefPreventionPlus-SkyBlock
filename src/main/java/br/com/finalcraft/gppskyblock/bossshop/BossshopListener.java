package br.com.finalcraft.gppskyblock.bossshop;

import br.com.finalcraft.gppskyblock.placeholders.PlaceHolderIntegration;
import org.black_ixx.bossshop.events.BSDisplayItemEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

class BossshopListener implements Listener {

	@EventHandler
	void onBossShopInventoryOpen(BSDisplayItemEvent event) {
		if (event.getShopItem() != null && event.getShopItem().getName() != null && event.getShopItem().getName().equalsIgnoreCase("IslandLock")){
			if (PlaceHolderIntegration.playersIslandIsPublic(event.getPlayer())) {
				ItemStack itemStack = event.getShop().getItem("IslandLockPublic").getItem();
				event.getShopItem().setItem(itemStack,true);
			}else {
				ItemStack itemStack = event.getShop().getItem("IslandLockPrivate").getItem();
				event.getShopItem().setItem(itemStack,true);
			}

		}
	}

}
