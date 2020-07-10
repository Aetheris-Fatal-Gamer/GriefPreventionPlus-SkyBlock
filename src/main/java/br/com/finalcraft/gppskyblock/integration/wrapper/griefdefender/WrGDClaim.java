package br.com.finalcraft.gppskyblock.integration.wrapper.griefdefender;

import br.com.finalcraft.gppskyblock.integration.IClaim;
import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.claim.GDClaim;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class WrGDClaim implements IClaim {

    public final GDClaim claim;

    public WrGDClaim(GDClaim claim) {
        this.claim = claim;
    }

    @Override
    public World getWorld() {
        return claim.getWorld();
    }

    @Override
    public String getID() {
        return claim.getUniqueId().toString();
    }

    @Override
    public Location getGreaterBoundaryCorner() {
        Vector3i vector3i = claim.getGreaterBoundaryCorner();
        return new Location(claim.getWorld(), vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    @Override
    public Location getLesserBoundaryCorner() {
        Vector3i vector3i = claim.getLesserBoundaryCorner();
        return new Location(claim.getWorld(), vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    @Override
    public String locationToString() {
        return "[" + (this.getWorld() != null ? this.getWorld().getName() : "(Invalid world " + this.claim.getWorldUniqueId() + ")") + ", " + this.getLesserBoundaryCorner().getX() + "," + this.getLesserBoundaryCorner().getZ() + "~" + this.getGreaterBoundaryCorner().getX() + "," + this.getGreaterBoundaryCorner().getZ() + "]";
    }

    @Override
    public int getArea() {
        return claim.getArea();
    }

    @Override
    public boolean canEnter(Player player) {
        return true;
        //claim.canEnclose()
        //return claim.canEnter(player) != null;
    }

    @Override
    public boolean contains(Location location, boolean excludeSubdivisions) {
        return claim.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ(), excludeSubdivisions);
    }

    @Override
    public void setPermission(UUID uuid, String permission) {
        switch (permission){
            case "ENTRY":
                //claim.addUserTrust(uuid, )
                //claim.setPermission(uuid, ClaimPermission.ENTRY);
                break;
        }
    }

    @Override
    public void dropPermission(UUID uuid) {
        claim.removeAllTrustsFromUser(uuid);
    }

    @Override
    public boolean isPublicEntryTrust() {
        HashSet context = new HashSet();
        context.add(new Context(ContextKeys.CLAIM, "check-entrytrust"));
        return claim.getFlagPermissionValue(Flags.ENTER_CLAIM, context).asBoolean();
    }

    @Override
    public void setPublicEntryTrust(boolean value) {
        HashSet context = new HashSet();
        context.add(new Context(ContextKeys.CLAIM, "change-entrytrust"));
        claim.setFlagPermission(Flags.ENTER_CLAIM, Tristate.fromBoolean(value), context);
    }
}
