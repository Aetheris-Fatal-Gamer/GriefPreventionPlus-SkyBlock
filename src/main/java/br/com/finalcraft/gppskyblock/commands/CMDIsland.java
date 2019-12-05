package br.com.finalcraft.gppskyblock.commands;

import br.com.finalcraft.evernifecore.FCBukkitUtil;
import br.com.finalcraft.evernifecore.argumento.MultiArgumentos;
import br.com.finalcraft.evernifecore.config.playerdata.PlayerController;
import br.com.finalcraft.evernifecore.config.playerdata.PlayerData;
import br.com.finalcraft.evernifecore.cooldown.Cooldown;
import br.com.finalcraft.evernifecore.fancytext.FancyText;
import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.Utils;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimPermission;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.events.ClaimDeleteEvent;
import br.com.finalcraft.gppskyblock.PermissionNodes;
import br.com.finalcraft.gppskyblock.bossshop.BSPHook;
import br.com.finalcraft.gppskyblock.tasks.SpawnTeleportTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CMDIsland implements CommandExecutor {

    private Map<String,String> confirmations = new HashMap<String,String>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        //  Passando os argumentos para um ArrayList
        MultiArgumentos argumentos = FCBukkitUtil.parseBukkitArgsToMultiArgumentos(args);

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
            case "biomelist":
                return biomelist(label,sender,argumentos);
            case "setbiome":
                return setbiome(label,sender,argumentos);
            case "private":
                return privatec(label,sender,argumentos);
            case "public":
                return publicc(label,sender,argumentos);
            case "reset":
                return reset(label,sender,argumentos);
            case "delete":
                return delete(label,sender,argumentos);
            case "setraio":
                return setraio(label,sender,argumentos);
            case "reload":
                return reload(label,sender,argumentos);

        }

        sender.sendMessage("&cErro de parametros, por favor use /" + label + " help");
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command Help
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean help(String label, CommandSender sender, MultiArgumentos argumentos){

        if (argumentos.get(0).isEmpty() && sender instanceof Player && BSPHook.isEnabled()){
            Player player = (Player) sender;
            BSPHook.openShop(player,"islands");
            return true;
        }

        sender.sendMessage("§6§m------------§6(  §a§lGPPSkyBlock§e  §6)§m------------");

        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " help","§bMostra essa mensagem!","/" + label + " help",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " spawn [Player]","§bTeleporta para a sua ilha ou a de algum jogador!","/" + label + " spawn",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " setspawn","§bAltera a localização do Spawn da sua ilha!","/" + label + " setspawn",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " private","§bDeixa a sua ilha Privada!","/" + label + " private",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " public","§bDeixa a sua ilha Pública!","/" + label + " public",true));
        FancyText.sendTo(sender, new FancyText("§3§l ▶ §a/" + label + " reset","§bReseta a sua ilha!(Apaga ela inteira!!!)","/" + label + " reset",true));

        if (sender.hasPermission(PermissionNodes.COMMAND_SETRADIUS))
            FancyText.sendTo(sender, new FancyText("§6§l ▶ §e/" + label + " setraio <Player> <Raio>","§bAltera o tamanho do raio da ilha!","/" + label + " setraio",true));

        if (sender.hasPermission(PermissionNodes.COMMAND_SETBIOME)) {
            FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " biomelist","§bMostra os possíveis biomas!","/" + label + " biomelist",true));
            if (sender.hasPermission(PermissionNodes.COMMAND_SETBIOME_OTHER)){
                FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiome <Bioma> [Player]","§bDefine o bioma de toda a sua ilha!","/" + label + " setbiome ",true));
            }else {
                FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiome <Bioma>","§bDefine o bioma de toda a sua ilha!","/" + label + " setbiome ",true));
            }
        }

        if (sender.hasPermission("be.evernife")){
            FancyText.sendTo(sender, new FancyText("§5§l ▶ §6/" + label + " delete <Player>","§bDeleta a ilha de algum jogador!\n\nNá pratica nao deleta fisicamente, apenas remove o claim!\nFazendo com que ele tenha que criar uma nova ilha em outro lugar.","/" + label + " biomelist",true));
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

            if (island.getClaim().canEnter(thePlayer) != null) {
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

        if (!island.getClaim().contains(thePlayer.getLocation(), true, false)) {
            sender.sendMessage("§4§l ▶ §cVocê precisa estar dentro da sa ilha para usar esse comando!");
            return false;
        }

        if (!island.ready) {
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return false;
        }

        try {
            island.setSpawn(thePlayer.getLocation().add(0, 2, 0));
            sender.sendMessage("§3§l ▶ §aSpawn da ilha definido com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED+"An error occurred while creating the island: data store issue.");
            return false;
        }
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

                if(!sender.hasPermission("gppskyblock.setbiome."+biome.toString())) {
                    continue;
                }

                sb.append(Utils.fromSnakeToCamelCase(biome.toString()));
                sb.append(", ");
            }
        }

        sender.sendMessage(sb.substring(0, sb.length()-2).toString());
        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------------------//
    // Command setbiome
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean setbiome(String label, CommandSender sender, MultiArgumentos argumentos){

        if ( !FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_SETBIOME)){
            return true;
        }

        PlayerData playerData;

        if (sender.hasPermission(PermissionNodes.COMMAND_SETBIOME_OTHER) && !argumentos.get(2).isEmpty()){
            playerData = argumentos.get(2).getPlayerData();
            if (playerData == null){
                sender.sendMessage("§4§l ▶ §cNão existem nenhum jogador chamado [" + argumentos.get(1) + "] !");
                return true;
            }
        }else {
            if ( !(sender instanceof Player) ){
                FancyText.sendTo(sender, new FancyText("§3§l ▶ §b/" + label + " setbiome <Bioma> [Player]","§bDefine o bioma de toda a sua ilha!","/" + label + " setbiome ",true));
                return true;
            }

            playerData = PlayerController.getPlayerData((Player) sender);
        }

        Biome biome = Utils.matchAllowedBiome(argumentos.get(1).toString());

        if (biome == null) {
            sender.sendMessage("§4§l ▶ §cNão existe nenhum bioma chamado §e" + argumentos.get(1) + ". Use /" + label + " biomelist");
            return true;
        }

        if (!sender.hasPermission("gppskyblock.setbiome.all") && !FCBukkitUtil.hasThePermission(sender,"gppskyblock.setbiome." + argumentos.get(1).toLowerCase())){
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

        Claim claim = island.getClaim();
        claim.setPermission(player.getUniqueId(), ClaimPermission.ENTRY);
        claim.dropPermission(GriefPreventionPlus.UUID0);
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

        Claim claim = island.getClaim();
        claim.dropPermission(player.getUniqueId());
        claim.setPermission(GriefPreventionPlus.UUID0, ClaimPermission.ENTRY);
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

        Cooldown cooldown = new Cooldown("GPPSkyBlock-ISRESET",player.getName(),259200);//3 Dias

        if (cooldown.isInCooldown()){
            cooldown.warnPlayer(sender);
            return true;
        }
        cooldown.setPermaCooldown(true);
        cooldown.start();

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
            sender.sendMessage("§4§l ▶ §cExiste alguma operação pendente nessa ilha!");
            return true;
        }

        ClaimDeleteEvent event = new ClaimDeleteEvent(island.getClaim(), (sender instanceof Player ? (Player) sender : null) , ClaimDeleteEvent.Reason.DELETE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
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
    // Command Reload
    // -----------------------------------------------------------------------------------------------------------------------------//
    public boolean reload(String label, CommandSender sender, MultiArgumentos argumentos){

        if ( !FCBukkitUtil.hasThePermission(sender,PermissionNodes.COMMAND_RELOAD)){
            return true;
        }
        sender.sendMessage("§aEsse plugin não pode ser recarregado!");
        return true;
    }
}
