package org.core.coreSystem.cores.KEY.Benzene.Passive;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.effect.crowdControl.EffectManager;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.Benzene;

import java.util.*;

public class damageShare {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public damageShare(Benzene config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void damageShareTrigger(Player player, Entity target, double damage) {

        long times = Bukkit.getServer().getCurrentTick();

        if(!config.damageTimes.getOrDefault(target, new LinkedHashMap<>()).containsValue(times)) {
            dShare(player, target, damage, times);
        }

    }

    private void dShare(Player player, Entity target, double damage, long times) {

        Set<Entity> processedEntities = new HashSet<>();

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity chainedEntity : new ArrayList<>(config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values())) {
            if (!(chainedEntity instanceof LivingEntity) || chainedEntity == target) continue;

            if (processedEntities.contains(chainedEntity)) continue;
            processedEntities.add(chainedEntity);

            Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
            Location loc2 = chainedEntity.getLocation().add(0, chainedEntity.getHeight() / 2 + 0.2, 0);
            double distance = loc1.distance(loc2);

            if (distance <= 22 && !config.damageTimes.getOrDefault(target, new LinkedHashMap<>()).containsValue(times)) {

                World world = chainedEntity.getWorld();

                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);

                config.damageTimes.putIfAbsent(chainedEntity, new LinkedHashMap<>());
                config.damageTimes.get(chainedEntity).put(target, times);

                double shareDamage = damage * (config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).size()) / 10;
                if (config.q_Skill_effect_2.containsValue(target)) {
                    shareDamage *= (5.0 / 3);
                }

                ForceDamage forceDamage = new ForceDamage((LivingEntity) chainedEntity, shareDamage, source, false);
                forceDamage.applyEffect(player);

                Location effectLoc = chainedEntity.getLocation().add(0, 1.2, 0);
                world.spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 1, 0.1, 0.1, 0.1, 1);
                world.spawnParticle(Particle.ENCHANTED_HIT, effectLoc, 10, 0.4, 0, 0.4, 1);
                world.playSound(effectLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                world.playSound(effectLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
            }

            Map<Entity, Long> timesMap = config.damageTimes.getOrDefault(chainedEntity, new LinkedHashMap<>());
            timesMap.remove(target, times);
            if (timesMap.isEmpty()) {
                config.damageTimes.remove(chainedEntity);
            }
        }
    }
}