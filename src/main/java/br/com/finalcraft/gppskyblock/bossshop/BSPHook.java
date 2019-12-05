package br.com.finalcraft.gppskyblock.bossshop;

import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.api.BossShopAPI;
import org.black_ixx.bossshop.core.BSShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BSPHook {

    private static BossShop bs; //BossShopPro Plugin Instance
    private static boolean isEnabled = false;

    public static void initiliaze(JavaPlugin instance){

        Plugin plugin = Bukkit.getPluginManager().getPlugin("BossShopPro"); //Get BossShopPro Plugin

        if(plugin==null){ //Not installed?
            System.out.print("[BSP Hook] BossShopPro was not found... you can download it here: https://www.spigotmc.org/resources/25699/");
            return;
        }

        bs = (BossShop) plugin; //Success :)
        isEnabled  = true;
        instance.getServer().getPluginManager().registerEvents(new BossshopListener(), instance);

    }

    public static void openShop(Player player, String shopName){
        BSShop shop = BSPHook.getBSPAPI().getShop(shopName);
        BSPHook.getBSPAPI().openShop(player, shop);
    }


    public static BossShopAPI getBSPAPI(){
        return bs.getAPI(); //Returns BossShopPro API
    }


    public static boolean isEnabled(){
        return isEnabled;
    }
}