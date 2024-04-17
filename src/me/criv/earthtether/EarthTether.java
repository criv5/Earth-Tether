package me.criv.earthtether;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class EarthTether extends EarthAbility implements AddonAbility {

    private Listener listener;
    private Permission perm;
    private Block fire;
    private final int range = 25;

    public EarthTether(Player player, Location source) {
        super(player);
        //check if bendable block atsome point, then set it to different block type maybe
        //if(isEarth(source.getBlock())) {
            player.sendMessage("1 - block is earth...");
            source.getBlock().setType(Material.MUD);
            if(checkForEntity(player.getEyeLocation(), player.getLocation().getDirection()) != null) {
                player.sendMessage("2 - entity found");
                bPlayer.addCooldown(this);
                start();
            } else {
                remove();
            }
        //} else {
            //remove();
        //}
    }

    private Entity checkForEntity(Location location, Vector direction) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            location.add(direction);
            location.getBlock().setType(Material.GLASS);

            entities.addAll(GeneralMethods.getEntitiesAroundPoint(location, 4)
                    .stream()
                    .filter(entity -> !entity.getUniqueId().equals(player.getUniqueId()))
                    .toList());
            if (!entities.isEmpty()) {
                player.sendMessage("first entity detected thats not you, it was " + entities.get(0));
                entities.get(0).setGlowing(true);
                return entities.get(0);
            }
            player.sendMessage("incrementing blocks... at " + i);
        }
        remove();
        return null;
    }


    private void cwwdheckForPlayer(Location location, Vector direction) {
        for (int i = 0; i < range; i++) {
            location.add(direction);
            location.getBlock().setType(Material.GLASS);
            List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(location, 4);
            player.sendMessage("incrementing blocks... at " + i);
        }
    }

    private void setOnFire(Block block, BlockFace blockFace) {
        fire = block.getRelative(blockFace);
        fire.setType(Material.FIRE);
    }

    @Override
    public void progress() {
        Location loc = fire.getLocation();
        loc.add(0.5,0.5,0.5);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.1,0.1,0.1);

        if(getStartTime() + 3000 < System.currentTimeMillis()) {
            remove();
        }
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return 2000;
    }

    @Override
    public String getName() {
        return "EarthTether";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public void load() {
        listener = new EarthStackListener();
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);

        perm = new Permission("bending.ability.earthtether");
        perm.setDefault(PermissionDefault.OP);
        ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(listener);
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
    }

    @Override
    public String getAuthor() {
        return "Criv";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
