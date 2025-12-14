package org.core.coreProgram.Cores.VOL1.Benzene.Passive;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.EffectManager;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.Cores.VOL1.Benzene.coreSystem.Benzene;

import java.util.*;

public class DamageShare {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public DamageShare(Benzene config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void damageShareTrigger(Player player, Entity target, double damage) {

        long times = Bukkit.getServer().getCurrentTick();

        if(!config.damageTimes.getOrDefault(target, new LinkedHashMap<>()).containsValue(times)) {
            damageShare(player, target, damage, times);
        }

    }

    private void damageShare(Player player, Entity target, double damage, long times) {

        Set<Entity> processedEntities = new HashSet<>();

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity chainedEntity : new ArrayList<>(config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values())) {
            if (!(chainedEntity instanceof LivingEntity) || chainedEntity == target) continue;

            if (processedEntities.contains(chainedEntity)) continue;
            processedEntities.add(chainedEntity);

            Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
            Location loc2 = chainedEntity.getLocation().add(0, chainedEntity.getHeight() / 2 + 0.2, 0);
            double distance = loc1.distance(loc2);

            if (distance <= 22 && !config.damageTimes.getOrDefault(target, new LinkedHashMap<>()).containsValue(times)) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                chainedEntity.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);

                config.damageTimes.putIfAbsent(chainedEntity, new LinkedHashMap<>());
                config.damageTimes.get(chainedEntity).put(target, times);

                double shareDamage = damage * (config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).size()) / 10;
                if (config.q_Skill_effect_2.containsValue(target)) {
                    shareDamage *= (5.0 / 3);
                }

                ForceDamage forceDamage = new ForceDamage((LivingEntity) chainedEntity, shareDamage, source);
                forceDamage.applyEffect(player);
                chainedEntity.setVelocity(new Vector(0, 0, 0));

                Location effectLoc = chainedEntity.getLocation().add(0, 1.2, 0);
                chainedEntity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 1, 0.1, 0.1, 0.1, 1);
                chainedEntity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, effectLoc, 10, 0.4, 0, 0.4, 1);
                chainedEntity.getWorld().playSound(effectLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                chainedEntity.getWorld().playSound(effectLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);

            }

            Map<Entity, Long> timesMap = config.damageTimes.getOrDefault(chainedEntity, new LinkedHashMap<>());
            timesMap.remove(target, times);
            if (timesMap.isEmpty()) {
                config.damageTimes.remove(chainedEntity);
            }
        }
    }
}