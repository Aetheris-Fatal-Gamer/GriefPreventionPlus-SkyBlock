package br.com.finalcraft.gppskyblock.listeners;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.CreateClaimEvent;
import com.griefdefender.api.event.RemoveClaimEvent;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDTransferClaimEvent;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class EventListenerGD implements Listener {
	private GPPSkyBlock instance;

	public EventListenerGD(GPPSkyBlock instance) {
		this.instance = instance;
	}

	@Subscribe
	public void onClaimDeleteMonitor(RemoveClaimEvent event) {
		if (event.cancelled()){
			return;
		}
		Island island = getIsland(event.getClaim());
		if (island != null) {
			Player player = Bukkit.getPlayer(event.getClaim().getUniqueId());

			if ( !(event instanceof RemoveClaimEvent.Delete) && !(event instanceof RemoveClaimEvent.Expire)){
				event.cancelled(true);
				if (player != null && player.isOnline()){
					player.sendMessage(ChatColor.RED + "Se você quer deletar a sua ilha use o comando \"/is delete\"!");
				}
				return;
			}

			island.teleportEveryoneToSpawn();
			if (instance.config().deleteRegion) {
				island.deleteRegionFile();
			}
			try {
				instance.getDataStore().removeIsland(island);
			} catch (Exception e) {
				e.printStackTrace();
			}
			instance.getLogger().info("Removed "+island.getOwnerName()+"'s island because the claim was deleted.");
		}
	}

	@Subscribe
	public void onClaimCreate(CreateClaimEvent.Pre event) {
		if (event.cancelled()){
			return;
		}

		GDClaim gdClaim = (GDClaim) event.getClaim();

		if (gdClaim.getParent() != null) {
			return;
		}

		Player player = Bukkit.getPlayer(gdClaim.getOwnerUniqueId());

		if (player == null) {
			return;
		}

		if (player.isOp()) {
			return;
		}

		if (!gdClaim.getWorld().getName().equals(instance.config().worldName)) {
			if (!player.hasPermission("gppskyblock.claimoutsidemainworld")){
				event.cancelled(true);
			}
			return;
		}

		event.cancelled(true);
		player.sendMessage("You do not have permissions to create claims on the islands world.");
	}

	@Subscribe
	public void onClaimResize(ChangeClaimEvent.Resize event) {
		GDClaim gdClaim = (GDClaim) event.getClaim();
		if (isIsland(event.getClaim())) {
			event.cancelled(true);
			Player player = Bukkit.getPlayer(gdClaim.getOwnerUniqueId());
			if (player!=null) {
				player.sendMessage(ChatColor.RED+"Você não pode redefinir o tamanho dessa ilha. É uma ilha afinal das contas!");
			}
		}
	}

	@Subscribe
	public void onClaimOwnerTransfer(GDTransferClaimEvent event) {
		if (isIsland(event.getClaim())){
			event.cancelled(true);
			Player player = Bukkit.getPlayer(event.getClaim().getOwnerUniqueId());
			if (player != null){
				player.sendMessage("§cEsse claim é uma ilha! E as ilhas são Intransferíveis.");
			}
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.getPlayer().hasPermission("gppskyblock.override") && isIslandWorld(event.getTo().getWorld()) && !isIslandWorld(event.getFrom().getWorld()) && !event.getTo().equals(Bukkit.getWorld(instance.config().worldName).getSpawnLocation())) {
			Claim claim = GriefDefender.getCore().getClaimManager(event.getPlayer().getWorld().getUID()).getClaimAt(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());
			if (claim == null) {
				event.getPlayer().teleport(Bukkit.getWorld(instance.config().worldName).getSpawnLocation()); // TODO
			}
		}
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerPortalEvent event) {
		if (event.getCause()==TeleportCause.END_PORTAL && isIslandWorld(event.getFrom().getWorld())) {
			Location loc = event.getPortalTravelAgent().findPortal(new Location(event.getTo().getWorld(), 0, 64, 0));
			if (loc!=null) {
				event.setTo(loc);
				event.useTravelAgent(false);
			}
		}
	}
	
	public boolean isIsland(Claim claim) {
		Island island = getIsland(claim);
		if (island == null) {
			return false;
		}
		return island.getClaim() == claim;
	}

	public Island getIsland(Claim claim) {
		GDClaim gdClaim = (GDClaim) claim;
		if (!isIslandWorld(gdClaim.getWorld())) {
			return null;
		}
		Island island = instance.getDataStore().getIsland(claim.getOwnerUniqueId());
		if (((GDClaim)island.getClaim()).getUniqueId().equals(claim.getUniqueId())) {
			return island;
		}
		return null;
	}

	public boolean isIslandWorld(World world) {
		return world.getName().equals(instance.config().worldName);
	}
}
