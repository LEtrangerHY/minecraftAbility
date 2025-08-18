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
import org.core.coreProgram.Abs.SkillBase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

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

        Entity entity = getTargetedEntity(player, 12, 0.3);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(30, 30, 30), 1.0f);
        world.spawnParticle(Particle.DUST, player.getLocation().add(0, 0.6, 0), 220, 3, 0, 3, 0, dustOptions);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);

        if(entity != null){

            new BukkitRunnable() {
                private double ticks = 0;

                @Override
                public void run() {

                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3,
                            Material.CHAIN.createBlockData());

                    if(ticks > 5){
                        cancel();
                        return;
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 1);

            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 30, 0.6, 0, 0.6, 1);

            world.spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1.1, 0), 12, 0.3, 0.3, 0.3, 0);
            world.spawnParticle(Particle.DRAGON_BREATH, entity.getLocation().add(0, 1.1, 0), 12, 0.3, 0.3, 0.3, 0);
            chain_qSkill_Particle_Effect(player, entity, 40);

            Grounding grounding = new Grounding(entity, 2000);
            grounding.applyEffect(entity);

            config.q_Skill_effect_1.put(player.getUniqueId(), entity);
            for(int i = 0; i < 3; i++) {
                if(config.atkCount.getOrDefault(player.getUniqueId(), 0) < 3) {
                    config.atkCount.put(player.getUniqueId(), config.atkCount.getOrDefault(player.getUniqueId(), 0) + 1);
                }
            }

            if(config.atkCount.getOrDefault(player.getUniqueId(), 0) == 3) {
                player.sendActionBar(Component.text("Skill Enable").color(NamedTextColor.DARK_GRAY));
            }else{
                player.sendActionBar(Component.text("Attack Count : " + config.atkCount.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));
            }

            entity.setVelocity(new Vector(0, 0, 0));

        }else{
            for (Entity rangeTarget : world.getNearbyEntities(player.getLocation(), 6.0, 6.0, 6.0)) {
                if (rangeTarget instanceof LivingEntity target && rangeTarget != player) {

                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3,
                            Material.CHAIN.createBlockData());

                    world.spawnParticle(Particle.SOUL, rangeTarget.getLocation().add(0, 1.1, 0), 6, 0.3, 0.3, 0.3, 0);
                    world.spawnParticle(Particle.DRAGON_BREATH, rangeTarget.getLocation().add(0, 1.1, 0), 6, 0.3, 0.3, 0.3, 0);

                    chain_qSkill_Particle_Effect(player, rangeTarget, 40);

                    Grounding grounding = new Grounding(rangeTarget, 2000);
                    grounding.applyEffect(rangeTarget);

                    config.q_Skill_effect_2.put(player.getUniqueId(), rangeTarget);
                    target.setVelocity(new Vector(0, 0, 0));

                    if(config.atkCount.getOrDefault(player.getUniqueId(), 0) < 3) {
                        config.atkCount.put(player.getUniqueId(), config.atkCount.getOrDefault(player.getUniqueId(), 0) + 1);
                    }

                }
            }

            if(config.atkCount.getOrDefault(player.getUniqueId(), 0) == 3) {
                player.sendActionBar(Component.text("Skill Enable").color(NamedTextColor.DARK_GRAY));
            }else{
                player.sendActionBar(Component.text("Attack Count : " + config.atkCount.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));
            }
        }
    }

    public void chain_qSkill_Particle_Effect(Player player, Entity entity, int time){

        World world = player.getWorld();

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

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);

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
