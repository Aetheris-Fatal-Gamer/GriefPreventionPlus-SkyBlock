package br.com.finalcraft.gppskyblock.tasks;

import br.com.finalcraft.evernifecore.FCWorldUtil;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.Utils;
import com.gamerforea.eventhelper.util.ConvertUtils;
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
import net.kaikk.mc.gpp.Claim;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ResetIslandThread extends Thread {

    private final ResetIslandThread instance;
    private final Island island;
    private final String ownerName;
    private final Claim claim;
    private final WorldServer world;
    private final World bWorld;
    private final Player player;
    private final ChunkProviderServer chunkProvider;
    private final List<Supplier<Chunk>> chunkSuppliers = new ArrayList<>();
    private final List<Runnable> bukkitChunkUnloaders = new ArrayList<>();
    private final List<Runnable> bukkitBiomaChanger = new ArrayList<>();
    private final File schematic;

    public ResetIslandThread(Island island, File schematic) {
        this.instance = this;
        this.island = island;
        this.ownerName = island.getOwnerName();
        this.claim  = island.getClaim();
        this.world  = ConvertUtils.toMinecraftWorld(island.getClaim().getWorld());
        this.bWorld = ConvertUtils.toBukkitWorld(world);
        this.player = island.getPlayer();
        this.chunkProvider = (ChunkProviderServer) world.getChunkProvider();
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
        chunkSuppliers.clear();
        List<Location> minAndMaxPoints = FCWorldUtil.getMinimumAndMaximumLocation(Arrays.asList(claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner()));
        List<Chunk> allChunks = new ArrayList<>();
        int lowerX = minAndMaxPoints.get(0).getBlockX()>>4;
        int lowerZ = minAndMaxPoints.get(0).getBlockZ()>>4;
        int upperX = minAndMaxPoints.get(1).getBlockX()>>4;
        int upperZ = minAndMaxPoints.get(1).getBlockZ()>>4;
        for (; lowerX <= upperX; lowerX++) {
            for (int z = lowerZ; z <= upperZ; z++) {
                final int chunkXCoord = lowerX;
                final int chunkZCoord = z;
                chunkSuppliers.add(() -> {
                    return world.getChunkFromChunkCoords(chunkXCoord, chunkZCoord);
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

    private void regenChunk(Chunk chunk){
        chunk.sendUpdates = false;
        try {
            for (Object o : chunk.chunkTileEntityMap.values()) {
                if (o instanceof TileEntity){
                    TileEntity entity = (TileEntity) o;
                    entity.invalidate();
                }
            }
        }catch (Exception e){
            GPPSkyBlock.debug("Error removing TileEntities from Chunk:" + chunk);
            e.printStackTrace();
        }
        try {
            for (List entityList : chunk.entityLists) {
                for (Object o : entityList) {
                    if (o instanceof net.minecraft.entity.Entity){
                        net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) o;
                        if ( !(mcEntity instanceof EntityPlayer)){
                            mcEntity.setDead();
                        }
                    }
                }
            }
        }catch (Exception e){
            GPPSkyBlock.debug("Error removing Entities from Chunk:" + chunk);
            e.printStackTrace();
        }
        chunk.sendUpdates = true;
        chunk.isModified = true;
        final org.bukkit.Chunk bChunk = bWorld.getChunkAt(chunk.xPosition,chunk.zPosition);
        for (Entity entity : bChunk.getEntities()) {
            if (entity instanceof Player){
                ((Player)entity).kickPlayer("§cUma ilha estava sendo resetada enquanto você estava próximo! T.T");
            }else {
                entity.remove();
            }
        }
        bChunk.getWorld().regenerateChunk(chunk.xPosition,chunk.zPosition);
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
            } catch (SQLException e) {
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

    private void ensureAllTilesWereRemoved(){
        int contador = 0;
        int delay = 1;
        for (int i = 0; i < chunkSuppliers.size(); i++) {
            if (contador >= 9){
                contador = 0;
                delay++;
            }
            contador++;
            final Supplier<Chunk> chunkSupplier = chunkSuppliers.get(i);
            final boolean isLastChunk = (i == (chunkSuppliers.size() - 1));
            new BukkitRunnable(){
                @Override
                public void run() {
                    Chunk chunk = chunkSupplier.get();
                    chunk.sendUpdates = false;
                    for (Object o : chunk.chunkTileEntityMap.values()) {
                        if (o instanceof TileEntity){
                            TileEntity entity = (TileEntity) o;
                            entity.invalidate();
                        }
                    }
                    for (List entityList : chunk.entityLists) {
                        for (Object o : entityList) {
                            if (o instanceof net.minecraft.entity.Entity){
                                net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) o;
                                if ( !(mcEntity instanceof EntityPlayer)){
                                    mcEntity.setDead();
                                }
                            }
                        }
                    }
                    chunk.sendUpdates = true;
                    chunk.isModified = true;
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
            int totalChunks = chunkSuppliers.size();

            sendMessage("§7§oIniciando restauração das " + totalChunks + " Chunks!");

            int contador = 0;
            int delay = 1;
            for (int i = 0; i < chunkSuppliers.size(); i++) {
                if (contador >= 9){
                    contador = 0;
                    delay++;
                }
                contador++;
                final Supplier<Chunk> chunkSupplier = chunkSuppliers.get(i);
                final boolean isLastChunk = (i == (chunkSuppliers.size() - 1));
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        chunkSupplier.get();//Load the chunk
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                try {
                                    final Chunk chunk = chunkSupplier.get();
                                    regenChunk(chunk);
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
            sendMessage("§7§oFazendo ultima varredura!");

            ensureAllTilesWereRemoved();
            pauseProcessUntilWeCanContinue();

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