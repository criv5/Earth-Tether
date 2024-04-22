package me.criv.earthtether;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
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

    public final int interval = 1;
    private int range; //default 30
    private int increment; //default 3
    private int delay; //default 10
    private long cooldown; //default 3000
    private final Location source;
    private final List<Entity> entities = new ArrayList<>();
    private final ArrayList<TempBlock> tempBlocks = new ArrayList<>();
    private final Set<Long> extendTimings = new HashSet<>();
    private final Set<Long> retractTimings = new HashSet<>();
    private Entity target;
    private Listener listener;
    private Permission perm;
    private long retractStart;
    private Vector direction;
    private Location location;

    public EarthTether(Player player, Location source) {
        super(player);
        this.source = source;
        this.player = player;
        if(!bPlayer.canBend(this)) return;
        setConfig();
        start();
        resetBeenDamaged();
        resetLeftClickOccured();
    }

    public void setConfig() {
        this.range = ConfigManager.getConfig().getInt("ExtraAbilities.Criv.Earth.EarthTether.Range");
        this.increment = ConfigManager.getConfig().getInt("ExtraAbilities.Criv.Earth.EarthTether.Increment");
        this.delay = ConfigManager.getConfig().getInt("ExtraAbilities.Criv.Earth.EarthTether.Delay");
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Criv.Earth.EarthTether.Cooldown");
    }
    @Override
    public void progress() {
        if (hasLeftClickOccurred()) {
            resetLeftClickOccured();
            if(player.getLocation().distance(source) > range) {
                remove();
            }
            long clickTime = getCurrentTick();
            direction = player.getEyeLocation().getDirection().clone().normalize();
            location = source.clone();
            retractStart = clickTime + range * interval + delay;
            for (int i = 0; i <= range; i++) {
                extendTimings.add(clickTime + i * interval);
            }
            retractTimings.add(clickTime + range * interval);
            bPlayer.addCooldown(this);
        }
        if (extendTimings.contains(getCurrentTick())) {
            for (int i = 0; i < increment; i++) {
                location.add(direction);
                TempBlock tempBlock = new TempBlock(location.getBlock(), source.getBlock().getType());
                tempBlock.setRevertTime(15000);
                tempBlocks.add(tempBlock);
                player.getWorld().playSound(location, Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 2F, 0.1F);

                entities.addAll(location.getWorld().getNearbyEntities(location, 2, 1, 2)
                        .stream()
                        .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distance(location)))
                        .filter(entity -> !entity.getUniqueId().equals(player.getUniqueId()) && entity instanceof LivingEntity)
                        .toList());

                if (!entities.isEmpty()) {
                    target = entities.get(0);
                    extendTimings.clear();
                    retractStart = getCurrentTick() + delay;
                }
                if(location.distance(source) > range) {
                    target = null;
                    extendTimings.clear();
                    retractStart = getCurrentTick() + delay;
                }
            }
        }
        if(retractTimings.contains(getCurrentTick())) {
            if (!tempBlocks.isEmpty()) {
                for (int i = 0; i < increment; i++) {
                    int last = tempBlocks.size() - 1;
                    if (target != null && target.getLocation().distance(tempBlocks.get(last).getLocation()) < 5) {
                        target.setVelocity(target.getLocation().toVector().clone().subtract(source.toVector().clone()).normalize().multiply(-increment));
                        if (source.distance(target.getLocation()) < 2) target.setVelocity(new Vector(0, 0, 0));
                        //entity.teleport(tempBlocks.get(last).getLocation().add(0.5, 0, 0.5));
                    }
                    tempBlocks.get(last).revertBlock();
                    player.getWorld().playSound(tempBlocks.get(last).getLocation(), Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 2F, 0.8F);
                    tempBlocks.remove(last);
                }
            } else {
                remove();
            }
        }
        if(hasBeenDamaged()) {
            for (TempBlock block : tempBlocks) {
                block.revertBlock();
                if(block.getBlock().getType() == Material.AIR) {
                    TempFallingBlock tempFallingBlock = new TempFallingBlock(block.getLocation(), source.getBlock().getBlockData(), new Vector(0,0,0), this);
                    tempFallingBlock.tryPlace();
                }
            }
            remove();
        }
        if (retractStart <= getCurrentTick()) {
            if (!tempBlocks.isEmpty()) {
                for (int i = 0; i < tempBlocks.size(); i++) {
                    retractTimings.add(getCurrentTick() + i * interval + delay);
                }
            }
        }
        if (getStartTime() + 10000 < System.currentTimeMillis() && !hasLeftClickOccurred())
            remove();
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return cooldown;
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
        ConfigManager.getConfig().addDefault("ExtraAbilities.Criv.Earth.EarthTether.Range", 30);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Criv.Earth.EarthTether.Increment", 3);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Criv.Earth.EarthTether.Delay", 4);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Criv.Earth.EarthTether.Cooldown", 2000);

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
