package org.core.Effect;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ForceDamage implements Effects, Listener {
    private final LivingEntity target;
    private final double damage;
    private final DamageSource damageSource;

    public ForceDamage(LivingEntity target, double damage, DamageSource damageSource) {
        this.target = target;
        this.damage = damage;
        this.damageSource = damageSource;
    }

    @Override
    public void applyEffect(Entity entity) {
        if(target.isInvulnerable()) return;

        target.setNoDamageTicks(1);
        target.damage(damage, damageSource);
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
