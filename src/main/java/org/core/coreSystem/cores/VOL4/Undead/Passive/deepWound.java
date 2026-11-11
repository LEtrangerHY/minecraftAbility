package org.core.coreSystem.cores.VOL4.Undead.Passive;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class deepWound {

    private static final BlockData BLOOD = Material.REDSTONE_BLOCK.createBlockData();
    private static final Particle.DustOptions WOUND = new Particle.DustOptions(Color.fromRGB(120, 8, 12), 0.9f);

    private final Undead config;
    private final JavaPlugin plugin;

    private final Map<UUID, Map<UUID, Wound>> wounds = new HashMap<>();
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    private static final class Wound {
        final UUID owner;
        double remaining;
        int ticks;

        Wound(UUID owner, double remaining, int ticks) {
            this.owner = owner;
            this.remaining = remaining;
            this.ticks = ticks;
        }
    }

    public deepWound(Undead config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void mark(Player player, LivingEntity target) {
        Map<UUID, Wound> marks = wounds.computeIfAbsent(target.getUniqueId(), uuid -> new HashMap<>());
        marks.put(player.getUniqueId(), new Wound(player.getUniqueId(), config.melee_q_Skill_Damage, config.melee_q_Skill_Duration));

        World world = target.getWorld();
        Location center = target.getBoundingBox().getCenter().toLocation(world);

        world.spawnParticle(Particle.SWEEP_ATTACK, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.BLOCK, center, 18, 0.25, 0.35, 0.25, 0, BLOOD);
        world.spawnParticle(Particle.CRIT, center, 12, 0.3, 0.4, 0.3, 0.4);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        world.playSound(center, Sound.ITEM_TRIDENT_HIT, 1.1f, 0.8f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);

        slow(target);
        track(target);
    }

    public void pop(Player attacker, LivingEntity target) {
        Map<UUID, Wound> marks = wounds.remove(target.getUniqueId());
        if (marks == null || marks.isEmpty()) return;

        stop(target);
        unslow(target);

        World world = target.getWorld();
        Location center = target.getBoundingBox().getCenter().toLocation(world);

        world.spawnParticle(Particle.BLOCK, center, 40, 0.3, 0.45, 0.3, 0, BLOOD);
        world.spawnParticle(Particle.DUST, center, 20, 0.3, 0.45, 0.3, 0, WOUND);
        world.spawnParticle(Particle.CRIT, center, 20, 0.3, 0.4, 0.3, 0.6);
        world.playSound(center, Sound.ITEM_TRIDENT_HIT, 1.4f, 0.55f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.3f, 0.6f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.5f);

        for (Wound wound : marks.values()) {
            Player owner = plugin.getServer().getPlayer(wound.owner);
            if (owner == null || wound.remaining <= 0) continue;
            hurt(owner, target, wound.remaining * config.melee_q_Skill_Burst, false);
        }
    }

    public void clear(Player player) {
        List<UUID> empty = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Wound>> entry : wounds.entrySet()) {
            entry.getValue().remove(player.getUniqueId());
            if (entry.getValue().isEmpty()) empty.add(entry.getKey());
        }
        for (UUID target : empty) {
            wounds.remove(target);
            BukkitRunnable task = tasks.remove(target);
            if (task != null) task.cancel();
        }
    }

    private void track(LivingEntity target) {
        UUID id = target.getUniqueId();
        if (tasks.containsKey(id)) return;

        BukkitRunnable task = new BukkitRunnable() {
            int age = 0;

            @Override
            public void run() {
                Map<UUID, Wound> marks = wounds.get(id);

                if (marks == null || marks.isEmpty() || !target.isValid() || target.isDead()) {
                    wounds.remove(id);
                    tasks.remove(id);
                    cancel();
                    return;
                }

                age++;
                boolean tick = age % config.melee_q_Skill_Interval == 0;
                int steps = config.melee_q_Skill_Duration / config.melee_q_Skill_Interval;

                List<UUID> done = new ArrayList<>();
                for (Wound wound : marks.values()) {
                    wound.ticks--;

                    if (tick && wound.remaining > 0) {
                        double amount = Math.min(wound.remaining, config.melee_q_Skill_Damage / steps);
                        wound.remaining -= amount;

                        Player owner = plugin.getServer().getPlayer(wound.owner);
                        if (owner != null) hurt(owner, target, amount, true);
                    }

                    if (wound.ticks <= 0 || wound.remaining <= 1.0E-6) done.add(wound.owner);
                }
                for (UUID owner : done) marks.remove(owner);

                if (marks.isEmpty()) {
                    wounds.remove(id);
                    tasks.remove(id);
                    unslow(target);
                    cancel();
                    return;
                }

                slow(target);

                if (age % 4 == 0) {
                    Location center = target.getBoundingBox().getCenter().toLocation(target.getWorld());
                    target.getWorld().spawnParticle(Particle.DUST, center, 2, 0.2, 0.35, 0.2, 0, WOUND);
                    target.getWorld().spawnParticle(Particle.BLOCK, center.add(0, -0.2, 0), 1, 0.15, 0.1, 0.15, 0, BLOOD);
                }
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
        tasks.put(id, task);
    }

    private void stop(LivingEntity target) {
        BukkitRunnable task = tasks.remove(target.getUniqueId());
        if (task != null) task.cancel();
    }

    private void hurt(Player owner, LivingEntity target, double amount, boolean keepMotion) {
        if (target.isDead() || !target.isValid()) return;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(owner)
                .withDirectEntity(owner)
                .build();

        double buff = config.restore_buff_rate.getOrDefault(owner.getUniqueId(), 0.0);
        Vector motion = target.getVelocity();

        config.is_damaging.add(owner.getUniqueId());
        try {
            new ForceDamage(target, amount * (1.0 + buff), source, true).applyEffect(owner);
        } finally {
            config.is_damaging.remove(owner.getUniqueId());
        }

        if (keepMotion) target.setVelocity(motion);
    }

    private void slow(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 12, config.melee_q_Skill_Slow, false, false, true));
    }

    private void unslow(LivingEntity target) {
        PotionEffect effect = target.getPotionEffect(PotionEffectType.SLOWNESS);
        if (effect != null && effect.getAmplifier() == config.melee_q_Skill_Slow && effect.getDuration() <= 12) {
            target.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }
}