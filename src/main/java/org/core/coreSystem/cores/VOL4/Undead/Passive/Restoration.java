package org.core.coreSystem.cores.VOL4.Undead.Passive;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Restoration {

    private static final Particle.DustOptions CELL_RED = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.2f);
    private static final BlockData BLOOD = Material.REDSTONE_BLOCK.createBlockData();

    private final Undead config;
    private final JavaPlugin plugin;
    private final handSwap swap;

    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();
    private final Map<UUID, BossBar> chargeBars = new HashMap<>();
    private final Map<UUID, BukkitTask> decayTasks = new HashMap<>();

    public Restoration(Undead config, JavaPlugin plugin, handSwap swap) {
        this.config = config;
        this.plugin = plugin;
        this.swap = swap;
    }

    public boolean isCharging(Player player) {
        return chargeTasks.containsKey(player.getUniqueId());
    }

    public void startCharge(Player player) {
        UUID uuid = player.getUniqueId();
        if (chargeTasks.containsKey(uuid) || !swap.isEquipped(player) || player.isDead()) return;

        config.restore_charge_hit_count.put(uuid, 0);

        BossBar bar = Bukkit.createBossBar("Restoration Charge", BarColor.RED, BarStyle.SEGMENTED_10);
        bar.setProgress(0.0);
        bar.addPlayer(player);
        chargeBars.put(uuid, bar);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !swap.isEquipped(player)) {
                    stopCharge(player);
                    return;
                }

                if (!player.getCurrentInput().isSprint()) {
                    stopCharge(player);
                    if (ticks >= config.restore_Min_Ticks && !Stun.isStunned(player)) {
                        restore(player, Math.min(1.0, (double) ticks / config.restore_Charge_Ticks));
                    }
                    return;
                }

                if (ticks < config.restore_Charge_Ticks) ticks++;

                double progress = (double) ticks / config.restore_Charge_Ticks;
                bar.setProgress(Math.min(1.0, progress));

                double baseReduction = progress <= 0.5 ? 0.16 + (0.48 * (progress / 0.5)) : 0.64;
                int hitCount = config.restore_charge_hit_count.getOrDefault(uuid, 0);
                double reduction = Math.max(0.0, baseReduction - hitCount * 0.04);
                config.restore_charge_defend.put(uuid, 1.0 - reduction);

                if (progress >= 1.0) bar.setTitle("Restoration Charge - MAX");
            }
        }.runTaskTimer(plugin, 0L, 1L);

        chargeTasks.put(uuid, task);
    }

    private void stopCharge(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = chargeTasks.remove(uuid);
        if (task != null) task.cancel();
        BossBar bar = chargeBars.remove(uuid);
        if (bar != null) bar.removeAll();

        config.restore_charge_defend.remove(uuid);
        config.restore_charge_hit_count.remove(uuid);
    }

    private void restore(Player player, double chargeRate) {
        if (player.isDead() || !player.isValid()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        UUID uuid = player.getUniqueId();

        boolean isBuffActive = config.restore_buff_ticks.getOrDefault(uuid, 0) > 0;
        double currentBuff = config.restore_buff_rate.getOrDefault(uuid, 0.0);
        double currentDebuff = config.restore_debuff_rate.getOrDefault(uuid, 0.0);

        double addedDebuff = isBuffActive ? (chargeRate * 2.0) : chargeRate;
        config.restore_debuff_rate.put(uuid, Math.min(config.restore_Debuff_Max, currentDebuff + addedDebuff));

        if (!isBuffActive || chargeRate > currentBuff) {
            config.restore_buff_rate.put(uuid, chargeRate);
            config.restore_buff_ticks.put(uuid, (int) (80 + (40 * chargeRate)));
            player.setWalkSpeed((float) (0.2 * (1.0 + chargeRate)));
        }

        double chargeTime = chargeRate * 4.0;
        final int durationTicks = chargeTime >= 1.0
                ? (int) (6 + (7 * ((Math.min(chargeTime, 4.0) - 1.0) / 3.0)))
                : (int) Math.max(1, 6 * chargeTime);

        final double maxHealth = maxHealthAttr.getValue();
        final double healPerTick = maxHealth * chargeRate / durationTicks;

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.7f);

        dash(player, chargeRate, durationTicks);

        new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (currentTick >= durationTicks || !player.isValid() || player.isDead()) {
                    cancel();
                    return;
                }

                player.setHealth(Math.min(maxHealth, player.getHealth() + healPerTick));

                Location center = player.getLocation().add(0, 1.0, 0);

                world.spawnParticle(Particle.BLOCK, center, 5, 0.4, 0.5, 0.4, 0, BLOOD);
                world.spawnParticle(Particle.DUST, center, 3, 0.4, 0.5, 0.4, 0, CELL_RED);

                world.playSound(center, Sound.BLOCK_SLIME_BLOCK_STEP, 0.3f, 1.2f + (currentTick * 0.05f));

                currentTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        startDecay(player);
    }

    private void dash(Player player, double chargeRate, int durationTicks) {
        Vector direction = player.getLocation().getDirection().normalize()
                .multiply(config.restore_Dash_Min + (config.restore_Dash_Max - config.restore_Dash_Min) * chargeRate);

        player.setVelocity(direction);

        new Invulnerable(player, durationTicks * 50L).applyEffect(player);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP,
                (float) (0.6 + 0.4 * chargeRate), (float) (1.7 - 0.5 * chargeRate));
    }

    private void startDecay(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask old = decayTasks.remove(uuid);
        if (old != null) old.cancel();

        BukkitTask task = new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || (!config.restore_buff_ticks.containsKey(uuid) && !config.restore_debuff_rate.containsKey(uuid))) {
                    decayTasks.remove(uuid);
                    cancel();
                    return;
                }

                int buffTicks = config.restore_buff_ticks.getOrDefault(uuid, 0);
                if (buffTicks > 0) {
                    buffTicks--;
                    if (buffTicks <= 0) {
                        config.restore_buff_ticks.remove(uuid);
                        config.restore_buff_rate.remove(uuid);
                        if (player.isOnline()) player.setWalkSpeed(0.2f);
                    } else {
                        config.restore_buff_ticks.put(uuid, buffTicks);
                    }
                }

                if (++counter >= 4) {
                    counter = 0;
                    double rate = config.restore_debuff_rate.getOrDefault(uuid, 0.0);
                    if (rate > 0) {
                        double next = Math.max(0.0, rate - 0.01);
                        if (next <= 0.0) config.restore_debuff_rate.remove(uuid);
                        else config.restore_debuff_rate.put(uuid, next);
                    }
                }

                if (buffTicks <= 0 && config.restore_debuff_rate.getOrDefault(uuid, 0.0) <= 0.0) {
                    decayTasks.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        decayTasks.put(uuid, task);
    }

    public void clear(Player player) {
        stopCharge(player);
        BukkitTask task = decayTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }
}