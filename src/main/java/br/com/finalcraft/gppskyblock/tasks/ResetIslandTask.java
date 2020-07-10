package br.com.finalcraft.gppskyblock.tasks;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.Utils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated //Not used anymore
public class ResetIslandTask extends BukkitRunnable {
	private File schematic;
	private Island island;
	private String ownerName;
	private int x, z, lz, gx, gz;
	private Stage stage;

	public ResetIslandTask(Island island, File schematic) {
		this.island = island;
		this.schematic = schematic;
		this.stage = Stage.REGEN;
		this.initCoords();
		ownerName = island.getOwnerName();
		if (island.isOwnerOnline()) {
			island.getPlayer().sendMessage(ChatColor.GREEN+"Por favor espere enquanto a sua ilha está sendo criada!");
		}
		Bukkit.getLogger().info("Generating "+ownerName+" island at "+island.getClaim().locationToString());
	}

	private final List<Chunk> islandChunks = new ArrayList<>();
	private int INDEX = 0;
	private int contador = 0;
	@Override
	public void run() {
		GPPSkyBlock.info("[" + (contador++) +  "] ResetIslandTask - PhaseProcess " + this.stage);
		switch(this.stage) {
			case REGEN: { // Regenera 8 chunks por tick!
				for (int i = 0; i<8; i++) {
					if (x <= gx) {
						if (z <= gz) {
							try {
								Chunk chunk = island.getClaim().getWorld().getChunkAt(x,z);
								chunk.load(true);

								for (Entity entity : chunk.getEntities()) {
									if (entity instanceof Player){
										((Player)entity).kickPlayer("§cUma ilha estava sendo resetada enquanto você estava próximo! T>T");
									}else {
										entity.remove();
									}
								}

								for (BlockState blockState : chunk.getTileEntities()) {
									blockState.setType(Material.AIR);
									blockState.setData(new MaterialData(Material.AIR));
									blockState.update(true,true);
								}

								chunk.getWorld().regenerateChunk(chunk.getX(),chunk.getZ());
								chunk.unload(true);
								islandChunks.add(chunk);
							} catch (Exception e) {
								if (island.isOwnerOnline()) {
									island.getPlayer().sendMessage(ChatColor.RED+"An error occurred while generating a new island: regen error.");
								}
								Bukkit.getLogger().info("An error occurred while generating "+ownerName+" island");
								e.printStackTrace();
								this.cancel();
								return;
							}
							z++;
						} else {
							this.z = lz;
							x++;
						}
					} else {
						this.stage = Stage.CLEAR_THEM_ALL;
						return;
					}
				}
				return;
			}
			case CLEAR_THEM_ALL: {

				for (int INDEX_PLUS_8 = INDEX + 8; INDEX < INDEX_PLUS_8 && INDEX < islandChunks.size(); INDEX++){
					Chunk chunk = islandChunks.get(INDEX);
					chunk.load(true);

					final World world = chunk.getWorld();
					int cx = chunk.getX() << 4;
					int cz = chunk.getZ() << 4;
					boolean foundAnyBlock = false;
					for (int x = cx; x < cx + 16; x++) {
						for (int z = cz; z < cz + 16; z++) {
							for (int y = 0; y < 128; y++) {
								Block block;
								if ((block = world.getBlockAt(x, y, z)).getType() != Material.AIR) {
									block.setType(Material.AIR);
									foundAnyBlock = true;
								}
							}
						}
					}
					if (foundAnyBlock == true){
						chunk.unload(true);
						chunk.load();
					}
				}

				if (INDEX == islandChunks.size()){
					this.stage = Stage.SCHEMATIC;
				}

				return;
			}
			case SCHEMATIC: {
				Bukkit.getLogger().info(ownerName + " island regeneration complete.");
				try {
					// read schematic file
					FileInputStream fis = new FileInputStream(schematic);
					BufferedInputStream bis = new BufferedInputStream(fis);
					ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(bis);
		
					// create clipboard
					WorldData worldData = LegacyWorldData.getInstance();
					Clipboard clipboard = reader.read(worldData);
					fis.close();
		
					ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard, worldData);
					EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(Utils.fromBukkitToWorldEditWorld(island.getClaim().getWorld()), 1000000);
		
					try {
						island.setSpawn(island.getCenter());
					} catch (Exception e) {
						e.printStackTrace();
					}
		
					island.getSpawn().getChunk().load();
					
					Operation operation = clipboardHolder.createPaste(editSession, LegacyWorldData.getInstance()).to(Utils.toVector(island.getSpawn())).ignoreAirBlocks(true).build();
					Operations.completeLegacy(operation);
					
					Bukkit.getLogger().info(ownerName+" island schematic load complete.");
		
					if (GPPSkyBlock.getInstance().config().defaultBiome != null) {
						island.setIslandBiome(GPPSkyBlock.getInstance().config().defaultBiome);
						Bukkit.getLogger().info(ownerName+" island biome set to default biome ("+GPPSkyBlock.getInstance().config().defaultBiome.toString()+")");
					}
					
					this.stage = Stage.UNLOADCHUNKS;
					this.initCoords();
				} catch (MaxChangedBlocksException | IOException e) {
					if (island.isOwnerOnline()) {
						island.getPlayer().sendMessage(ChatColor.RED+"An error occurred while generating a new island: schematic load error.");
					}
					this.cancel();
					e.printStackTrace();
				}
				return;
			}
			case UNLOADCHUNKS: {
				islandChunks.forEach(Chunk::unload);
				this.stage = Stage.COMPLETED;
				return;
			}
			case COMPLETED: {
				island.ready = true;
				if (island.isOwnerOnline()) {
					island.getPlayer().sendMessage(ChatColor.GREEN+"A sua ilha foi gerada com sucesso! Você será teletransportado em " + GPPSkyBlock.getInstance().config().tpCountdown + " segundos.");
					SpawnTeleportTask.teleportTask(island.getPlayer(), island, GPPSkyBlock.getInstance().config().tpCountdown);
				}
				this.cancel();
				return;
			}
		}
	}
	
	private void initCoords() {
		this.lz = island.getClaim().getLesserBoundaryCorner().getBlockZ()>>4;
		this.x = island.getClaim().getLesserBoundaryCorner().getBlockX()>>4;
		this.z = lz;
		this.gx = island.getClaim().getGreaterBoundaryCorner().getBlockX()>>4;
		this.gz = island.getClaim().getGreaterBoundaryCorner().getBlockZ()>>4;
	}
	
	enum Stage {
		REGEN, //Carrega 8 chunks por tick, e usa a função .regen() do próprio bukkit
		CLEAR_THEM_ALL, //Uma proteção extra que eu fiz, passa por todas as chunks novamente deletando todos os blocos dela.
		SCHEMATIC, //Copia a schematic e cola ela no centro da bagaça
		UNLOADCHUNKS, //Descarrega todas as chunks (uma chunk só é salva quando descarregada)
		COMPLETED; //Teleporta o jgoador para a ilha
	}
	
}
