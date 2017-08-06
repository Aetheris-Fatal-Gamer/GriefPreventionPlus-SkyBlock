package net.kaikk.mc.gpp.skyblock;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import net.kaikk.mc.gpp.events.ClaimCreateEvent;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent.Reason;
import net.kaikk.mc.gpp.events.ClaimExitEvent;
import net.kaikk.mc.gpp.events.ClaimOwnerTransfer;
import net.kaikk.mc.gpp.events.ClaimResizeEvent;

class EventListener implements Listener {
	private GPPSkyBlock instance;
	
	EventListener(GPPSkyBlock instance) {
		this.instance = instance;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!player.isOnline() || player.getName() == null) {
			return;
		}
		
		if (instance.config().autoSpawn && !player.hasPlayedBefore()) {
			Island island = instance.dataStore().getIsland(player.getUniqueId());
			if (island==null) {
				try {
					island = instance.dataStore().createIsland(player.getUniqueId());
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
		
		if (isIslandWorld(player.getLocation().getWorld()) && GriefPreventionPlus.getInstance().getDataStore().getClaimAt(player.getLocation()) == null) {
			player.teleport(GPPSkyBlock.getInstance().getSpawn());
		}
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimExit(ClaimExitEvent event) {
		if (event.getPlayer().hasPermission("gppskyblock.override") || event.getPlayer().hasPermission("gppskyblock.leaveisland")) {
			return;
		}
		
		if (isIslandWorld(event.getFrom().getWorld()) && isIslandWorld(event.getTo().getWorld())) {
			event.getPlayer().sendMessage(ChatColor.RED+"You can't walk/fly out of your island!");
			Island island = getIsland(event.getClaim());
			if (island!=null) {
				event.getPlayer().teleport(island.getSpawn());
			} else {
				event.setCancelled(true);
			}
			return;
		}
	}
	
	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	void onClaimDeleteMonitor(ClaimDeleteEvent event) {
		Island island = getIsland(event.getClaim());
		if (island != null) {
			if (event.getDeleteReason()!=Reason.EXPIRED && event.getDeleteReason()!=Reason.DELETE) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "If you want to delete your island, use the \"/is delete\" command. Notice that this will permanently delete your island!");
				return;
			}
			island.teleportEveryoneToSpawn();
			if (instance.config().deleteRegion) {
				island.deleteRegionFile();
			}
			try {
				instance.dataStore().removeIsland(island);
				PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(event.getClaim().getOwnerID());
				playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()-(((instance.config().radius*2)+1)*2));
			} catch (SQLException e) {
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
			return;
		}
		
		event.setCancelled(true);
		event.setReason("You do not have permissions to create claims on the islands world.");
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimResize(ClaimResizeEvent event) {
		if (event.getPlayer()!=null && isIsland(event.getClaim())) {
			event.setCancelled(true);
			if (event.getPlayer()!=null) {
				event.getPlayer().sendMessage(ChatColor.RED+"You can't resize this claim. It's an island!");
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true) 
	void onClaimOwnerTransfer(ClaimOwnerTransfer event) {
		Island island = getIsland(event.getClaim());
		if (island != null) {
			Island is2 = instance.dataStore().getIsland(event.getNewOwnerUUID());
			if (is2 != null) {
				event.setCancelled(true);
				event.setReason("This claim is an island and the other player has an island already. The other player has to delete their island first.");
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR) 
	void onClaimOwnerTransferMonitor(ClaimOwnerTransfer event) {
		Island island = getIsland(event.getClaim());
		if (island != null) {
			Island newIsland = new Island(event.getNewOwnerUUID(), event.getClaim(), island.getSpawn());
			try {
				this.instance.dataStore().removeIsland(island);
				this.instance.dataStore().addIsland(newIsland);
			} catch (SQLException e) {
				instance.getLogger().severe("SQL exception while transferring island " + event.getClaim().getID() + "owner from " + event.getClaim().getOwnerName() + " to " + event.getNewOwnerUUID());
				if (event.getPlayer() != null) {
					event.getPlayer().sendMessage(ChatColor.RED + "WARNING! A severe error occurred while transferring the island! Contact your server administrator!");
				}
				e.printStackTrace();
			}
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
	
	@EventHandler
	void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction()==Action.RIGHT_CLICK_BLOCK && event.getPlayer().getItemInHand().getType()==Material.BUCKET && event.getClickedBlock().getType()==Material.OBSIDIAN && event.getPlayer().hasPermission("gppskyblock.lava")) {
			event.getClickedBlock().setType(Material.AIR);
			event.getPlayer().getItemInHand().setType(Material.LAVA_BUCKET);
		}
	}
	
	boolean isIsland(Claim claim) {
		Island island = getIsland(claim);
		if (island == null) {
			return false;
		}
		
		return island.getClaim() == claim;
	}
	
	Island getIsland(Claim claim) {
		if (!isIslandWorld(claim.getWorld())) {
			return null;
		}
		Island island = instance.dataStore().getIsland(claim.getOwnerID());
		if (island.getClaim() == claim) {
			return island;
		}
		return null;
	}
	
	boolean isIslandWorld(World world) {
		return world.getName().equals(instance.config().worldName);
	}
}
