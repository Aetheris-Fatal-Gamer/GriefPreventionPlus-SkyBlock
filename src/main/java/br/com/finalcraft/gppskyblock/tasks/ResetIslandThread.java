package br.com.finalcraft.gppskyblock.tasks;

import br.com.finalcraft.evernifecore.util.FCWorldUtil;
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
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ResetIslandThread extends Thread {

    private final ResetIslandThread instance;
    private final Island island;
    private final String ownerName;
    private final IClaim claim;
    private final World bWorld;
    private final Player player;
    private final List<Supplier<Chunk>> bukkitChunkSuppliers = new ArrayList<>();
    private final List<Runnable> bukkitChunkUnloaders = new ArrayList<>();
    private final List<Runnable> bukkitBiomaChanger = new ArrayList<>();
    private final File schematic;

    public ResetIslandThread(Island island, File schematic) {
        this.instance = this;
        this.island = island;
        this.ownerName = island.getOwnerName();
        this.claim  = island.getClaim();
        this.bWorld = island.getClaim().getWorld();
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
        GPPSkyBlock.info("[" + this.getName() + "] " + message);
    }

    private void fillSuppliers(){
        bukkitChunkSuppliers.clear();
        List<Location> minAndMaxPoints = FCWorldUtil.getMinimumAndMaximumLocation(Arrays.asList(claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner()));
        int lowerX = minAndMaxPoints.get(0).getBlockX()>>4;
        int lowerZ = minAndMaxPoints.get(0).getBlockZ()>>4;
        int upperX = minAndMaxPoints.get(1).getBlockX()>>4;
        int upperZ = minAndMaxPoints.get(1).getBlockZ()>>4;
        for (; lowerX <= upperX; lowerX++) {
            for (int z = lowerZ; z <= upperZ; z++) {
                final int chunkXCoord = lowerX;
                final int chunkZCoord = z;
                bukkitChunkSuppliers.add(() -> {
                    return bWorld.getChunkAt(chunkXCoord, chunkZCoord);
                });
                bukkitChunkUnloaders.add(() ->{
                    bWorld.unloadChunk(chunkXCoord, chunkZCoord);
                });
                bukkitBiomaChanger.add(() ->{
                    bWorld.setBiome(chunkXCoord, chunkZCoord, Biome.PLAINS);
                });
            }
        }
    }

    private void regenChunk(Chunk bChunk){
        for (Entity entity : bChunk.getEntities()) {
            if (entity instanceof Player){
                ((Player)entity).kickPlayer("§cUma ilha estava sendo resetada enquanto você estava próximo! T.T");
            }else {
                entity.remove();
            }
        }
        bChunk.getWorld().regenerateChunk(bChunk.getX(), bChunk.getZ());
        new BukkitRunnable(){
            @Override
            public void run() {
                bChunk.unload(true);
            }
        }.runTaskLater(GPPSkyBlock.getInstance(),1);
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
        unpauseProcess();
    }

    private void changeBiomes(){
        for (Runnable runnable : bukkitBiomaChanger) {
            try{
                runnable.run();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void bringOwnerBack(){
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN+"A sua ilha foi gerada com sucesso! Você será teletransportado em " + GPPSkyBlock.getInstance().config().tpCountdown + " segundos.");
            SpawnTeleportTask.teleportTask(player, island, GPPSkyBlock.getInstance().config().tpCountdown);
        }
    }

    boolean canWeContinue = true;
    private void pauseProcessUntilWeCanContinue() throws InterruptedException{
        canWeContinue = false;
        while (canWeContinue() == false){
            Thread.sleep(100);
        }
    }

    private void unpauseProcess(){
        canWeContinue = true;
    }

    private boolean canWeContinue(){
        return canWeContinue;
    }

    private void runSync(Runnable runnable){
        new BukkitRunnable(){
            @Override
            public void run() {
                runnable.run();
            };
        }.runTaskLater(GPPSkyBlock.getInstance(),1);
    }


    private AtomicInteger failedChunks = new AtomicInteger(0);

    @Override
    public void run() {
        try {
            sendMessage("§7§oIniciando Contagem de chunks da ilha!");
            island.teleportEveryoneToSpawn();
            fillSuppliers();
            int totalChunks = bukkitChunkSuppliers.size();

            sendMessage("§7§oIniciando restauração das " + totalChunks + " Chunks!");

            int contador = 0;
            int delay = 1;
            for (int i = 0; i < bukkitChunkSuppliers.size(); i++) {
                if (contador >= 9){
                    contador = 0;
                    delay++;
                }
                contador++;
                final Supplier<Chunk> chunkSupplier = bukkitChunkSuppliers.get(i);
                final boolean isLastChunk = (i == (bukkitChunkSuppliers.size() - 1));
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        chunkSupplier.get();//Load the chunk
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                try {
                                    final Chunk bChunk = chunkSupplier.get();
                                    regenChunk(bChunk);
                                }catch (Exception e){
                                    synchronized (failedChunks){
                                        failedChunks.incrementAndGet();
                                    }
                                }
                            }
                        }.runTaskLater(GPPSkyBlock.getInstance(),1);
                    }
                }.runTaskLater(GPPSkyBlock.getInstance(),delay);
                if (isLastChunk){
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            instance.unpauseProcess();
                        }
                    }.runTaskLater(GPPSkyBlock.getInstance(),delay + 5);
                }
            }

            pauseProcessUntilWeCanContinue();//Pause the Threads!
            sendMessage("§7§oRestauração concluida, Iniciando colagem da nova ilha!");

            runSync(() -> pasteSchematic());
            pauseProcessUntilWeCanContinue();//Pause the Threads!

            sendMessage("§7§oProcesso de colagem finalizado!");
            sendMessage("§7§oParece que está tudo OK!");
            island.ready = true;

            runSync(() -> {
                for (Runnable bukkitChunkUnloader : bukkitChunkUnloaders) {
                    bukkitChunkUnloader.run();
                }
            });

            bringOwnerBack();

            new BukkitRunnable(){
                @Override
                public void run() {
                    changeBiomes();
                }
            }.runTaskLater(GPPSkyBlock.getInstance(),1000);

            if (failedChunks.get() > 0) {
                GPPSkyBlock.debug("Aprentemente " + failedChunks.get() + " chunks falharam na restauração da ilha do jogador " + island.getOwnerName());
            }
        }catch (Exception e){
            e.printStackTrace();
            sendMessage("§cErro ao tentar contar as chunks do seu Claim!");
            sendMessage("§c§o" + e.getMessage());
        }
    }
}