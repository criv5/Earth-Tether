package me.criv.earthtether;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import java.util.*;

import static me.criv.earthtether.EarthTetherListener.*;

public class EarthTether extends EarthAbility implements AddonAbility {

    private Location source;
    private Entity target;

    private Listener listener;
    private Permission perm;
    private final int range = 35;
    private final int interval = 1;
    private final int increment = 3;
    private final int delay = 5;
    private long retractStart;
    private Vector direction;
    private Location location;
    private List<Entity> entities = new ArrayList<>();
    private ArrayList<TempBlock> tempBlocks = new ArrayList<>();
    private Set<Long> extendTimings = new HashSet<>();
    private ArrayList<Long> retractTimings = new ArrayList<>();

    public EarthTether(Player player, Location source) {
        super(player);
        this.source = source;
        this.player = player;
        if(!bPlayer.canBend(this)) return;
        start();
    }

    @Override
    public void progress() {
        if (hasLeftClickOccurred()) {
            resetLeftClickOccured();
            long clickTime = getCurrentTick();
            this.direction = player.getEyeLocation().getDirection().clone().normalize();
            this.location = source.clone();
            this.retractStart = clickTime + range * interval + delay;
            for (int i = 0; i <= range; i++) {
                extendTimings.add(clickTime + i * interval);
            }
            player.sendMessage(String.valueOf(clickTime + range * interval));
            retractTimings.add(clickTime + range * interval);
            bPlayer.addCooldown(this);
        }
        if (extendTimings.contains(getCurrentTick())) {
            for (int i = 0; i < increment; i++) {
                location.add(direction);
                //player.sendMessage("createline at " + i);
                TempBlock tempBlock = new TempBlock(location.getBlock(), source.getBlock().getType());
                tempBlock.setRevertTime(15000);
                this.tempBlocks.add(tempBlock);
                player.getWorld().playSound(location, Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 2F, 0.1F);

                this.entities.addAll(location.getWorld().getNearbyEntities(location, 2, 1, 2)
                        .stream()
                        .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distance(location)))
                        .filter(entity -> !entity.getUniqueId().equals(player.getUniqueId()) && entity instanceof LivingEntity)
                        .toList());

                if (!this.entities.isEmpty()) {
                    this.target = this.entities.get(0);
                    extendTimings.clear();
                    this.retractStart = getCurrentTick() + delay;
                }
                if(location.distance(source) > range) {
                    this.target = null;
                    extendTimings.clear();
                    this.retractStart = getCurrentTick() + delay;
                }
            }
        }
        if(retractTimings.contains(getCurrentTick())) {
            if (!tempBlocks.isEmpty()) {
                for (int i = 0; i < increment; i++) {
                    //player.sendMessage("starting revert..." + tempBlocks.size());
                    int last = tempBlocks.size() - 1;
                    tempBlocks.get(last).revertBlock();
                    player.getWorld().playSound(tempBlocks.get(last).getLocation(), Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 2F, 0.8F);
                    //if (target == null) player.sendMessage("why null");
                    if (target != null) {
                        target.setVelocity(target.getLocation().toVector().clone().subtract(source.toVector().clone()).normalize().multiply(-this.increment));
                        if (source.distance(target.getLocation()) < 2) target.setVelocity(new Vector(0, 0, 0));
                        //entity.teleport(tempBlocks.get(last).getLocation().add(0.5, 0, 0.5));
                    }
                    tempBlocks.remove(last);
                }
            } else {
                remove();
            }
        }
        if (retractStart == getCurrentTick()) {
            if (!tempBlocks.isEmpty()) {
                for (int i = 0; i < tempBlocks.size(); i++) {
                    retractTimings.add(getCurrentTick() + i * interval + delay);
                }
            }
        }
        if (getStartTime() + 10000 < System.currentTimeMillis() && !hasLeftClickOccurred()) {
            player.sendMessage("time reset");
            //this.source = null;
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

    @Override
    public String getDescription() {
        return "EarthTether is an earth ability that allows earthbenders to pull down entities. Simply sneak to set the source point and left click to create the tether. The tether will pull nearby entities towards the source point.";
    }
}
