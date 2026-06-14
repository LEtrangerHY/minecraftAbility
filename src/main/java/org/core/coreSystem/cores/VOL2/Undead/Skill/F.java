package org.core.coreSystem.cores.VOL2.Undead.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class F implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final BlockData bloodData = Material.REDSTONE_BLOCK.createBlockData();
    private static final Particle.DustOptions CELL_RED = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.2f);

    private final Map<UUID, BukkitTask> activeDecayTasks = new HashMap<>();

    public F(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        if (player.isDead() || !player.isValid()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        UUID uuid = player.getUniqueId();
        double chargeRate = config.f_charge_progress.getOrDefault(uuid, 0.0);

        if (chargeRate <= 0.0) return;

        config.f_charge_consumed.put(uuid, true);

        boolean isBuffActive = config.f_buff_ticks.getOrDefault(uuid, 0) > 0;
        double currentBuff = config.f_buff_rate.getOrDefault(uuid, 0.0);
        double currentDebuff = config.f_debuff_rate.getOrDefault(uuid, 0.0);

        double addedDebuff = isBuffActive ? (chargeRate * 2.0) : chargeRate;
        double newDebuff = Math.min(4.0, currentDebuff + addedDebuff);
        config.f_debuff_rate.put(uuid, newDebuff);

        if (!isBuffActive || chargeRate > currentBuff) {
            config.f_buff_rate.put(uuid, chargeRate);

            int buffDuration = (int) (80 + (40 * chargeRate));
            config.f_buff_ticks.put(uuid, buffDuration);

            float newSpeed = (float) (0.2 * (1.0 + chargeRate));
            player.setWalkSpeed(newSpeed);
        }

        double chargeTime = chargeRate * 4.0;

        final int durationTicks;

        if (chargeTime >= 1.0) {
            double clampedTime = Math.min(chargeTime, 4.0);
            durationTicks = (int) (6 + (7 * ((clampedTime - 1.0) / 3.0)));
        } else {
            durationTicks = (int) Math.max(1, 6 * chargeTime);
        }

        final double maxHealth = maxHealthAttr.getValue();
        final double totalHealAmount = maxHealth * chargeRate;
        final double healPerTick = totalHealAmount / durationTicks;

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.7f);

        if (chargeTime >= 1.0) {
            long invulnDurationMs = durationTicks * 50L;
            Invulnerable invulnerable = new Invulnerable(player, invulnDurationMs);
            invulnerable.applyEffect(player);
        }

        new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (currentTick >= durationTicks || !player.isValid() || player.isDead()) {
                    this.cancel();
                    return;
                }

                player.setHealth(Math.min(maxHealth, player.getHealth() + healPerTick));

                Location centerLoc = player.getLocation().add(0, 1.0, 0);

                world.spawnParticle(Particle.BLOCK, centerLoc, 5, 0.4, 0.5, 0.4, 0, bloodData);
                world.spawnParticle(Particle.DUST, centerLoc, 3, 0.4, 0.5, 0.4, 0, CELL_RED);

                world.playSound(centerLoc, Sound.BLOCK_SLIME_BLOCK_STEP, 0.3f, 1.2f + (currentTick * 0.05f));

                currentTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        if (activeDecayTasks.containsKey(uuid)) {
            activeDecayTasks.get(uuid).cancel();
        }

        BukkitTask decayTask = new BukkitRunnable() {
            int debuffTickCounter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || (!config.f_buff_ticks.containsKey(uuid) && !config.f_debuff_rate.containsKey(uuid))) {
                    activeDecayTasks.remove(uuid);
                    this.cancel();
                    return;
                }

                int currentBuffTicks = config.f_buff_ticks.getOrDefault(uuid, 0);
                if (currentBuffTicks > 0) {
                    currentBuffTicks--;
                    if (currentBuffTicks <= 0) {
                        config.f_buff_ticks.remove(uuid);
                        config.f_buff_rate.remove(uuid);
                        if (player.isOnline()) {
                            player.setWalkSpeed(0.2f);
                        }
                    } else {
                        config.f_buff_ticks.put(uuid, currentBuffTicks);
                    }
                }

                debuffTickCounter++;
                if (debuffTickCounter >= 4) {
                    debuffTickCounter = 0;
                    double currentDebuffRate = config.f_debuff_rate.getOrDefault(uuid, 0.0);
                    if (currentDebuffRate > 0) {
                        double nextRate = Math.max(0.0, currentDebuffRate - 0.01);
                        if (nextRate <= 0.0) {
                            config.f_debuff_rate.remove(uuid);
                        } else {
                            config.f_debuff_rate.put(uuid, nextRate);
                        }
                    }
                }

                if (currentBuffTicks <= 0 && config.f_debuff_rate.getOrDefault(uuid, 0.0) <= 0.0) {
                    activeDecayTasks.remove(uuid);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeDecayTasks.put(uuid, decayTask);
    }
}