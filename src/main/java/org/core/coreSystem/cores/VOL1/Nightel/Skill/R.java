package org.core.coreSystem.cores.VOL1.Nightel.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Nightel.Passive.Chain;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.Nightel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class R implements SkillBase {

    private final Nightel config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Chain chain;
    private final NamespacedKey keyR;

    private static final Particle.DustOptions DUST_OPT = new Particle.DustOptions(Color.WHITE, 0.5f);

    public R(Nightel config, JavaPlugin plugin, Cool cool, Chain chain) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chain = chain;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        String lastSkill = config.chainSkill.getOrDefault(uuid, "");
        boolean diff = config.chainSkill.containsKey(uuid) && !lastSkill.equals("R");

        chain.chainCount(player, config.r_Skill_Cool, "R");

        player.swingMainHand();

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player, diff);
    }

    public void detect(Player player, boolean diff) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        config.rskill_using.put(uuid, true);

        Set<Entity> damagedSet = new HashSet<>();
        config.damaged.put(uuid, damagedSet);

        double baseDamage = diff ? config.r_Skill_damage * 3 : config.r_Skill_damage;
        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double finalDamage = baseDamage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(uuid);
                    config.damaged.remove(uuid);
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DUST, pLoc, 120, 0.3, 0, 0.3, 0.08, DUST_OPT);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (!damagedSet.contains(target)) {
                            if (diff) {
                                Location tLoc = target.getLocation();
                                world.playSound(tLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                                world.spawnParticle(Particle.ENCHANTED_HIT, tLoc.add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);
                            }

                            ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, true);
                            forceDamage.applyEffect(player);

                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}