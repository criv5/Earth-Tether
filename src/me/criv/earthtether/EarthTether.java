package me.criv.earthtether;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EarthTether extends EarthAbility implements AddonAbility {

    private Location source;

    private Listener listener;
    private Permission perm;
    private final int sourceRange = 20;
    private final int range = 35;
    private final long interval = 1;
    ArrayList<TempBlock> tempBlocks = new ArrayList<>();

    public EarthTether(Player player, Location source) {
        super(player);
        this.source = source;
        this.player = player;
            start();
    }

    @Override
    public void progress() {
        if(EarthTetherListener.hasLeftClickOccurred()) {
            EarthTetherListener.resetLeftClickOccured();

            Entity target = checkForEntity(player.getEyeLocation(), player.getEyeLocation().getDirection());
            if (source == null || target == null) {
                remove();
                return;
            }

            player.sendMessage("we saw that click, we gonna start right now");
            bPlayer.addCooldown(this);

            createLine(target.getLocation(), source, source.getBlock().getType(), interval, target);
            this.source = null;
            remove();
        }
        if (getStartTime() + 10000 < System.currentTimeMillis() && !EarthTetherListener.hasLeftClickOccurred()) {
            player.sendMessage("time reset");
            this.source = null;
            remove();
        }
    }

    public Entity checkForEntity(Location location, Vector direction) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            location.add(direction);
            entities.addAll(GeneralMethods.getEntitiesAroundPoint(location, 4)
                    .stream()
                    .filter(entity -> !entity.getUniqueId().equals(player.getUniqueId()))
                    .toList());
            if (!entities.isEmpty()) {
                player.sendMessage("first entity detected thats not you, it was " + entities.get(0));
                entities.get(0).setGlowing(true);
                return entities.get(0);
            }
            player.sendMessage("tracking beam block #: " + i);
        }
        return null;
    }

    public void createLine(Location target, Location source, Material blockType, long interval, Entity entity) {

        Vector direction = target.subtract(0,1,0).toVector().clone().subtract(source.toVector().clone()).normalize();
        double targetDistance = source.distance(target);

        for (int i = 0; i <= targetDistance; i++) {
            Location location = source.clone().add(direction.clone().multiply(i));
            int i1 = i;
            new BukkitRunnable() {
                public void run() {
                    //if(location.getBlock().getType() != Material.AIR) return;
                    player.sendMessage("createline at " + i1);
                    TempBlock tempBlock = new TempBlock(location.getBlock(), source.getBlock().getType());
                    tempBlock.setRevertTime(5000);
                    tempBlocks.add(tempBlock);
                    if(i1 >= targetDistance-1) {
                        player.sendMessage("done now");
                        retractLine(target, source, interval, entity);
                    }
                }
            }.runTaskLater(ProjectKorra.plugin, interval * i);
        }
    }

    public void retractLine(Location target, Location source, long interval, Entity entity) {
        double targetDistance = source.distance(target);

        for (int i = 0; i <= targetDistance; i++) {
            new BukkitRunnable() {
                public void run() {
                   player.sendMessage("starting revert..." + tempBlocks.size());
                    int last = tempBlocks.size() - 1;
                    entity.teleport(tempBlocks.get(last).getLocation().add(0.5,1,0.5));
                    tempBlocks.get(last).revertBlock();
                    tempBlocks.remove(last);
                }
            }.runTaskLater(ProjectKorra.plugin, (10 + i * interval));
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
        listener = new EarthTetherListener();
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
