package org.core.coreSystem.cores.KEY.PLAYER.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.PLAYER;

import java.time.Duration;

public class Q implements SkillBase {

    private final PLAYER config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_TARGET = new Particle.DustOptions(Color.fromRGB(30, 30, 30), 0.6f);
    private static final PotionEffect GLOW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 60, 2, false, false);

    public Q(PLAYER config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        player.swingOffHand();
        World world = player.getWorld();

        LivingEntity entity = getTargetedEntity(player, 12, 0.5);

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);

        if (entity != null) {
            addTargetEffect(player, world, entity);
        }else{
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("not designated").color(NamedTextColor.DARK_GRAY),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "Q", cools);
        }
    }

    private void addTargetEffect(Player player, World world, LivingEntity entity) {
        qSkill_Particle_Effect(player, entity, 60);

        entity.addPotionEffect(GLOW_EFFECT);

        config.q_Skill_effect.put(player.getUniqueId(), entity);
        world.playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.5f, 1.0f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 3 || player.isDead()) {
                    cancel();
                    return;
                }
                Location loc = entity.getLocation();
                world.spawnParticle(Particle.ENCHANT, loc.add(0, 1.2, 0), 5, 0.5, 0.5, 0.5, 0.2);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void qSkill_Particle_Effect(Player player, Entity entity, int time) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > time || entity.isDead()) {
                    config.q_Skill_effect.remove(player.getUniqueId(), entity);
                    cancel();
                    return;
                }

                Location baseLoc = entity.getLocation();
                world.spawnParticle(Particle.DUST, baseLoc.clone().add(0, 3.3, 0), 3, 0.1, 0.1, 0.1, 0.06, DUST_TARGET);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLocation, direction, range, raySize,
                e -> e instanceof LivingEntity && !e.equals(player));

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            return target;
        }
        return null;
    }
}