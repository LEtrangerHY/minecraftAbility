package org.core.coreProgram.Cores.Benzene.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Grounding;
import org.core.coreProgram.Cores.Benzene.coreSystem.Benzene;
import org.core.coreProgram.AbsCoreSystem.SkillBase;

import java.util.*;

public class Q implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Benzene config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        player.swingOffHand();

        World world = player.getWorld();

        LivingEntity entity = getTargetedEntity(player, 12, 0.3);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(30, 30, 30), 0.6f);

        world.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3, 1);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);

        if(entity != null){

            int atk = Math.min(6 + config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).size() * 2, 12);

            chain_qSkill_Particle_Effect(player, entity, 40);

            Grounding grounding = new Grounding(entity, 2000);
            grounding.applyEffect(entity);

            config.q_Skill_effect_1.put(player.getUniqueId(), entity);

            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick >= atk || player.isDead()) {
                        this.cancel();
                        return;
                    }

                    world.playSound(entity.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3,
                            Material.CHAIN.createBlockData());

                    if(tick % 2 == 0) {
                        ForceDamage forceDamage = new ForceDamage(entity, config.q_Skill_damage / 2);
                        forceDamage.applyEffect(player);
                        entity.setVelocity(new Vector(0, 0, 0));

                        world.playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
                        world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1.2, 0), 12, 0.6, 0.6, 0.6, 0, dustOptions);
                    }

                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            entity.setVelocity(new Vector(0, 0, 0));

        }else{
            world.spawnParticle(Particle.DUST, player.getLocation().add(0, 0.6, 0), 222, 3, 0, 3, 0, dustOptions);

            for (Entity rangeTarget : world.getNearbyEntities(player.getLocation(), 6.0, 6.0, 6.0)) {
                if (rangeTarget instanceof LivingEntity target && rangeTarget != player) {

                    world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
                    world.playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3,
                            Material.CHAIN.createBlockData());
                    chain_qSkill_Particle_Effect(player, rangeTarget, 40);

                    Grounding grounding = new Grounding(rangeTarget, 2000);
                    grounding.applyEffect(rangeTarget);

                    config.q_Skill_effect_2.put(player.getUniqueId(), rangeTarget);
                    ForceDamage forceDamage = new ForceDamage(target, config.q_Skill_damage);
                    forceDamage.applyEffect(player);
                    target.setVelocity(new Vector(0, 0, 0));

                }
            }
        }
    }

    public void chain_qSkill_Particle_Effect(Player player, Entity entity, int time){

        World world = player.getWorld();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > time || entity.isDead()) {
                    if(config.q_Skill_effect_1.containsKey(player.getUniqueId())){config.q_Skill_effect_1.remove(player.getUniqueId(), entity);}
                    if(config.q_Skill_effect_2.containsKey(player.getUniqueId())){config.q_Skill_effect_2.remove(player.getUniqueId(), entity);}
                    this.cancel();
                    return;
                }

                for(int i = 0; i < 33; i++){
                    world.spawnParticle(Particle.DUST, entity.getLocation().add(0, ((double) i) / 10, 0), 1, 0, 0, 0, 0, dustOptions);
                    if(i % 3 == 0){
                        world.spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, (3.3 - (((double) i * 1.2) / 10)), 0), 1, 0, 0, 0, 0);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;

            RayTraceResult result = world.rayTraceEntities(
                    eyeLocation, direction, range, raySize, e -> e.equals(entity)
            );

            if (result != null) {
                candidates.add((LivingEntity) entity);
            }
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(Damageable::getHealth))
                .orElse(null);
    }
}
