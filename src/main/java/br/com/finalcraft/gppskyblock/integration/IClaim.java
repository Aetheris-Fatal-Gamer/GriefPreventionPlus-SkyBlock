package br.com.finalcraft.gppskyblock.integration;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface IClaim {

    public World getWorld();

    public String getID();

    public Location getGreaterBoundaryCorner();

    public Location getLesserBoundaryCorner();

    public String locationToString();

    public int getArea();

    public boolean canEnter(Player player);

    public boolean contains(Location location, boolean excludeSubdivisions);

    public void setPermission(UUID uuid, String permission);

    public void dropPermission(UUID uuid);

    public boolean isPublicEntryTrust();

    public void setPublicEntryTrust(boolean value);
}
