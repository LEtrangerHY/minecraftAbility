package org.core.coreSystem.cores.VOL1.Dagger.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.bloodStroke;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class R implements SkillBase {
    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bloodStroke bloodStroker;

    public R(Dagger config, JavaPlugin plugin, Cool cool, bloodStroke bloodStroker) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.bloodStroker = bloodStroker;
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();

        player.swingMainHand();

        LivingEntity target = getTargetedEntity(player,4.0, 0.4);

        if(target != null){

            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
            double damage = config.r_Skill_damage * (1 + amp);

            Stun stun = new Stun(target, config.r_Stun);
            stun.applyEffect(player);

            config.r_damaged.put(player.getUniqueId(), true);
            bloodStroker.damageStroke(player, target);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
            forceDamage.applyEffect(player);
            config.r_damaged.remove(player.getUniqueId());

            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
            world.spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 24, 0.4, 0.4, 0.4, 0, dustOptions);
            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.5, 0), 8, 0.2, 0.2, 0.2, 0);

        }else {

            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

            Vector direction = player.getEyeLocation().getDirection().normalize();
            Location particleLocation = player.getEyeLocation().clone()
                    .add(direction.clone().multiply(2.0));

            world.spawnParticle(Particle.CRIT, particleLocation, 8, 0.08, 0.08, 0.08, 0);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("not designated").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
            );
            player.showTitle(title);

            long cools = 250L;
            cool.updateCooldown(player, "R", cools);

        }

    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isInvulnerable()) continue;

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