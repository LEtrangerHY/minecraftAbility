package org.core.effect.effectPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;

public class moveCancelManager implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (Stiff.isStiff(player)) {
            Location from = e.getFrom();
            Location to = e.getTo();

            if (to == null) return;

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {

                Location fixedLocation = from.clone();

                fixedLocation.setYaw(to.getYaw());
                fixedLocation.setPitch(to.getPitch());

                e.setTo(fixedLocation);
            }
        }
    }
}