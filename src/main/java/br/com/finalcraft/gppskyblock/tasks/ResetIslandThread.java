package br.com.finalcraft.gppskyblock.tasks;

import br.com.finalcraft.evernifecore.minecraft.vector.BlockPos;
import br.com.finalcraft.evernifecore.minecraft.vector.ChunkPos;
import br.com.finalcraft.evernifecore.scheduler.FCScheduler;
import br.com.finalcraft.evernifecore.time.FCTimeFrame;
import br.com.finalcraft.evernifecore.util.FCCollectionsUtil;
import br.com.finalcraft.evernifecore.vectors.CuboidSelection;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.Utils;
import br.com.finalcraft.gppskyblock.integration.IClaim;
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
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResetIslandThread extends Thread {

    private final Island island;
    private final IClaim claim;
    private final World world;
    private final Player player;
    private final File schematic;
    private long start;

    public ResetIslandThread(Island island, File schematic) {
        this.island = island;
        this.claim  = island.getClaim();
        this.world = island.getClaim().getWorld();
        this.player = island.getPlayer();
        this.schematic = schematic;
        this.setName("IslandResetThread - " + " - " + claim.getID() + " - " + island.getOwnerName());
        this.setDaemon(true);
        this.start();
    }

    private void sendMessage(String message){
        if (player != null && player.isOnline()){
            player.sendMessage(message);
        }
        GPPSkyBlock.info("[" + this.getName() + " - " + getTimeSinceStart() + "] " + message);
    }

    private void pasteSchematic(){
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
        } catch (MaxChangedBlocksException | IOException e) {
            if (island.isOwnerOnline()) {
                island.getPlayer().sendMessage("§c§lErro ao tentar colar a nova ilha, avise o EverNife!!!!");
            }
            e.printStackTrace();
        }
    }

    private AtomicInteger failedChunks = new AtomicInteger(0);

    private String getTimeSinceStart(){
        return FCTimeFrame.of(System.currentTimeMillis() - start).getFormattedDiscursive(true);
    }

    @Override
    public void run() {
        try {
            start = System.currentTimeMillis();
            sendMessage(String.format("§7 ● §oIniciando Contagem de chunks da ilha!"));
            island.teleportEveryoneToSpawn();

            CuboidSelection cuboidSelection = CuboidSelection.of(
                    BlockPos.from(claim.getLesserBoundaryCorner()),
                    BlockPos.from(claim.getGreaterBoundaryCorner())
            );

            List<ChunkPos> allIslandChunks = cuboidSelection.getChunks();
            sendMessage("§7 ● §oIniciando restauração das " + allIslandChunks.size() + " Chunks...");

            List<List<ChunkPos>> subLists = FCCollectionsUtil.partitionEvenly(allIslandChunks, 20);

            for (List<ChunkPos> chunksToReset : subLists) {

                FCScheduler.SynchronizedAction.schedule(() -> {
                    for (ChunkPos chunkPos : chunksToReset) {
                        Chunk chunk = chunkPos.getChunk(claim.getWorld());
                        try {
                            //Kill all Entiteis
                            for (Entity entity : chunk.getEntities()) {
                                if (entity instanceof Player){
                                    ((Player)entity).kickPlayer("§cUma ilha estava sendo resetada enquanto você estava próximo! T.T");
                                }else {
                                    entity.remove();
                                }
                            }

                            //Regen the chunk
                            world.regenerateChunk(chunk.getX(), chunk.getZ());

                            //Change chunks biomes
                            world.setBiome(chunk.getX(), chunk.getZ(), Biome.PLAINS);
                        }catch (Exception e){
                            e.printStackTrace();
                            synchronized (failedChunks){
                                failedChunks.incrementAndGet();
                            }
                        }
                    }
                },2); //  +-12 chunks per 2 tick
            }

            sendMessage(String.format("§7 ● §oRestauração concluída, Iniciando colagem da nova ilha..."));
            FCScheduler.SynchronizedAction.run(() -> {
                pasteSchematic();
            });

            sendMessage(String.format("§7 ● §oProcesso de colagem finalizado! Salvando Ilha."));

            FCScheduler.SynchronizedAction.schedule(() -> {
                for (List<ChunkPos> chunksToReset : subLists) {
                    for (ChunkPos chunkPos : chunksToReset) {
                        world.unloadChunk(chunkPos.getX(), chunkPos.getZ(),true);
                    }
                }
            }, 1);

            sendMessage(String.format("§7 ● §oSalvamento Finalizado! Parece que está tudo OK!!!"));
            island.ready = true;

            //With Chunk Grace period, is a good idea to 'touch' the main chunks to make them re-render corretly
            FCScheduler.runSync(() -> {
                CuboidSelection center = CuboidSelection.of(BlockPos.from(island.getCenter()));
                center.expand(32);
                for (ChunkPos chunkPos : center.getChunks()) {
                    Block block = chunkPos.getBlock(0, 0, 0).getBlock(world);
                    block.setType(Material.STONE);
                    FCScheduler.scheduleSyncInTicks(() -> {
                        block.setType(Material.AIR);
                    }, 5);
                }
            });

            //bringOwnerBack
            if (player != null && player.isOnline()) {
                player.sendMessage("§2§l ▶ §aA sua ilha foi gerada com sucesso! Você será teletransportado em " + GPPSkyBlock.getInstance().config().tpCountdown + " segundos.");
                SpawnTeleportTask.teleportTask(player, island, GPPSkyBlock.getInstance().config().tpCountdown);
            }

            if (failedChunks.get() > 0) {
                GPPSkyBlock.debug("Aparentemente " + failedChunks.get() + " chunks falharam na restauração da ilha do jogador " + island.getOwnerName());
            }

        }catch (Exception e){
            e.printStackTrace();
            sendMessage("§cErro ao tentar contar as chunks do seu Claim!");
            sendMessage("§c§o" + e.getMessage());
        }
    }
}