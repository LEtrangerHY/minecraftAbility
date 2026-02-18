package org.core.coreSystem.cores.VOL1.Pyro.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.debuff.Burn;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class R implements SkillBase {

    private final Pyro config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Pyro config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        player.swingMainHand();

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.BLAZE_POWDER && offhandItem.getAmount() > 4) {

            Entity entity = getTargetedEntity(player, 27, 0.3);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

            if (entity != null) {

                initiate(player, entity.getLocation());

                offhandItem.setAmount(offhandItem.getAmount() - 4);

            } else {
                player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                long cools = 1000L;
                cool.updateCooldown(player, "R", cools);
            }

        }else{
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("powder needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            long cools = 500L;
            cool.updateCooldown(player, "R", cools);
        }

    }

    public void initiate(Player player, Location initiateLoc){

        World world = player.getWorld();

        Location bottom = initiateLoc.add(0, 0.2, 0);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > 20) {
                    firePoll(player, initiateLoc);
                    this.cancel();
                    return;
                }

                world.spawnParticle(Particle.FLAME, bottom, 1, 0.4, 0.1, 0.4, 0);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, bottom, 1, 0.4, 0.1, 0.4, 0);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void firePoll(Player player, Location initiateLoc){

        World world = player.getWorld();

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        player.getWorld().playSound(initiateLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
        player.getWorld().playSound(initiateLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        player.spawnParticle(Particle.FLAME, initiateLoc, 21, 0.1, 0.1, 0.1, 0.8);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, initiateLoc, 14, 0.1, 0.1, 0.1, 0.8);

        for (Entity entity : world.getNearbyEntities(initiateLoc, 1, 6, 1)) {
            if (entity instanceof LivingEntity target && entity != player) {

                ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                forceDamage.applyEffect(player);

                Stun stun = new Stun(target, config.r_Skill_stun);
                stun.applyEffect(player);

            }
        }

        effect(player, initiateLoc);
    }

    public void effect(Player player, Location initiateLoc){
        World world = player.getWorld();
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 90) {
                    this.cancel();
                    return;
                }

                for(int i = 0; i < 100; i++){
                    Location particleLoc = initiateLoc.clone().add(0, i / 10.0, 0);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.07);
                }

                if(tick % 30 == 0) {
                    player.spawnParticle(Particle.FLAME, initiateLoc.clone().add(0, 1, 0), 21, 0.1, 0.1, 0.1, 0.8);
                    player.spawnParticle(Particle.SOUL_FIRE_FLAME, initiateLoc.clone().add(0, 1, 0), 14, 0.1, 0.1, 0.1, 0.8);

                    for (Entity entity : world.getNearbyEntities(initiateLoc, 4, 10, 4)) {
                        if (entity instanceof LivingEntity target && entity != player) {
                            Burn burn = new Burn(target, 2000L);
                            burn.applyEffect(player);
                        }
                    }
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
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
