package br.com.finalcraft.gppskyblock.integration.wrapper.griefpreventionplus;

import br.com.finalcraft.gppskyblock.integration.IClaim;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimPermission;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WrGPPClaim implements IClaim {

    private final Claim claim;

    public WrGPPClaim(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }

    @Override
    public World getWorld() {
        return claim.getWorld();
    }

    @Override
    public String getID() {
        return String.valueOf(claim.getID());
    }

    @Override
    public Location getGreaterBoundaryCorner() {
        return claim.getGreaterBoundaryCorner();
    }

    @Override
    public Location getLesserBoundaryCorner() {
        return claim.getLesserBoundaryCorner();
    }

    @Override
    public String locationToString() {
        return claim.locationToString();
    }

    @Override
    public int getArea() {
        return claim.getArea();
    }

    @Override
    public boolean canEnter(Player player) {
        return claim.canEnter(player) != null;
    }

    @Override
    public boolean contains(Location location, boolean excludeSubdivisions) {
        return claim.contains(location,true,excludeSubdivisions);
    }

    @Override
    public void setPermission(UUID uuid, String permission) {
        switch (permission){
            case "ENTRY":
                claim.setPermission(uuid, ClaimPermission.ENTRY);
                break;
        }
    }

    @Override
    public void dropPermission(UUID uuid) {
        claim.dropPermission(uuid);
    }

    @Override
    public boolean isPublicEntryTrust() {
        return claim.getPermission(GriefPreventionPlus.UUID0) == 16;
    }

    @Override
    public void setPublicEntryTrust(boolean value) {
        if (value){
            claim.setPermission(GriefPreventionPlus.UUID0, ClaimPermission.ENTRY);
        }else {
            claim.dropPermission(GriefPreventionPlus.UUID0);
        }
    }
}
