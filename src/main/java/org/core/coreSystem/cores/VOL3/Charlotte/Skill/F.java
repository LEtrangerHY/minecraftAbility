package org.core.coreSystem.cores.VOL3.Charlotte.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Charlotte.coreSystem.Charlotte;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class F implements SkillBase {

    private final Charlotte config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyF;

    private final Map<UUID, BukkitRunnable> activePrismDecayTasks = new HashMap<>();

    private static final BlockData GLASS = Material.GLASS.createBlockData();
    private static final BlockData BLOOD = Material.REDSTONE_BLOCK.createBlockData();
    private static final BlockData CHAIN = Material.IRON_CHAIN.createBlockData();

    public F(Charlotte config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        int prismCount = Math.max(1, config.f_prism.getOrDefault(uuid, 1));

        for (int i = 0; i < prismCount; i++) {
            long delay = Math.round(i * (20.0 / prismCount));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isDead() || !player.isOnline() || !player.isConnected()) {
                    return;
                }

                Location currentLoc = player.getLocation().add(0, 1.5, 0);

                final Vector fixedVelocity = currentLoc.getDirection().normalize().multiply(2.3);

                world.playSound(currentLoc, Sound.ITEM_TRIDENT_THROW, 1, 1);

                Item item = world.dropItem(currentLoc, new ItemStack(Material.PRISMARINE_CRYSTALS));
                item.setVelocity(fixedVelocity);
                item.setPickupDelay(1000);
                item.setGravity(false);

                DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                        .withCausingEntity(player)
                        .withDirectEntity(item)
                        .withDamageLocation(player.getLocation())
                        .build();

                new BukkitRunnable() {
                    int life = 80;

                    @Override
                    public void run() {
                        if (item.isDead() || !item.isValid()) {
                            this.cancel();
                            return;
                        }

                        item.setVelocity(fixedVelocity);

                        Location loc = item.getLocation();

                        for (Entity nearby : item.getNearbyEntities(1.2, 1.2, 1.2)) {
                            if (nearby instanceof LivingEntity target && nearby != player) {

                                ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                                forceDamage.applyEffect(player);

                                cool.updateCooldown(player, "Prism", 3000L, "boss");
                                startPrismDecayTask(player);

                                world.playSound(target.getLocation().clone(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3, BLOOD);
                                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3, GLASS);

                                item.remove();
                                this.cancel();
                                return;
                            }
                        }

                        Vector dir = fixedVelocity.clone().normalize();
                        double checkDistance = fixedVelocity.length() + 0.5;

                        RayTraceResult rayTrace = world.rayTraceBlocks(loc, dir, checkDistance, FluidCollisionMode.NEVER, true);
                        Block hitBlock = null;
                        Location hitPos = loc;

                        if (rayTrace != null && rayTrace.getHitBlock() != null) {
                            hitBlock = rayTrace.getHitBlock();
                            hitPos = rayTrace.getHitPosition().toLocation(world);
                        } else {
                            Block frontBlock = loc.clone().add(dir.clone().multiply(0.8)).getBlock();
                            if (!frontBlock.isPassable()) {
                                hitBlock = frontBlock;
                                hitPos = frontBlock.getLocation().add(0.5, 0.5, 0.5);
                            }
                        }

                        if (hitBlock != null && !hitBlock.isPassable()) {
                            if (hitBlock.getType().name().contains("GLASS")) {
                                world.spawnParticle(Particle.BLOCK, hitBlock.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, CHAIN);

                                int currentPrism = config.f_prism.getOrDefault(uuid, 1);
                                if (currentPrism < 6) {
                                    config.f_prism.put(uuid, currentPrism + 1);
                                }

                                long qCooldown = cool.getRemainCooldown(player, "Q");
                                cool.updateCooldown(player, "Q", Math.max(qCooldown - 1000L, 0L));

                                cool.updateCooldown(player, "Prism", 3000L, "boss");
                                startPrismDecayTask(player);
                            }

                            world.playSound(hitPos, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                            world.spawnParticle(Particle.BLOCK, hitPos, 12, 0.3, 0.3, 0.3, GLASS);

                            item.remove();
                            this.cancel();
                            return;
                        }

                        if (life-- <= 0) {
                            item.remove();
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);

            }, delay);
        }
    }

    private void startPrismDecayTask(Player player) {
        UUID uuid = player.getUniqueId();

        if (activePrismDecayTasks.containsKey(uuid)) {
            activePrismDecayTasks.get(uuid).cancel();
            activePrismDecayTasks.remove(uuid);
        }

        BukkitRunnable decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    config.f_prism.remove(uuid);
                    activePrismDecayTasks.remove(uuid);
                    this.cancel();
                    return;
                }

                long remain = cool.getRemainCooldown(player, "Prism");

                if (remain <= 0) {
                    int currentPrism = config.f_prism.getOrDefault(uuid, 0);

                    if (currentPrism > 0) {
                        World world = player.getWorld();
                        Location particleLoc = player.getLocation().add(0, 1.2, 0);

                        world.spawnParticle(Particle.BLOCK, particleLoc, 10 * currentPrism, 0.5, 0.5, 0.5, CHAIN);
                        world.playSound(particleLoc, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.8f);
                        world.playSound(particleLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
                    }

                    config.f_prism.remove(uuid);
                    activePrismDecayTasks.remove(uuid);
                    this.cancel();
                }
            }
        };

        activePrismDecayTasks.put(uuid, decayTask);
        decayTask.runTaskTimer(plugin, 0L, 1L);
    }
}