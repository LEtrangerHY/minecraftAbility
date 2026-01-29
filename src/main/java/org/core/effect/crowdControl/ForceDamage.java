package org.core.effect.crowdControl;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class ForceDamage implements Effects, Listener {
    private final LivingEntity target;
    private final double damage;
    private final DamageSource damageSource;
    private final boolean isNonKnock;

    public ForceDamage(LivingEntity target, double damage, DamageSource damageSource, boolean isNonKnock) {
        this.target = target;
        this.damage = damage;
        this.damageSource = damageSource;
        this.isNonKnock = isNonKnock;
    }

    @Override
    public void applyEffect(Entity entity) {
        if(target.isInvulnerable()) return;

        target.setNoDamageTicks(1);
        target.damage(damage, damageSource);
        if(isNonKnock) target.setVelocity(new Vector(0, 0, 0));
        target.setNoDamageTicks(10);
    }

    @EventHandler
    public void quitRemove(PlayerQuitEvent event){
        Player player = event.getPlayer();
        player.setNoDamageTicks(10);
    }

    @Override
    public void removeEffect(Entity entity) {
    }
}
