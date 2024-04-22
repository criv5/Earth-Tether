package me.criv.earthtether;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import static com.projectkorra.projectkorra.ability.EarthAbility.isEarthbendable;

public class EarthTetherListener implements Listener {
    private static boolean leftClickOccurred = false;
    private static boolean beenDamaged = false;
    private static final int sourceRange = 10;

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        Location source = player.getTargetBlock(null, sourceRange).getLocation();
        if (!source.getBlock().getType().isSolid())
            source.add(0, -1, 0);
        if(!isEarthbendable(player, source.getBlock())) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer == null) return;
            if (bPlayer.canBend(CoreAbility.getAbility(EarthTether.class))) {
                EarthTether et = CoreAbility.getAbility(player, EarthTether.class);
                if (et != null)
                    et.remove();
                new EarthTether(player, source);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if(event.getAction() != Action.LEFT_CLICK_AIR) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
        if (bPlayer == null) return;
        if((bPlayer.canBend(CoreAbility.getAbility(EarthTether.class))) && CoreAbility.hasAbility(event.getPlayer(), EarthTether.class)) {
            leftClickOccurred = true;
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;
        if((CoreAbility.hasAbility(player, EarthTether.class))) {
                beenDamaged = true;
            }
        }

    public static boolean hasLeftClickOccurred() {
        return leftClickOccurred;
    }

    public static void resetLeftClickOccured() {
        leftClickOccurred = false;
    }

    public static boolean hasBeenDamaged() {
        return beenDamaged;
    }

    public static void resetBeenDamaged() {
        beenDamaged = false;
    }
}
