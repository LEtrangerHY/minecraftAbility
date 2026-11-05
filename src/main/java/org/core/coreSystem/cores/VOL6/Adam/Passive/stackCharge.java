package org.core.coreSystem.cores.VOL6.Adam.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.core.cool.Cool;
import org.core.coreSystem.cores.VOL6.Adam.coreSystem.Adam;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class stackCharge {

    private final Adam config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyF;
    private final NamespacedKey speedKey;
    private final NamespacedKey absorbKey;

    private final Map<UUID, Integer> stacks = new HashMap<>();
    private final Map<UUID, Charge> charges = new HashMap<>();

    private static class Charge {
        private BukkitTask task;
        private BossBar bar;
        private double base;
        private double granted;
        private boolean ready;
        private long started;
    }

    public stackCharge(Adam config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyF = new NamespacedKey(plugin, "F");
        this.speedKey = new NamespacedKey(plugin, "adam_stack_speed");
        this.absorbKey = new NamespacedKey(plugin, "adam_charge_absorption");
    }

    public int count(Player player) {
        return stacks.getOrDefault(player.getUniqueId(), 0);
    }

    public void add(Player player, int amount) {
        if (amount <= 0) return;
        int before = count(player);
        int after = Math.min(config.stack_Max, before + amount);
        if (after == before) return;

        stacks.put(player.getUniqueId(), after);
        speed(player);
        show(player);
        if (before < config.stack_Max && after >= config.stack_Max) cool.updateCooldown(player, "F", 0L);
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.6f, 0.7f + after * 0.08f);
    }

    public int consume(Player player) {
        Integer amount = stacks.remove(player.getUniqueId());
        speed(player);
        return amount == null ? 0 : amount;
    }

    private void show(Player player) {
        int amount = count(player);
        Component bar = amount >= config.stack_Max
                ? Component.text("⬤").color(NamedTextColor.GRAY)
                : Component.text("○ ".repeat(amount).trim()).color(NamedTextColor.GRAY);

        player.showTitle(Title.title(
                Component.empty(),
                bar,
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))
        ));
    }

    public void clear(Player player) {
        stacks.remove(player.getUniqueId());
        stop(player);
        speed(player);
    }

    private void speed(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) return;

        if (attribute.getModifier(speedKey) != null) attribute.removeModifier(speedKey);

        int amount = count(player);
        if (amount > 0) {
            attribute.addTransientModifier(new AttributeModifier(speedKey, config.stack_Speed * amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
        }
    }

    public boolean isReady(Player player) {
        Charge charge = charges.get(player.getUniqueId());
        return charge != null && charge.ready;
    }

    public long requiredMillis(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double scale = level <= config.charge_Free_Level
                ? 1.0
                : Math.max(0.0, 1.0 - config.charge_Level_Cut * (level - config.charge_Free_Level));

        int last = Math.max(1, config.f_Auto_Stack - 1);
        int amount = Math.max(1, Math.min(last, count(player)));
        double ratio = last <= 1 ? 1.0 : (amount - 1) / (double) (last - 1);
        double millis = (config.charge_Min + (config.charge_Max - config.charge_Min) * ratio) * scale;

        return Math.max(50L, Math.round(millis));
    }

    public void start(Player player, Predicate<Player> equipped) {
        stop(player);
        if (count(player) <= 0 || count(player) >= config.f_Auto_Stack || cool.isReloading(player, "F")) return;

        UUID uuid = player.getUniqueId();
        Charge charge = new Charge();
        charge.base = player.getAbsorptionAmount();
        charge.started = System.currentTimeMillis();
        charge.bar = Bukkit.createBossBar("F skill Charge", BarColor.PURPLE, BarStyle.SOLID);
        charge.bar.setProgress(0.0);
        charge.bar.addPlayer(player);
        absorbCap(player, true);
        charges.put(uuid, charge);

        charge.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (charges.get(uuid) != charge) {
                    cancel();
                    return;
                }

                if (!player.isOnline() || player.isDead() || !player.isSneaking() || !equipped.test(player)
                        || cool.isReloading(player, "F") || count(player) <= 0 || count(player) >= config.f_Auto_Stack) {
                    stop(player);
                    return;
                }

                double elapsed = System.currentTimeMillis() - charge.started;
                double progress = Math.min(1.0, elapsed / requiredMillis(player));
                charge.bar.setProgress(progress);
                grant(player, charge, progress);

                if (progress >= 1.0) charge.ready = true;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void grant(Player player, Charge charge, double progress) {
        double target = maxHealth(player) * config.charge_Absorption * progress;
        double amount = target - charge.granted;
        if (amount <= 1.0E-4) return;

        charge.granted = target;
        player.setAbsorptionAmount(player.getAbsorptionAmount() + amount);
    }

    public void stop(Player player) {
        Charge charge = charges.remove(player.getUniqueId());
        if (charge == null) return;

        if (charge.task != null) charge.task.cancel();
        if (charge.bar != null) charge.bar.removeAll();

        double current = player.getAbsorptionAmount();
        if (charge.granted > 0 && current > charge.base) {
            player.setAbsorptionAmount(Math.max(charge.base, current - charge.granted));
        }

        absorbCap(player, false);
    }

    private void absorbCap(Player player, boolean apply) {
        AttributeInstance cap = player.getAttribute(Attribute.MAX_ABSORPTION);
        if (cap == null) return;

        if (cap.getModifier(absorbKey) != null) cap.removeModifier(absorbKey);
        if (!apply) return;

        cap.addTransientModifier(new AttributeModifier(absorbKey, maxHealth(player) * config.charge_Absorption,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    private double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0;
    }
}