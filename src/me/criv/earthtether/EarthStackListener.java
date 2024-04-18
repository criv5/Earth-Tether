package me.criv.earthtether;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class EarthTetherListener implements Listener {
    private int sourceRange = 25;
    Location source = null;
    private static boolean leftClickOccurred = false;

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        source = player.getTargetBlock(null, sourceRange).getLocation();
        if (!source.getBlock().getType().isSolid())
            source.add(0, -1, 0);
        if (source.getBlock().getType() != Material.AIR) {
            //TempBlock tempBlock = new TempBlock(source.getBlock(), Material.MUD);
            //tempBlock.setRevertTime(10000);
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer == null) return;
            if (bPlayer.canBend(CoreAbility.getAbility(EarthTether.class))) {
                EarthTether et = CoreAbility.getAbility(player, EarthTether.class);
                if (et != null)
                    et.remove();
                new EarthTether(player, source);
            }
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if(event.getAction() != Action.LEFT_CLICK_AIR) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
        if (bPlayer == null) return;
        if(bPlayer.canBend(CoreAbility.getAbility(EarthTether.class))) {
            leftClickOccurred = true;
        }
    }

    public static boolean hasLeftClickOccurred() {
        return leftClickOccurred;
    }

    public static void resetLeftClickOccured() {
        leftClickOccurred = false;
    }
}
