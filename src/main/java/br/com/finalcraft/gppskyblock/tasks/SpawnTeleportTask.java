package br.com.finalcraft.gppskyblock.tasks;

import br.com.finalcraft.evernifecore.scheduler.FCScheduler;
import br.com.finalcraft.evernifecore.thread.SimpleThread;
import br.com.finalcraft.gppskyblock.Island;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnTeleportTask extends SimpleThread {

	private Player player;
	private Island island;
	private int countdown;

	private Location startLocation;

	private SpawnTeleportTask(Player player, Island island, int countdown) {
		this.player = player;
		this.island = island;
		this.countdown = countdown;
		this.startLocation = player.getLocation();
	}

	@Override
	protected void run() throws InterruptedException {
		//Force chunk loading
		Chunk islandChunk = FCScheduler.SynchronizedAction.runAndGet(() -> {
			return island.getSpawn().getChunk();
		});

		for (int i = 0; i < countdown; i++) {
			Thread.sleep(1000);
			if (!player.isOnline() || this.player.getLocation().getWorld() != startLocation.getWorld() ||  this.player.getLocation().distanceSquared(startLocation) > 5) {
				player.sendMessage("§e§l ▶ §cO teleporte foi cancelado pois você se moveu.");
				return;
			}
		}

		player.teleport(island.getSpawn());
	}
	
	public static void teleportTask(Player player, Island island, int countdown) {
		new SpawnTeleportTask(player, island, countdown * 4).start();
	}
}
