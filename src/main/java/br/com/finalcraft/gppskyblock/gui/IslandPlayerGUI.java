package br.com.finalcraft.gppskyblock.gui;

import br.com.finalcraft.evernifecore.config.playerdata.PlayerController;
import br.com.finalcraft.evernifecore.gui.PlayerGui;
import br.com.finalcraft.evernifecore.gui.util.EnumStainedGlassPane;
import br.com.finalcraft.evernifecore.itemstack.FCItemFactory;
import br.com.finalcraft.evernifecore.placeholder.replacer.RegexReplacer;
import br.com.finalcraft.evernifecore.util.FCBukkitUtil;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.config.playerdata.SBPlayerData;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IslandPlayerGUI extends PlayerGui<SBPlayerData, Gui> {

    protected final int INFO = 4;
    protected final int TP_ISLAND_SPAWN = 10;
    protected final int PUBLIC = 13;
    protected final int SET_ISLAND_SPAWN = 19;
    protected final int RESET = 16;

    private static final RegexReplacer<Island> REGEX_REPLACER = new RegexReplacer<Island>()
            .addParser("owner", Island::getOwnerName)
            .addParser("public_state", island -> island.getClaim().isPublicEntryTrust() ? "§2§lPublica" : "§9§lPrivada!")
            ;

    public IslandPlayerGUI(Player player, @Nullable Island island) {
        super(PlayerController.getPDSection(player, SBPlayerData.class), Gui.gui()
                .title("➲  §0§lSkyBlock Islands")
                .rows(6)
                .disableAllInteractions()
                .create());

        boolean isPublic = island != null && island.getClaim().isPublicEntryTrust();

        drawnBackground(getGui());

        Material CUSTOM_ICON = Material.matchMaterial("EVERPOKEUTILS_CUSTOMICON");
        getGui().setItem(INFO,
                FCItemFactory.from(Material.PAPER)
                        .applyIf(() -> CUSTOM_ICON != null, builder -> builder.material(CUSTOM_ICON).durability(12))
                        .displayName("§a§l✞ §7§lCartaz§a§l ✞")
                        .lore(
                                "§7§m--------§7§l< §a§lBoletim Informativo §7§l>§7§m------ --",
                                "",
                                "§2  ♠ §7Existem §a%gppskyblock_total_ilhas% §7ilhas nesse Servidor",
                                "",
                                "§2   §l[Clique Aqui]§2 para ver a lista de todos os",
                                "§2  §2comandos de ilhas e suas respectivas funções!",
                                "",
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------"
                        )
                        .asGuiItem()
                        .setAction(event -> FCBukkitUtil.makePlayerExecuteCommand(player, "is help"))
        );

        getGui().setItem(TP_ISLAND_SPAWN,
                FCItemFactory.from("BED")
                        .displayName("§b§l☀ §a§lIsland Spawn")
                        .lore(
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------",
                                "",
                                "§2  ♠ §3Teleporta você para a sua ilha!",
                                "",
                                "",
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------"
                        )
                        .asGuiItem()
                        .setAction(event -> FCBukkitUtil.makePlayerExecuteCommand(player, "is spawn"))
        );

        GuiItem lockGuiItem = getLockItem(isPublic);
        lockGuiItem.setAction(event -> {
            Island theIsland = getPlayerData().getIsland();
            boolean publicState =  theIsland != null && theIsland.getClaim().isPublicEntryTrust();

            FCBukkitUtil.makePlayerExecuteCommand(player, "is " + (publicState ? "private" : "public"));

            publicState =  theIsland != null && theIsland.getClaim().isPublicEntryTrust(); //Check again after the command being executed
            getGui().updateItem(PUBLIC, getLockItem(publicState).getItemStack());//Only update the ItemStack of the previous GuiItem
        });

        getGui().setItem(PUBLIC,
                lockGuiItem
        );

        getGui().setItem(SET_ISLAND_SPAWN,
                FCItemFactory.from("COMPASS")
                        .displayName("§b§l☀ §a§lRedefine Spawn")
                        .lore(
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------",
                                "",
                                "§6    Redefine o spawn da sua ilha para a sua",
                                "§6    posição atual!",
                                "",
                                "",
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------"
                        )
                        .asGuiItem()
                        .setAction(event -> FCBukkitUtil.makePlayerExecuteCommand(player, "is setspawn"))
        );

        getGui().setItem(RESET,
                FCItemFactory.from(Material.getMaterial("COMPASS"))
                        .displayName("§b§l☀ §a§lRedefine Spawn")
                        .lore(
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------",
                                "",
                                "§c§l ✘ [Cuidado] ✘       §4§l✘ [Cuidado] ✘",
                                "",
                                "§c     Clicar aqui irá resetar sua ilha!",
                                "§4     Clicar aqui irá resetar sua ilha!",
                                "§c     Clicar aqui irá resetar sua ilha!",
                                "",
                                "§c§l ✘ [Cuidado] ✘       §4§l✘ [Cuidado] ✘",
                                "",
                                "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------"
                        )
                        .asGuiItem()
                        .setAction(event -> FCBukkitUtil.makePlayerExecuteCommand(player, "is reset"))
        );
    }

    private GuiItem getLockItem(boolean isPublic){
        List<String> lorelines = new ArrayList<>();
        lorelines.addAll(
                Arrays.asList(
                        "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------",
                        ""
                )
        );
        if (isPublic){
            lorelines.addAll(
                    Arrays.asList(
                            "§e  ◆ Sua ilha está §2§lPublica",
                            "",
                            "§7 Ou seja, qualquer um pode entrar nela!",
                            "",
                            "§9   §l[Clique Aqui] §9para torná-la privada!"
                    )
            );
        }else {
            lorelines.addAll(
                    Arrays.asList(
                            "§e  ◆ Sua ilha está §9§lPrivada!",
                            "",
                            "§7 Ou seja, apenas jogadores com §n/entrytrust§7 podem entrar nela!",
                            "§7 podem entrar nela!",
                            "",
                            "§2   §l[Clique Aqui] §2para torná-la publica!"
                    )
            );
        }
        lorelines.addAll(
                Arrays.asList(
                        "",
                        "§7§m-------------§7§l< §5§lFinalCraft §7§l>§7§m-------------"
                )
        );

        GuiItem lockGuiItem = FCItemFactory.from("INK_SACK")
                .durability(isPublic ? 10 : 8)
                .displayName("§b§l☀ §a§lIsland Lock")
                .lore(lorelines)
                .asGuiItem();

        return lockGuiItem;
    }

    private void drawnBackground(final Gui gui){
        Arrays.asList(0,1,9, 7,8,17, 36,45,46, 49, 44,52,53).forEach(slot -> gui.setItem(slot, EnumStainedGlassPane.BLACK.asBackground()));
        Arrays.asList(2,3,5,6, 18,27, 26,35, 47,48,50,51).forEach(slot -> gui.setItem(slot, EnumStainedGlassPane.WHITE.asBackground()));
    }

}
