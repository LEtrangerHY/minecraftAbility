package org.core.coreSystem.cores.VOL1.Commander.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.Commander;

import java.time.Duration;
import java.util.HashSet;

public class Q implements SkillBase {

    private final Commander config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Commander config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        if(!config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>()).isEmpty()) {

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 2.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);

            for (FallingBlock fb : config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>())) {
                Location center = fb.getLocation().clone().add(0, 0.5, 0);

                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 30, 0.5, 0.5, 0.5, 0.2);

                circleParticle(player, center);
                commandReceiver(player, fb);
            }
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 0.8f);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("com-block uninstalled").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "Q", cools);
        }
    }

    public void circleParticle(Player player, Location center){
        double maxRadius = 4.5;
        int maxTicks = 6;

        Particle.DustOptions waveDust = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.2f); // 시안(Cyan) 색상 파동

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > maxTicks) {
                    this.cancel();
                    return;
                }

                double currentRadius = (maxRadius / maxTicks) * ticks;

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    Location particleLocation = center.clone().add(x, 0, z);

                    player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, waveDust);

                    if (Math.random() < 0.1) {
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLocation, 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void commandReceiver(Player player, FallingBlock fb) {
        World world = player.getWorld();
        Location center = fb.getLocation();

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);
        double heal = config.q_Skill_Heal * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(center, 4.5, 4.5, 4.5)) {
            if (!(entity instanceof LivingEntity)) continue;

            if(!entity.equals(player)) {

                ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage, source, false);
                forceDamage.applyEffect(player);

                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 4, 1, false, false);
                PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false);
                ((LivingEntity) entity).addPotionEffect(slowness);
                ((LivingEntity) entity).addPotionEffect(blindness);

                fb.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.5f);
                fb.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 2.0f);

                Location start = fb.getLocation().clone().add(0, 0.5, 0);
                Vector dir = entity.getLocation().clone().add(0, 1.2, 0).toVector().subtract(start.toVector()).normalize();
                double maxDistance = start.distance(entity.getLocation().clone().add(0, 1, 0));

                attackLine(player, maxDistance, start, dir);
            } else {
                ((LivingEntity) entity).heal(heal);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 1.2, 0), 10, 0.4, 0.4, 0.4, 0.05);
            }
        }
    }

    public void attackLine(Player player, double maxDistance, Location start, Vector direction){
        double step = 0.2;

        Particle.DustOptions coreDust = new Particle.DustOptions(Color.fromRGB(200, 255, 255), 0.6f);
        Particle.DustOptions outerDust = new Particle.DustOptions(Color.fromRGB(0, 150, 255), 0.8f);

        for (double i = 0; i <= maxDistance; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));

            player.getWorld().spawnParticle(Particle.DUST, point, 1, 0.02, 0.02, 0.02, 0, coreDust);
            player.getWorld().spawnParticle(Particle.DUST, point, 1, 0.05, 0.05, 0.05, 0, outerDust);

            if (Math.random() < 0.15) {
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}