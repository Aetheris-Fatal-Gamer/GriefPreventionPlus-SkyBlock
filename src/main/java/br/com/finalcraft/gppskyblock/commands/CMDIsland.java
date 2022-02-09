package br.com.finalcraft.gppskyblock.commands;

import br.com.finalcraft.evernifecore.argumento.MultiArgumentos;
import br.com.finalcraft.evernifecore.config.playerdata.PlayerController;
import br.com.finalcraft.evernifecore.config.playerdata.PlayerData;
import br.com.finalcraft.evernifecore.cooldown.Cooldown;
import br.com.finalcraft.evernifecore.fancytext.FancyText;
import br.com.finalcraft.evernifecore.integration.everforgelib.EverForgeLibIntegration;
import br.com.finalcraft.evernifecore.util.FCBukkitUtil;
import br.com.finalcraft.evernifecore.version.ServerType;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.PermissionNodes;
import br.com.finalcraft.gppskyblock.Utils;
import br.com.finalcraft.gppskyblock.config.datastore.gpp.DataStoreGPPMysql;
import br.com.finalcraft.gppskyblock.config.datastore.gpp.DataStoreGPPYML;
import br.com.finalcraft.gppskyblock.gui.IslandPlayerGUI;
import br.com.finalcraft.gppskyblock.integration.GPPluginBase;
import br.com.finalcraft.gppskyblock.integration.IClaim;
import br.com.finalcraft.gppskyblock.tasks.SpawnTeleportTask;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CMDIsland implements CommandExecutor {

    private Map<String,String> confirmations = new HashMap<String,String>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        //  Passando os argumentos para um ArrayList
        MultiArgumentos argumentos = new MultiArgumentos(args, true);

        switch (argumentos.get(0).toLowerCase()){
            case "":
            case "?":
            case "help":
                return help(label,sender,argumentos);
            case "spawn":
            case "home":
            case "tp":
                return spawn(label,sender,argumentos);
            case "setspawn":
                return setspawn(label,sender,argumentos);
            case "invite":
                return invite(label,sender,argumentos);
            case "biomelist":
                return biomelist(label,sender,argumentos);
            case "setbiomeother":
                return setbiomeother(label,sender,argumentos);
            case "setbiomeisland":
                return setbiomeisland(label,sender,argumentos);
            case "setbiomechunk":
                return setbiomechunk(label,sender,argumentos);
            case "private":
                return privatec(label,sender,argumentos);
            case "public":
                return publicc(label,sender,argumentos);
            case "reset":
                return reset(label,sender,argumentos);
            case "hardreset":
                return hardreset(label,sender,argumentos);
            case "delete":
                return delete(label,sender,argumentos);
            case "setraio":
                return setraio(label,sender,argumentos);
            case "transfer":
                return transfer(label,sender,argumentos);
            case "convertdatabase":
                return convertdatabase(label,sender,argumentos);
            case "reload":
                return reload(label,sender,argumentos);

        }

        PlayerData playerData = argumentos.get(0).getPlayerData();
        if (playerData != null){
            FCBukkitUtil.makePlayerExecuteCommand(sender,label + " spawn " + playerData.getPlayerName());
            return true;
        }

        sender.sendMessage("§cErro de parâmetros, por favor use /" + label + " help");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command Help
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean help(String label, CommandSender sender, MultiArgumentos argumentos){

        if (argumentos.get(0).isEmpty() && sender instanceof Player){
            Player player = (Player) sender;
            IslandPlayerGUI playerGUI = new IslandPlayerGUI(player, GPPSkyBlock.getInstance().getIsland(player.getUniqueId()));
            playerGUI.open();
            return true;
        }

        sender.sendMessage("§6§m------------§6(  §a§lGPPSkyBlock§e  §6)§m------------");

        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " help","§bMostra essa mensagem!","/" + label + " help",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " spawn [Player]","§bTeleporta para a sua ilha ou a de algum jogador!","/" + label + " spawn",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " setspawn","§bAltera a localização do Spawn da sua ilha!","/" + label + " setspawn",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " invite","§bConvida alguem para MORAR na sua ilha (dando TRUST para ela)!","/" + label + " invite",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " private","§bDeixa a sua ilha Privada!","/" + label + " private",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " public","§bDeixa a sua ilha Pública!","/" + label + " public",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " reset","§bReseta a sua ilha!(Apaga ela inteira!!!)","/" + label + " reset",true));

        if (sender.hasPermission(PermissionNodes.COMMAND_SETRADIUS))
            FancyText.sendTo(sender, new FancyText("§6§l ▶ §e/" + label + " setraio <Player> <Raio>","§bAltera o tamanho do raio da ilha!","/" + label + " setraio",true));

        if (sender.hasPermission(PermissionNodes.COMMAND_SETBIOME)) {
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomeisland <Bioma>","§bDefine o bioma de toda a sua ilha!","/" + label + " setbiome ",true));
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomechunk <Bioma>","§bDefine o bioma da chunk que você está dentro!","/" + label + " setbiomechunk ",true));
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " biomelist","§bMostra os possíveis biomas!","/" + label + " biomelist",true));
        }

        if (sender.hasPermission(PermissionNodes.COMMAND_SETBIOME_OTHER)){
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomeother <Player> <Bioma>","§bDefine o bioma de toda a ilha de um jogador!","/" + label + " setbiome ",true));
        }

        if (sender.hasPermission(PermissionNodes.COMMAND_DELETE_OTHER)){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " delete <Player>","§bDeleta a ilha de algum jogador!\n\nNá pratica não deleta fisicamente na hora, apenas remove o claim!\n\nFazendo com que o jogador tenha que criar uma nova ilha em outro lugar.\n\nNota: A ilha (construção fisica) será deletada no próximo restart!","/" + label + " delete",true));
        }

        if (sender.hasPermission(PermissionNodes.COMMAND_TRANSFERISLAND_OTHER)){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " transfer <oldOwner> <newOwner>","§bTransfere a ilha de um jogador para outro jogador!\n","/" + label + " transfer ",true));
        }

        if (sender.hasPermission(PermissionNodes.COMMAND_CONVERTDATABASE)){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " convertdatabase","§bConverte o banco de dados de MYSQL para YML!\n","/" + label + " convertdatabase ",true));
        }

        sender.sendMessage("");
        sender.sendMessage("§3§oPasse o mouse em cima dos comandos para ver a descrição!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command spawn
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean spawn(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)) {
            return false;
        }

        Player thePlayer = (Player) sender;

        Island island = null;
        if (!argumentos.get(1).isEmpty()){
            PlayerData playerData = argumentos.get(1).getPlayerData();
            if (playerData == null){
                sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
                return true;
            }

            island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

            if (island == null) {
                sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
                return false;
            }

            if (!thePlayer.hasPermission(PermissionNodes.ADMIN_PERM) && island.getClaim().canEnter(thePlayer)) {
                sender.sendMessage("§4§l ▶ §c Você não tem permissão para entrar nessa ilha!");
                return false;
            }
        }else {
            island = GPPSkyBlock.getInstance().getDataStore().getIsland(thePlayer.getUniqueId());

            if (island==null) {
                // this player doesn't have an island yet... so create a new island
                try {
                    island = GPPSkyBlock.getInstance().getDataStore().createIsland(thePlayer.getUniqueId());
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED+ "Um erro ocorreu ao gerar sua ilha: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        if (thePlayer.hasPermission("gppskyblock.nowarpup")){
            sender.sendMessage("§3§l ▶ §aVocê foi teleportado para a ilha!");
            SpawnTeleportTask.teleportTask(thePlayer, island, 0);
        }else {
            sender.sendMessage("§3§l ▶ §aVocê será teleportado em " + GPPSkyBlock.getInstance().config().tpCountdown + " segundos!");
            SpawnTeleportTask.teleportTask(thePlayer, island, GPPSkyBlock.getInstance().config().tpCountdown);
        }

        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setspawn
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setspawn(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)) {
            return false;
        }

        Player thePlayer = (Player) sender;

        sender.sendMessage(ChatColor.RED + "ATENÇAO: Certifique-se de usar blocos inteiros para o spawn de sua ilha! Não use escadas ou lajes!");

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(thePlayer.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §cVocê ainda não possui uma ilha nesse servidor! Para criar uma, use \"/"+label+" spawn\"");
            return false;
        }

        if (!island.getClaim().contains(thePlayer.getLocation(), false)) {
            sender.sendMessage("§4§l ▶ §cVocê precisa estar dentro da sa ilha para usar esse comando!");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        try {
            Integer ycoord = Math.max(0, thePlayer.getLocation().getBlockY()) + 2;
            Location spawnLoc = new Location(thePlayer.getWorld(), thePlayer.getLocation().getBlockX(),ycoord, thePlayer.getLocation().getBlockZ());
            island.setSpawn(spawnLoc);
            sender.sendMessage("§3§l ▶ §aSpawn da ilha definido com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED+"An error occurred while creating the island: data store issue.");
            return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command invite
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean invite(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)) {
            return true;
        }

        sender.sendMessage("§aO comando invite foi desabilidade! Use o comando §e/TRUST §apara permitir que alguem more na sua casa!");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command biomelist
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean biomelist(String label, CommandSender sender, MultiArgumentos argumentos){

        StringBuilder sb = new StringBuilder(ChatColor.GOLD + "Biome list: " + ChatColor.AQUA);
        if (GPPSkyBlock.getInstance().config().allowedBiomes.isEmpty()) {
            sb.append(ChatColor.RED+"Nenhuma");
        } else {
            for (Biome biome : GPPSkyBlock.getInstance().config().allowedBiomes) {
                sb.append(Utils.fromSnakeToCamelCase(biome.toString()));
                sb.append(", ");
            }
        }

        sender.sendMessage(sb.substring(0, sb.length()-2).toString());
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setbiomeother
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setbiomeother(String label, CommandSender sender, MultiArgumentos argumentos) {

        if (!FCBukkitUtil.hasThePermission(sender, PermissionNodes.COMMAND_SETBIOME_OTHER)) {
            return true;
        }

        if (argumentos.emptyArgs(1, 2)) {
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomeother <Player> <Bioma>", "§bDefine o bioma de toda a ilha de um jogador!", "/" + label + " setbiome ", true));
            return true;
        }

        PlayerData playerData = argumentos.get(1).getPlayerData();
        if (playerData == null){
            sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
            return true;
        }

        Biome biome = Utils.matchAllowedBiome(argumentos.getStringArg(2));

        if (biome == null) {
            sender.sendMessage("§4§l ▶ §cNão existe nenhum bioma chamado §e" + argumentos.getStringArg(2) + ". Use §e/" + label + " biomelist");
            return true;
        }

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        island.setIslandBiome(biome);
        sender.sendMessage("§3§l ▶ §aBioma da ilha do jogador [" + playerData.getPlayerName() + "] alterado com sucesso! Você precisa deslogar e logar para ver a diferença!");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setbiomeisland
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setbiomeisland(String label, CommandSender sender, MultiArgumentos argumentos){

        if ( !FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_SETBIOME)){
            return true;
        }

        if (argumentos.emptyArgs(1)){
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomeisland <Bioma>","§bDefine o bioma de toda a sua ilha!","/" + label + " setbiome ",true));
            return true;
        }

        PlayerData playerData = PlayerController.getPlayerData(sender.getName());

        String biomeName = argumentos.joinStringArgs(1);

        Biome biome = Utils.matchAllowedBiome(biomeName);

        if (biome == null) {
            sender.sendMessage("§4§l ▶ §cNão existe nenhum bioma chamado §e" + biomeName + ". Use §e/" + label + " biomelist");
            return true;
        }

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        Cooldown cooldown = playerData.getCooldown("BIOME_ISLAND");
        if (cooldown.isInCooldown()){
            cooldown.warnPlayer(sender);
            return true;
        }

        island.setIslandBiome(biome);
        cooldown.setPersist(true);
        cooldown.startWith(3600);
        sender.sendMessage("§3§l ▶ §aBioma alterado! Você vai precisar deslogar e logar para ver a diferença!");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setbiomechunk
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setbiomechunk(String label, CommandSender sender, MultiArgumentos argumentos){

        if ( !FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_SETBIOME)){
            return true;
        }

        if (argumentos.emptyArgs(1)){
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiomechunk <Bioma>","§bDefine o bioma da chunk que você está dentro!","/" + label + " setbiomechunk ",true));
            return true;
        }

        PlayerData playerData = PlayerController.getPlayerData(sender.getName());

        String biomeName = argumentos.joinStringArgs(1);

        Biome biome = Utils.matchAllowedBiome(biomeName);

        if (biome == null) {
            sender.sendMessage("§4§l ▶ §cNão existe nenhum bioma chamado §e" + biomeName + ". Use §e/" + label + " biomelist");
            return true;
        }

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        Cooldown cooldown = playerData.getCooldown("BIOME_ISLAND");
        if (cooldown.isInCooldown()){
            cooldown.warnPlayer(sender);
            return true;
        }

        Player player = playerData.getPlayer();

        if (!island.getClaim().contains(player.getLocation(), false)){
            sender.sendMessage("§4§l ▶ §cVocê precisa estar dentro da sua ilha para fazer isso!");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        island.setChunkBiome(biome, chunk.getX(), chunk.getZ());
        cooldown.setPersist(true);
        cooldown.startWith(60);

        sender.sendMessage("§3§l ▶ §aBioma alterado! Você vai precisar deslogar e logar para ver a diferença!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command privatec
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean privatec(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)){
            return true;
        }

        Player player = (Player) sender;

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §cVocê ainda não possui uma ilha nesse servidor! Para criar uma, use \"/"+label+" spawn\"");
            return false;
        }

        IClaim claim = island.getClaim();
        claim.setPermission(player.getUniqueId(), "ENTRY");
        claim.setPublicEntryTrust(false);
        sender.sendMessage("§6§l ▶ §eSua ilha está §9§lPrivada!");
        sender.sendMessage("§7§oOu seja, apenas jogadores com §n/entrytrust §7podem entrar nela!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command publicc
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean publicc(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)){
            return true;
        }

        Player player = (Player) sender;

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §cVocê ainda não possui uma ilha nesse servidor! Para criar uma, use \"/"+label+" spawn\"");
            return false;
        }

        IClaim claim = island.getClaim();
        claim.dropPermission(player.getUniqueId());
        claim.setPublicEntryTrust(true);
        sender.sendMessage("§6§l ▶ §eSua ilha está §a§lPublica!");
        sender.sendMessage("§7§oOu seja, qualquer um pode entrar nela!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command reset
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean reset(String label, CommandSender sender, MultiArgumentos argumentos){

        if (FCBukkitUtil.isNotPlayer(sender)){
            return true;
        }

        Player player = (Player) sender;

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §cVocê ainda não possui uma ilha nesse servidor! Para criar uma, use \"/"+label+" spawn\"");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        String conf = confirmations.remove(player.getName());

        if (conf == null || !conf.equals("reset")) {
            sender.sendMessage("§4§l ▶ §c§lCUIDADO: §cSua ilha inteira será APAGADA!\n§cSe você tem certeza disso, use \"/"+label+" reset\" novamente!");
            this.confirmations.put(player.getName(), "reset");
            return false;
        }

        PlayerData playerData = PlayerController.getPlayerData(player);

        Cooldown cooldown = playerData.getCooldown("GPPSkyBlock-ISRESET");
        if (!player.hasPermission(PermissionNodes.COMMAND_RESET_NOCOOLDOWN) && cooldown.isInCooldown()){
            cooldown.warnPlayer(sender);
            return true;
        }
        cooldown.setPersist(true);
        cooldown.startWith(259200);//3 Dias
        island.reset();
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command hardreset
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean hardreset(String label, CommandSender sender, MultiArgumentos argumentos){

        if (true){
            sender.sendMessage("§4§l ▶ §cPeça para um ADM realizar um HardReset para vc!");
            return true;
        }

        if (FCBukkitUtil.isNotPlayer(sender)){
            return true;
        }

        Player player = (Player) sender;

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(player.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §cVocê ainda não possui uma ilha nesse servidor! Para criar uma, use \"/"+label+" spawn\"");
            return false;
        }

        String conf = confirmations.remove(player.getName());

        if (conf == null || !conf.equals("hardreset")) {
            sender.sendMessage("§4§l ▶ §c§lCUIDADO: §cSua ilha inteira será APAGADA!" +
                    "\n§cSe você tem certeza disso, use \"/"+label+" hardreset\" novamente!\n\nVocê só poderá fazer isso 1 vez!");
            this.confirmations.put(player.getName(), "hardreset");
            return false;
        }

        PlayerData playerData = PlayerController.getPlayerData(player);

        Cooldown cooldown = playerData.getCooldown("GPPSkyBlock-ISHARDRESET");
        if (cooldown.isInCooldown()){
            cooldown.warnPlayer(sender);
            return true;
        }
        cooldown.setPersist(true);
        cooldown.startWith(86313600);//999 Dias
        island.reset();
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command delete
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean delete(String label, CommandSender sender, MultiArgumentos argumentos){

        if (!FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_DELETE_OTHER)){
            return true;
        }

        PlayerData playerData = argumentos.get(1).getPlayerData();

        if (playerData == null){
            sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
            return true;
        }

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return true;
        }

        if (!island.ready) {
            if (!argumentos.getFlag("force").isSet()){
                sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
                FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " delete <Player> -force","§bDeleta a ilha de algum jogador!\n\nNá pratica não deleta fisicamente na hora, apenas remove o claim!\n\nFazendo com que o jogador tenha que criar uma nova ilha em outro lugar.\n\nNota: A ilha (construção fisica) será deletada no próximo restart!","/" + label + " delete",true));
                return true;
            }else {
                island.ready = true;
            }
        }

        if (sender instanceof Player && !argumentos.getFlag("confirm").isSet()){
            FancyText.sendTo(sender,
                    new FancyText("§c§l ▶ §cDeletar ilha do jogador: §e" + playerData.getPlayerName() + " §cVocê tem certeza? ").setHoverText("§bDeixa a sua ilha Pública!"),
                    new FancyText("§c[§lConfirmar§c]","§bVai deletar memo irmão? Tem certeza?\n\nClica ai então...","/" + label + " delete " + playerData.getPlayerName() + " -confirm -force",false)
            );
            return true;
        }

        if (GPPluginBase.getInstance().deleteIslandClaim(island.getClaim(), (sender instanceof Player ? (Player) sender : null)) == false) {
            sender.sendMessage("§4§l ▶ §cEssa ilha não pode ser deletada!");
            return true;
        }

        sender.sendMessage("§2§l ▶ §aIlha deletada com sucesso!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setraio
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setraio(String label, CommandSender sender, MultiArgumentos argumentos){

        if (!FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_SETRADIUS)) {
            return true;
        }

        if (argumentos.get(1).isEmpty() || argumentos.get(2).isEmpty()){
            FancyText.sendTo(sender, new FancyText("§6§l ▶ §e/" + label + " setraio <Player> <Raio>","§bAltera o tamanho do raio da ilha!","/" + label + " setraio",true));
            return true;
        }

        PlayerData playerData = argumentos.get(1).getPlayerData();

        if (playerData == null){
            sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
            return true;
        }

        Island island = GPPSkyBlock.getInstance().getDataStore().getIsland(playerData.getUniqueId());

        if (island == null) {
            sender.sendMessage("§4§l ▶ §e" + playerData.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return true;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return true;
        }

        Integer newRadius = argumentos.get(2).getInteger();

        if (newRadius == null ){
            sender.sendMessage("§4§l ▶ §c[" + argumentos.get(2) + "] deve ser um número inteiro positivo menor que 254!");
            return true;
        }

        if (newRadius > 254 || newRadius < 2){
            sender.sendMessage("§4§l ▶ §cO novo tamanho da ilha deve ser menor que 254!");
            return true;
        }

        island.setRadius(newRadius);
        sender.sendMessage("§3§l ▶ §aO novo raio da ilha foi definido para " + newRadius + " blocos de distancia!");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command transfer
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean transfer(String label, CommandSender sender, MultiArgumentos argumentos){

        if (!FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_TRANSFERISLAND_OTHER)) {
            return true;
        }

        PlayerData oldOwner = argumentos.get(1).getPlayerData();
        PlayerData newOwner = argumentos.get(2).getPlayerData();

        if (argumentos.get(1).isEmpty() || argumentos.get(2).isEmpty()){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " transfer <oldOwner> <newOwner>","§bTransfere a ilha de um jogador para outro jogador!\n","/" + label + " transfer ",true));
            return true;
        }

        if (oldOwner == null){
            sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
            return true;
        }

        if (newOwner == null){
            sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(2) + "] !");
            return true;
        }

        Island oldOwnerIsland = GPPSkyBlock.getInstance().getDataStore().getIsland(oldOwner.getUniqueId());
        Island newOwnerIsland = GPPSkyBlock.getInstance().getDataStore().getIsland(newOwner.getUniqueId());

        if (oldOwnerIsland == null) {
            sender.sendMessage("§4§l ▶ §e" + oldOwner.getPlayerName() + "§c não possui uma ilha nesse servidor!");
            return true;
        }

        if (newOwnerIsland != null) {
            sender.sendMessage("§4§l ▶ §e" + newOwner.getPlayerName() + "§e já possui uma ilha nesse servidor! Delete a ilha dele antes de dar uma nova!");
            return true;
        }

        if (!oldOwnerIsland.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha! Transferencia bloqueada!");
            return true;
        }

        try {
            GPPSkyBlock.getInstance().getDataStore().removeIsland(oldOwnerIsland);                                      //Remove old Island!
            GPPluginBase.getInstance().transferIsland(oldOwnerIsland, newOwner.getUniqueId());                          //Transfer the claim to the new owner!
            Island newIsland = new Island(newOwner.getUniqueId(), oldOwnerIsland.getClaim(), oldOwnerIsland.getSpawn());//Create a new one as copy! Using same claim.
            GPPSkyBlock.getInstance().getDataStore().removeIsland(newIsland);                                           //Add new one to database!
        }catch (Exception e){
            sender.sendMessage("§c§l ▶ §cFalha ao transferir ilha...: " + e.getMessage());
            GPPSkyBlock.info("Failed to transfer island from [" + oldOwner.getPlayerName() + "] to [" + newOwner.getPlayerName());
            e.printStackTrace();
        }

        sender.sendMessage("§2§l ▶ §aIlha transferida com sucesso do jogador [§e" + oldOwner.getPlayerName() + "§a] para o jogador [§e" + newOwner.getPlayerName() + "§a]!");
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command convertdatabase
    // -----------------------------------------------------------------------------------------------------------------------------//
    boolean converting = false;
    public boolean convertdatabase(String label, CommandSender sender, MultiArgumentos argumentos){

        if (!FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_CONVERTDATABASE)){
            return true;
        }

        if (!argumentos.getFlag("confirm").isSet()){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §a§l[Clique para confirmar]","§bConverte o banco de dados de MYSQL para YML!\n","/" + label + " convertdatabase -confirm",false));
            return true;
        }

        if (!(GPPSkyBlock.getInstance().getDataStore() instanceof DataStoreGPPMysql)){
            sender.sendMessage("Você só pode converter de DataStoreGPPMysql para DataStoreGPPYML");
            return true;
        }

        if (converting){
            sender.sendMessage("Uma conversão já está em andamento!");
            return true;
        }

        converting = true;
        new Thread(){
            @Override
            public void run() {
                try {
                    DataStoreGPPYML dataStoreGPPYML = new DataStoreGPPYML(GPPSkyBlock.getInstance());
                    int max = GPPSkyBlock.getInstance().getDataStore().islands.size();
                    int current = 0;
                    for (Island island : GPPSkyBlock.getInstance().getDataStore().islands.values()) {
                        current++;
                        try {
                            dataStoreGPPYML.islands.put(island.getOwnerId(), island);
                            dataStoreGPPYML.config.setValue("Islands." + island.getClaim().getID() + ".ownerUUID", island.getOwnerId());
                            dataStoreGPPYML.config.setValue("Islands." + island.getClaim().getID() + ".spawnLocation", island.getSpawn());
                            if (current % 10 == 0){
                                sender.sendMessage("Island conversion [" + current + "/" + max + "] was converted.");
                            }
                        }catch (Exception e){
                            sender.sendMessage("Failed to add island from" + island.getOwnerName());
                            e.printStackTrace();
                        }
                    }
                    dataStoreGPPYML.config.save();
                }catch (Exception e){
                    sender.sendMessage("Failed to create the DataStoreGPPYML");
                    e.printStackTrace();
                }
                converting = false;
            }
        }.start();
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command Reload
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean reload(String label, CommandSender sender, MultiArgumentos argumentos){

        if ( !FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_RELOAD)){
            return true;
        }

        try {
            GPPSkyBlock.getInstance().initializeDataStore();
            sender.sendMessage("§aReload Success!");
        }catch (Exception e){
            sender.sendMessage("§cFailed to reload: " + e.getMessage());
        }
        return true;
    }
}
