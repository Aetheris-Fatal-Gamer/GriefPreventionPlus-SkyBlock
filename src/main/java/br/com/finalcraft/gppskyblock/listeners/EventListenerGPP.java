package br.com.finalcraft.gppskyblock.listeners;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import net.kaikk.mc.gpp.events.ClaimCreateEvent;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent.Reason;
import net.kaikk.mc.gpp.events.ClaimOwnerTransfer;
import net.kaikk.mc.gpp.events.ClaimResizeEvent;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class EventListenerGPP implements Listener {
	private GPPSkyBlock instance;
	
	public EventListenerGPP(GPPSkyBlock instance) {
		this.instance = instance;
	}

	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	void onClaimDeleteMonitor(ClaimDeleteEvent event) {
		Island island = getIsland(event.getClaim());
		if (island != null) {
			if (event.getDeleteReason()!=Reason.EXPIRED && event.getDeleteReason()!=Reason.DELETE) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "Se você quer deletar a sua ilha use o comando \"/is delete\"!");
				return;
			}
			island.teleportEveryoneToSpawn();
			if (instance.config().deleteRegion) {
				island.deleteRegionFile();
			}
			try {
				instance.getDataStore().removeIsland(island);
				PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(event.getClaim().getOwnerID());
				playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()-(((instance.config().radius*2)+1)*2));
			} catch (Exception e) {
				e.printStackTrace();
			}
			instance.getLogger().info("Removed "+island.getOwnerName()+"'s island because the claim was deleted. Reason: "+event.getDeleteReason()+".");
		}
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimCreate(ClaimCreateEvent event) {
		if (event.getPlayer() == null) {
			return;
		}
		
		if (event.getPlayer().isOp()) {
			return;
		}
		
		if (event.getClaim().getParent() != null) {
			return;
		}
		
		if (!event.getClaim().getWorld().getName().equals(instance.config().worldName)) {
			if (!event.getPlayer().hasPermission("gppskyblock.claimoutsidemainworld")){
				event.setCancelled(true);
			}
			return;
		}

		event.setCancelled(true);
		event.setReason("You do not have permissions to create claims on the islands world.");
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimResize(ClaimResizeEvent event) {
		if (event.getPlayer() != null && isIsland(event.getClaim())) {
			event.setCancelled(true);
			if (event.getPlayer() != null) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode redefinir o tamanho dessa ilha. É uma ilha afinal das contas!");
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimOwnerTransfer(ClaimOwnerTransfer event) {
		if (isIsland(event.getClaim())){
			event.setCancelled(true);
			event.setReason("Esse claim é uma ilha! E as ilhas são Intransferíveis.");
		}
	}

	@EventHandler
	void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.getPlayer().hasPermission("gppskyblock.override") && isIslandWorld(event.getTo().getWorld()) && !isIslandWorld(event.getFrom().getWorld()) && !event.getTo().equals(Bukkit.getWorld(instance.config().worldName).getSpawnLocation())) {
			Claim claim = GriefPreventionPlus.getInstance().getDataStore().getClaimAt(event.getTo());
			if (claim==null) {
				event.getPlayer().teleport(Bukkit.getWorld(instance.config().worldName).getSpawnLocation()); // TODO
			}
		}
	}
	
	@EventHandler
	void onPlayerTeleport(PlayerPortalEvent event) {
		if (event.getCause()==TeleportCause.END_PORTAL && isIslandWorld(event.getFrom().getWorld())) {
			Location loc = event.getPortalTravelAgent().findPortal(new Location(event.getTo().getWorld(), 0, 64, 0));
			if (loc!=null) {
				event.setTo(loc);
				event.useTravelAgent(false);
			}
		}
	}
	
	boolean isIsland(Claim claim) {
		return getIsland(claim) != null;
	}
	
	Island getIsland(Claim claim) {
		if (!isIslandWorld(claim.getWorld())) {
			return null;
		}
		Island island = instance.getDataStore().getIsland(claim.getOwnerID());
		if (island.getClaim() == claim) {
			return island;
		}
		return null;
	}
	
	boolean isIslandWorld(World world) {
		return world.getName().equals(instance.config().worldName);
	}
}
