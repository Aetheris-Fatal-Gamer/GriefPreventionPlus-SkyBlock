package net.kaikk.mc.gpp.skyblock;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimPermission;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent.Reason;

public class CommandExec implements CommandExecutor {
	private GPPSkyBlock instance;
	private Map<UUID,String> confirmations = new HashMap<UUID,String>();

	CommandExec(GPPSkyBlock instance) {
		this.instance = instance;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals(instance.getName())) {
			if (args.length==0) {
				sender.sendMessage(help(label));
				return false;
			}

			switch(args[0].toLowerCase()) {
			case "help":
				sender.sendMessage(help(label));
				return true;
			case "spawn":
			case "home":
			case "tp":
				return spawn(sender, label, args);
			case "reset":
			case "restart":
				return reset(sender, label, args);
			case "trust":
			case "invite":
				return invite(sender, label, args);
			case "setspawn":
			case "sethome":
				return setSpawn(sender, label, args);
			case "setbiome":
				return setBiome(sender, label, args);
			case "biomelist":
				return biomeList(sender);
			case "setradius":
				return setRadius(sender, label, args);
			case "delete":
				return delete(sender, label, args);
			case "private":
				return privatec(sender, label, args);
			case "public":
				return publicc(sender, label, args);
			}

			sender.sendMessage(ChatColor.RED+"Wrong parameter.");
			sender.sendMessage(help(label));
		}

		return false;
	}

	private boolean publicc(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.private")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}
		Player player = (Player) sender;

		Island island = instance.dataStore().getIsland(player.getUniqueId());
		if (island == null) {
			sender.sendMessage(ChatColor.RED + "The specified player does not have an island.");
			return false;
		}

		Claim claim = island.getClaim();
		claim.dropPermission(player.getUniqueId());
		claim.setPermission(GriefPreventionPlus.UUID0, ClaimPermission.ENTRY);
		sender.sendMessage(ChatColor.GOLD + "Your island can be now visited by all players.");
		return true;
	}

	private boolean privatec(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.private")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}
		Player player = (Player) sender;

		Island island = instance.dataStore().getIsland(player.getUniqueId());
		if (island == null) {
			sender.sendMessage(ChatColor.RED + "The specified player does not have an island.");
			return false;
		}

		Claim claim = island.getClaim();
		claim.setPermission(player.getUniqueId(), ClaimPermission.ENTRY);
		claim.dropPermission(GriefPreventionPlus.UUID0);
		sender.sendMessage(ChatColor.GOLD + "Your island is now private and can be visited only by the players you specify with /entrytrust [player]");
		return true;
	}

	@SuppressWarnings("deprecation")
	private boolean delete(CommandSender sender, String label, String[] args) {
		if (args.length<2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " delete [PlayerName]");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}

		if (sender.hasPermission("gppskyblock.delete.any")) {
			OfflinePlayer offP = Bukkit.getOfflinePlayer(args[1]);
			if (!offP.hasPlayedBefore() && !offP.isOnline()) {
				sender.sendMessage(ChatColor.RED + "Unknown player");
				return false;
			}

			Island island = instance.dataStore().getIsland(offP.getUniqueId());
			if (island == null) {
				sender.sendMessage(ChatColor.RED + "The specified player does not have an island.");
				return false;
			}

			ClaimDeleteEvent event = new ClaimDeleteEvent(island.getClaim(), player, Reason.DELETE);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				sender.sendMessage(ChatColor.RED + "This island can't be deleted.");
				return false;
			}
			GriefPreventionPlus.getInstance().getDataStore().deleteClaim(island.getClaim());
			return true;
		}

		if (!sender.hasPermission("gppskyblock.delete")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (player == null) {
			return false;
		}

		if (!args[1].equalsIgnoreCase(player.getName())) {
			sender.sendMessage(ChatColor.RED + "You cannot delete "+args[1]+"'s island.");
			return false;
		}

		Island island = instance.dataStore().getIsland(player.getUniqueId());
		if (island == null) {
			sender.sendMessage(ChatColor.RED + "You do not have an island.");
			return false;
		}

		ClaimDeleteEvent event = new ClaimDeleteEvent(island.getClaim(), player, Reason.DELETE);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			sender.sendMessage(ChatColor.RED + "Your island can't be deleted.");
			return false;
		}
		GriefPreventionPlus.getInstance().getDataStore().deleteClaim(island.getClaim());
		sender.sendMessage(ChatColor.RED + "Your island has been deleted.");
		return true;
	}

	private String help(String label) {
		return ChatColor.GOLD + "" + ChatColor.BOLD + "=== GriefPreventionPlus-SkyBlock ===\n" +
				ChatColor.AQUA + "/" + label + " help - shows this help\n" +
				ChatColor.AQUA + "/" + label + " reset - resets your island\n" +
				ChatColor.AQUA + "/" + label + " spawn [PlayerName] - teleports to your island or the specified player's island\n" +
				ChatColor.AQUA + "/" + label + " setspawn - sets your island's spawn at your current location\n" +
				ChatColor.AQUA + "/" + label + " setbiome (island|chunk|block) [biome] - sets the biome of your island\n" +
				ChatColor.AQUA + "/" + label + " biomelist - list allowed biomes that can be used with setbiome\n" +
				ChatColor.AQUA + "/" + label + " invite [PlayerName] - Adds a player to your island and tells them how to get to your island.\n" +
				ChatColor.AQUA + "/" + label + " delete [PlayerName] - Delete the specified island. You must specify your own name.\n" +
				ChatColor.AQUA + "/" + label + " private/public - Allow or deny all players to visit your island.\n" +
				ChatColor.RED + "You can use almost all GriefPreventionPlus commands on your island, like /trust [PlayerName].\n" +
				(Bukkit.getPluginManager().isPluginEnabled("GPPCities") ? ChatColor.RED + "GriefPreventionPlus-Cities is supported. Use '/city help' for more info." : "");
	}

	@SuppressWarnings("deprecation")
	private boolean spawn(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.spawn")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}

		Player player = (Player) sender;
		Island island = null;

		if (args.length>1) {
			OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(args[1]);
			island = this.instance.dataStore().getIsland(oPlayer.getUniqueId());
		} else {
			island = this.instance.dataStore().getIsland(player.getUniqueId());
			if (island==null) {
				// this player doesn't have an island yet... so create a new island
				try {
					island = this.instance.dataStore().createIsland(player.getUniqueId());
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED+"An error occurred while creating the island: "+e.getMessage());
					return false;
				}
				return true;
			}
		}

		if (island==null) {
			sender.sendMessage(ChatColor.RED+"The specified player doesn't have an island on this server.");
			return false;
		}

		if (island.getClaim().canEnter(player) != null) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to teleport to this island.");
			return false;
		}

		if (!island.ready) {
			sender.sendMessage(ChatColor.RED+"There's a pending operation on this island.");
			return false;
		}

		sender.sendMessage(ChatColor.GREEN+"You'll be teleported in "+instance.config().tpCountdown+" seconds. Do not move.");
		SpawnTeleportTask.teleportTask(player, island, instance.config().tpCountdown);

		return true;
	}

	private boolean reset(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.reset")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}

		Player player = (Player) sender;

		Island island = this.instance.dataStore().getIsland(player.getUniqueId());
		if (island==null) {
			sender.sendMessage(ChatColor.RED+"You don't have an island yet. Use the \"/"+label+" spawn\" command");
			return false;
		}

		if (!island.ready) {
			sender.sendMessage(ChatColor.RED+"There's a pending operation on this island.");
			return false;
		}

		String conf = this.confirmations.remove(player.getUniqueId());
		if (conf==null || !conf.equals("reset")) {
			sender.sendMessage(ChatColor.RED+"WARNING: your entire island will be reset!\nIf you're aware about this, type \"/"+label+" reset\" again.");
			this.confirmations.put(player.getUniqueId(), "reset");
			return false;
		}

		island.reset();
		return true;
	}

	private boolean setSpawn(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.setspawn")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}

		Player player = (Player) sender;
		String conf = this.confirmations.remove(player.getUniqueId());
		if (conf==null || !conf.equals("spawn")) {
			sender.sendMessage(ChatColor.RED+"WARNING: Be sure to use a full block for your island spawn. Do not use slabs!");
			this.confirmations.put(player.getUniqueId(), "spawn");
			return false;
		}

		Island island = this.instance.dataStore().getIsland(player.getUniqueId());
		if (island==null) {
			sender.sendMessage(ChatColor.RED+"You don't have an island yet. Use the \"/"+label+" spawn\" command");
			return false;
		}

		if (!island.getClaim().contains(player.getLocation(), true, false)) {
			sender.sendMessage(ChatColor.RED+"You aren't inside your island");
			return false;
		}

		if (!island.ready) {
			sender.sendMessage(ChatColor.RED+"There's a pending operation on this island.");
			return false;
		}

		try {
			island.setSpawn(player.getLocation().add(0, 2, 0));
			sender.sendMessage(ChatColor.GREEN+"Island spawn point set");
		} catch (SQLException e) {
			e.printStackTrace();
			sender.sendMessage(ChatColor.RED+"An error occurred while creating the island: data store issue.");
			return false;
		}

		return true;
	}

	private boolean setBiome(CommandSender sender, String label, String[] args) {
		if (args.length!=3) {
			sender.sendMessage(ChatColor.RED + "/" + label + " setbiome [island|chunk|block] [biome] - sets the biome of your island");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}

		Player player = (Player) sender;

		Biome biome = Utils.matchAllowedBiome(args[2]);
		if (biome==null) {
			sender.sendMessage(ChatColor.RED+"The biome "+args[2]+" is not allowed. Use /is biomelist.");
			return false;
		}

		if (!sender.hasPermission("gppskyblock.setbiome.all") && !sender.hasPermission("gppskyblock.setbiome."+args[2])) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to set this biome.");
			return false;
		}

		Island island = this.instance.dataStore().getIsland(player.getUniqueId());
		if (island==null) {
			sender.sendMessage(ChatColor.RED+"You don't have an island yet. Use the \"/"+label+" spawn\" command");
			return false;
		}

		if (!island.ready) {
			sender.sendMessage(ChatColor.RED+"There's a pending operation on this island.");
			return false;
		}

		switch(args[1].toLowerCase()) {
		case "island": {
			island.setIslandBiome(biome);
			break;
		}
		case "chunk": {
			if (!island.getClaim().contains(player.getLocation(), true, false)) {
				sender.sendMessage(ChatColor.RED+"You aren't inside your island.");
				return false;
			}

			island.setChunkBiome(biome, player.getLocation().getBlockX()>>4, player.getLocation().getBlockZ()>>4);
			break;
		}
		case "block": {
			if (!island.getClaim().contains(player.getLocation(), true, false)) {
				sender.sendMessage(ChatColor.RED+"You aren't inside your island.");
				return false;
			}

			island.setBlockBiome(biome, player.getLocation().getBlockX(), player.getLocation().getBlockZ());
			break;
		}
		default:
			sender.sendMessage("Invalid parameter "+args[1]);
			return false;
		}

		sender.sendMessage(ChatColor.GREEN+"Biome set. You may need to relog to see changes.");
		return true;
	}


	private boolean setRadius(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("gppskyblock.setradius")) {
			sender.sendMessage(ChatColor.RED+"You don't have permission to run this command.");
			return false;
		}

		if (args.length!=3) {
			sender.sendMessage(ChatColor.RED + "/" + label + " setradius (radius) (PlayerName) - sets the size of the specified player's island in block radius");
			return false;
		}

		int radius;
		try {
			radius = Integer.valueOf(args[1]);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Invalid radius (max 254)");
			return false;
		}

		if (radius>254 || radius<1) {
			sender.sendMessage(ChatColor.RED + "Invalid radius (max 254)");
			return false;
		}

		@SuppressWarnings("deprecation")
		OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
		if (!player.hasPlayedBefore()) {
			sender.sendMessage(ChatColor.RED + "Player not found");
			return false;
		}

		Island island = this.instance.dataStore().getIsland(player.getUniqueId());
		if (island==null) {
			sender.sendMessage(ChatColor.RED+"The specified player doesn't have an island yet.");
			return false;
		}

		island.setRadius(radius);
		sender.sendMessage(ChatColor.GREEN+player.getName()+"'s island size has been set to "+radius+" blocks radius.");
		return true;
	}

	private boolean biomeList(CommandSender sender) {
		StringBuilder sb = new StringBuilder(ChatColor.GOLD + "Biome list: " + ChatColor.AQUA);
		if (instance.config().allowedBiomes.isEmpty()) {
			sb.append(ChatColor.RED+"none");
		} else {
			for (Biome biome : instance.config().allowedBiomes) {
				sb.append(Utils.fromSnakeToCamelCase(biome.toString()));
				sb.append(", ");
			}
		}

		sender.sendMessage(sb.substring(0, sb.length()-2).toString());
		return true;
	}

	private boolean invite(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED+"Only players can run this command.");
			return false;
		}

		if (args.length<2) {
			sender.sendMessage(ChatColor.RED + "/" + label + " invite (PlayerName) - Adds a player to your island and tells them how to get to your island.");
			return false;
		}

		//cast sender to player
		Player p = (Player) sender;

		//get the player's island
		Island is = this.instance.dataStore().getIsland(p.getUniqueId());

		//if the island doesn't exist, don't continue
		if (is == null) {
			p.sendMessage(ChatColor.RED + "You do not have an island.");
			return false;
		}
		//get the island's claim
		Claim claim = is.getClaim();

		//get the player to be trusted
		@SuppressWarnings("deprecation")
		OfflinePlayer offP = Bukkit.getOfflinePlayer(args[1]);

		//if the player has played before, trust them to the island
		if (offP.getLastPlayed()==0 && !offP.isOnline()) {
			p.sendMessage(ChatColor.RED + "The specified player has never played on this server.");
			return false;
		}

		//add them to the island's claim
		claim.setPermission(offP.getUniqueId(), ClaimPermission.BUILD);

		//if they're online tell them they have been invited to the island.
		if (offP.isOnline()) {
			((Player) offP).sendMessage(ChatColor.GREEN + "Hey! " + p.getName() + " has invited you to their island! To teleport to them, do /is spawn " + sender.getName());
		}
		p.sendMessage(ChatColor.GREEN + offP.getName() + " can now access your island!");

		return true;
	}
}
