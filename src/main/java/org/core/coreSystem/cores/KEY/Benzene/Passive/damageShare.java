package org.core.coreSystem.cores.KEY.Benzene.Passive;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
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

    private final NamespacedKey lockKey;

    public damageShare(Benzene config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.lockKey = new NamespacedKey(plugin, "benzene_chain_lock");
    }

    public void damageShareTrigger(Player player, Entity target, double damage) {
        dShare(player, target, damage);
    }

    private void dShare(Player player, Entity target, double damage) {

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        List<Entity> lockedEntities = new ArrayList<>();
        String sessionID = player.getUniqueId().toString();

        try {
            LinkedHashMap<Integer, UUID> chainMap = config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>());

            Set<UUID> uniqueTargets = new LinkedHashSet<>(chainMap.values());
            uniqueTargets.remove(target.getUniqueId());

            for (UUID chainedUUID : uniqueTargets) {
                Entity chainedEntity = Bukkit.getEntity(chainedUUID);

                if (chainedEntity == null || !chainedEntity.isValid() || !(chainedEntity instanceof LivingEntity)) continue;

                Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
                Location loc2 = chainedEntity.getLocation().add(0, chainedEntity.getHeight() / 2 + 0.2, 0);
                double distance = loc1.distance(loc2);

                if (distance <= 22) {
                    chainedEntity.getPersistentDataContainer().set(lockKey, PersistentDataType.STRING, sessionID);
                    lockedEntities.add(chainedEntity);

                    World world = chainedEntity.getWorld();
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);

                    double shareDamage = damage * chainMap.size() / 10.0;

                    if (config.q_Skill_effect_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(target.getUniqueId())) {
                        shareDamage *= (5.0 / 3.0);
                    }

                    ForceDamage forceDamage = new ForceDamage((LivingEntity) chainedEntity, shareDamage, source, false);
                    forceDamage.applyEffect(player);

                    Location effectLoc = chainedEntity.getLocation().add(0, 1.2, 0);
                    world.spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 1, 0.1, 0.1, 0.1, 1);
                    world.spawnParticle(Particle.ENCHANTED_HIT, effectLoc, 10, 0.4, 0, 0.4, 1);
                    world.playSound(effectLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                    world.playSound(effectLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
                }
            }
        } finally {
            for (Entity lockedEntity : lockedEntities) {
                if (lockedEntity.isValid()) {
                    lockedEntity.getPersistentDataContainer().remove(lockKey);
                }
            }
        }
    }
}