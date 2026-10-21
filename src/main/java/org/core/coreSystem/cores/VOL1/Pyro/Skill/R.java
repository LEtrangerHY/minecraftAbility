package org.core.coreSystem.cores.VOL1.Pyro.Skill;

import org.core.effect.effectPlugin.swingEchoGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.BLAZE_POWDER && offhandItem.getAmount() >= 4) {

            offhandItem.setAmount(offhandItem.getAmount() - 4);
            cool.setCooldown(player, config.r_Skill_Cool, "R");

            swingEchoGuard.swingMainHand(player);

            Location startLocation = player.getLocation();
            Vector direction = startLocation.getDirection().normalize().multiply(1.9);
            player.setVelocity(direction);

            player.getWorld().playSound(startLocation, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
            player.getWorld().playSound(startLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

            Invulnerable invulnerable = new Invulnerable(player, 900);
            invulnerable.applyEffect(player);

            detectDashHit(player);

        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("blaze powder needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            cool.updateCooldown(player, "R", 500L);
        }
    }

    private void detectDashHit(Player player) {
        World world = player.getWorld();
        Set<Entity> damagedSet = new HashSet<>();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 14 || player.isDead()) {
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                world.spawnParticle(Particle.FLAME, pLoc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                world.spawnParticle(Particle.SMOKE, pLoc.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);

                for (int x = -3; x <= 3; x++) {
                    for (int y = -1; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            Block block = pLoc.clone().add(x, y, z).getBlock();

                            if (block.isBurnable() || block.getType() == Material.ICE || block.getType() == Material.SNOW ||
                                    block.getType() == Material.BLUE_ICE || block.getType() == Material.FROSTED_ICE ||
                                    block.getType() == Material.PACKED_ICE || block.getType() == Material.POWDER_SNOW ||
                                    block.getType() == Material.SNOW_BLOCK) {

                                boolean shouldBurn = true;
                                if (block.getType() == Material.BLUE_ICE) {
                                    if (Math.random() > 0.7) {
                                        shouldBurn = false;
                                    }
                                }

                                if (shouldBurn) {
                                    Location bLoc = block.getLocation().add(0.5, 0.5, 0.5);
                                    Block below = block.getRelative(BlockFace.DOWN);

                                    block.setType(Material.AIR);
                                    world.spawnParticle(Particle.FLAME, bLoc, 2, 0.3, 0.3, 0.3, 0.1);
                                    world.spawnParticle(Particle.LARGE_SMOKE, bLoc, 1, 0.3, 0.3, 0.3, 0.05);

                                    if (Math.random() < 0.2 && below.getType().isSolid()) {
                                        block.setType(Material.FIRE);
                                    }
                                }
                            }
                        }
                    }
                }

                for (Entity entity : player.getNearbyEntities(1.4, 1.4, 1.4)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        if (!damagedSet.contains(target)) {
                            damagedSet.add(target);

                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 3, false, false, false));
                            world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.0f);

                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 0.2, 0), 15, 0.3, 0.1, 0.3, 0.05);

                            Location markLocation = target.getLocation().clone();
                            scheduleFirePillar(player, markLocation);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void scheduleFirePillar(Player player, Location markLocation) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = markLocation.getWorld();
                if (world == null) return;

                world.playSound(markLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);
                world.playSound(markLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                for (double y = 0; y <= 4; y += 0.5) {
                    Location pLoc = markLocation.clone().add(0, y, 0);
                    world.spawnParticle(Particle.FLAME, pLoc, 20, 0.4, 0.2, 0.4, 0.1);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 10, 0.4, 0.2, 0.4, 0.1);
                    world.spawnParticle(Particle.LAVA, pLoc, 3, 0.3, 0.2, 0.3, 0);
                }

                double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
                double damage = config.r_Skill_Damage * (1 + amp);

                DamageSource source = DamageSource.builder(DamageType.MAGIC)
                        .withCausingEntity(player)
                        .withDirectEntity(player)
                        .build();

                for (Entity entity : world.getNearbyEntities(markLocation, 1.5, 4.0, 1.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(player);

                        Stun stun = new Stun(target, config.r_Skill_stun);
                        stun.applyEffect(player);
                    }
                }
            }
        }.runTaskLater(plugin, 12L);
    }
}