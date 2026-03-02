package org.core.coreSystem.cores.VOL3.Luster.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Luster.coreSystem.Luster;

import java.time.Duration;

public class R implements SkillBase {
    private final Luster config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Luster config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.LODESTONE || (offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() > 4)) {

            World world = player.getWorld();

            Vector dir = player.getEyeLocation().getDirection().normalize();
            Vector spawnOffset = dir.clone().multiply(0.8).add(new Vector(0, -0.4, 0));
            Location spawnLoc = player.getEyeLocation().add(spawnOffset);

            BlockDisplay fb = world.spawn(spawnLoc, BlockDisplay.class, entity -> {
                entity.setBlock(Material.IRON_BLOCK.createBlockData());
                entity.setTeleportDuration(1);
                Transformation transform = entity.getTransformation();
                transform.getTranslation().set(-0.5f, -0.5f, -0.5f);
                entity.setTransformation(transform);
            });

            double speed = 2.2;
            Vector velocity = dir.clone().multiply(speed);

            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
            double damage = config.r_Skill_Damage * (1 + amp);
            double splashDamage = damage * 0.4;

            DamageSource source = DamageSource.builder(DamageType.MAGIC)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(126, 126, 126), 1.3f);
            Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);
            BlockData iron = Material.IRON_BLOCK.createBlockData();

            world.spawnParticle(Particle.END_ROD, fb.getLocation(), 44, 0.8, 0.8, 0.8, 0.4);

            world.playSound(fb.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);
            world.spawnParticle(Particle.ENCHANTED_HIT, spawnLoc, 30, 0.2, 0.2, 0.2, 1);

            Vector backward = player.getLocation().getDirection().multiply(-0.7);
            player.setVelocity(player.getVelocity().add(backward));

            new BukkitRunnable() {
                int life = 100;
                Location currentLoc = spawnLoc.clone();

                @Override
                public void run() {

                    if (!fb.isValid()) {
                        cancel();
                        return;
                    }

                    if (life-- <= 0) {
                        fb.remove();
                        cancel();
                        return;
                    }

                    world.spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 3, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.DUST, currentLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                    world.spawnParticle(Particle.DUST, currentLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions_gra);

                    for (Entity e : world.getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {

                            world.playSound(currentLoc, Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                            world.playSound(currentLoc, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

                            ForceDamage forceDamage = new ForceDamage(le, damage, source, false);
                            forceDamage.applyEffect(player);

                            Vector knock = le.getLocation().toVector().subtract(player.getLocation().toVector())
                                    .normalize().multiply(1.7);
                            le.setVelocity(le.getVelocity().add(knock));

                            world.spawnParticle(Particle.BLOCK, currentLoc, 44, 0.3, 0.3, 0.3, iron);

                            fb.remove();
                            cancel();
                            return;
                        }
                    }

                    RayTraceResult hitBlockResult = world.rayTraceBlocks(currentLoc, dir, speed, FluidCollisionMode.NEVER, true);

                    if (hitBlockResult != null && hitBlockResult.getHitBlock() != null) {
                        Location hitLoc = hitBlockResult.getHitPosition().toLocation(world);

                        world.spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1, 0.0, 0.0, 0.0, 0.0);
                        world.spawnParticle(Particle.LARGE_SMOKE, hitLoc, 10, 0.8, 0.8, 0.8, 0.1);
                        world.spawnParticle(Particle.BLOCK, hitLoc, 44, 0.3, 0.3, 0.3, iron);

                        world.playSound(hitLoc, Sound.BLOCK_ANVIL_LAND, 0.9f, 0.7f);
                        world.playSound(hitLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.8f);
                        world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

                        for (Entity e : world.getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                            if (e instanceof LivingEntity le && !le.equals(player)) {
                                if (le.getLocation().distance(hitLoc) <= 4.0) {
                                    ForceDamage forceDamage = new ForceDamage(le, splashDamage, source, false);
                                    forceDamage.applyEffect(player);

                                    Vector knock = le.getLocation().toVector().subtract(hitLoc.toVector());
                                    if (knock.lengthSquared() > 0.001) {
                                        knock = knock.normalize().multiply(1.2);
                                    } else {
                                        knock = new Vector(0, 0.5, 0);
                                    }
                                    le.setVelocity(le.getVelocity().add(knock));
                                }
                            }
                        }

                        fb.remove();
                        cancel();
                        return;
                    }

                    currentLoc.add(velocity);
                    fb.teleport(currentLoc);
                }
            }.runTaskTimer(plugin, 0L, 1L);

            if(offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() >= 4) {
                offhandItem.setAmount(offhandItem.getAmount() - 4);
            }
        }else{
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("iron ingot needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "R", cools);
        }
    }
}