package org.core.coreSystem.cores.VOL6.Jester.Skill.JackInTheBox;

import org.core.models.coreObj.jestShieldModel;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Jester.coreSystem.Jester;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class Q_shield implements SkillBase {

    private final Jester config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Consumer<Player> onSkipStage;
    private final NamespacedKey keyQ;

    public Q_shield(Jester config, JavaPlugin plugin, Cool cool, Consumer<Player> onSkipStage) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.onSkipStage = onSkipStage;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        long parryEndTime = config.shieldParryTime.getOrDefault(uuid, 0L);

        if (System.currentTimeMillis() > parryEndTime) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        cool.updateCooldown(player, "Shield - Parry", 0L);
        config.shieldParryTime.remove(uuid);

        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double ampMultiplier = 1.0 + (config.q_Skill_amp * level);
        double damage = config.q_Skill_damage * ampMultiplier;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1);

        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        double shieldScale = 1.8;

        ItemDisplay shieldDisplay = new jestShieldModel(startLoc.getWorld(), startLoc, shieldScale).getDisplay();

        new BukkitRunnable() {
            Location loc = player.getEyeLocation().subtract(0, 0.2, 0);
            Vector dir = loc.getDirection().normalize();
            int bounces = 0;
            int ticks = 0;
            float spin = 0.0f;
            final Set<UUID> hitRecently = new HashSet<>();

            @Override
            public void run() {
                if (ticks > 66 || bounces >= 6 || !player.isOnline()) {
                    shieldDisplay.remove();
                    this.cancel();
                    return;
                }

                double speed = 1.6;
                RayTraceResult ray = loc.getWorld().rayTraceBlocks(loc, dir, speed, FluidCollisionMode.NEVER, true);

                if (ray != null && ray.getHitBlock() != null) {
                    Vector normal = ray.getHitBlockFace().getDirection();
                    double dot = dir.dot(normal);
                    dir.subtract(normal.multiply(2 * dot)).normalize();
                    bounces++;

                    loc = ray.getHitPosition().toLocation(loc.getWorld()).add(dir.clone().multiply(0.2));
                    loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 1.2f);
                    loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 10, 0.1, 0.1, 0.1, 0.1);

                    hitRecently.clear();
                } else {
                    loc.add(dir.clone().multiply(speed));
                }

                spin += 45.0f;
                if (spin >= 360.0f) spin -= 360.0f;

                Location renderLoc = loc.clone();
                renderLoc.setYaw(spin);
                renderLoc.setPitch(0f);
                shieldDisplay.teleport(renderLoc);

                loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.1, 0.1, 0.1, 0.05);
                loc.getWorld().spawnParticle(Particle.BLOCK, loc, 2, 0.1, 0.1, 0.1, 0.05, Bukkit.createBlockData(Material.IRON_BLOCK));

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.8, 1.8, 1.8)) {
                    if (e instanceof LivingEntity target && target != player && !hitRecently.contains(target.getUniqueId())) {
                        ForceDamage fd = new ForceDamage(target, damage, source, true);
                        fd.applyEffect(player);
                        hitRecently.add(target.getUniqueId());
                        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        onSkipStage.accept(player);
    }
}