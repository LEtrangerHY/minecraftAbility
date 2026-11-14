package org.core.coreSystem.cores.VOL2.Undead.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class handSwap {

    private static final List<String> SLOTS = List.of("R", "Q", "F");
    private static final Particle.DustOptions STEEL = new Particle.DustOptions(Color.fromRGB(204, 208, 214), 0.7f);
    private static final Particle.DustOptions RESET = new Particle.DustOptions(Color.fromRGB(150, 16, 20), 1.0f);

    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final Map<UUID, Boolean> dropMode = new HashMap<>();

    public handSwap(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void markDrop(Player player, boolean firearm) {
        UUID uuid = player.getUniqueId();
        dropMode.put(uuid, firearm);
        Bukkit.getScheduler().runTask(plugin, () -> dropMode.remove(uuid));
    }

    public boolean isMelee(Player player) {
        Boolean dropped = dropMode.get(player.getUniqueId());
        if (dropped != null) return !dropped;

        return config.isTool(player.getInventory().getItemInMainHand().getType())
                && config.isGun(player.getInventory().getItemInOffHand().getType());
    }

    public boolean isFirearm(Player player) {
        Boolean dropped = dropMode.get(player.getUniqueId());
        if (dropped != null) return dropped;

        return config.isGun(player.getInventory().getItemInMainHand().getType())
                && config.isTool(player.getInventory().getItemInOffHand().getType());
    }

    public boolean isEquipped(Player player) {
        return isMelee(player) || isFirearm(player);
    }

    public boolean isSwapping(Player player) {
        return config.is_swapping.contains(player.getUniqueId());
    }

    public boolean isBusy(Player player) {
        UUID uuid = player.getUniqueId();
        return config.is_swapping.contains(uuid) || config.is_acting.contains(uuid);
    }

    public String command(Player player) {
        return config.swap_Command.computeIfAbsent(player.getUniqueId(),
                uuid -> SLOTS.get(ThreadLocalRandom.current().nextInt(SLOTS.size())));
    }

    private List<String> others(String slot) {
        List<String> others = new ArrayList<>(SLOTS);
        others.remove(slot);
        return others;
    }

    public boolean isResetReady(Player player) {
        for (String other : others(command(player))) {
            if (!cool.isReloading(player, other)) return false;
        }
        return true;
    }

    public Component actionBar(Player player, String slot, long remaining, Component label, Component status) {
        if (!slot.equals(command(player))) return null;

        boolean reset = isResetReady(player);
        Component name = label.color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, reset);
        Component state = remaining > 0
                ? status
                : status.color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, reset);

        return Component.empty().append(name).append(state);
    }

    public void use(Player player, String slot, SkillBase skill) {
        boolean isCommand = slot.equals(command(player));
        boolean reset = isCommand && isResetReady(player);

        skill.Trigger(player);

        if (!isCommand) return;

        List<String> others = others(slot);

        if (reset) {
            for (String other : others) {
                cool.updateCooldown(player, other, 0L);
            }
            resetEffect(player);

            config.swap_Command.put(player.getUniqueId(), others.get(ThreadLocalRandom.current().nextInt(others.size())));
        }

        swapHands(player);
    }

    private void swapHands(Player player) {
        UUID uuid = player.getUniqueId();
        config.is_swapping.add(uuid);

        new BukkitRunnable() {
            int waited = 0;

            @Override
            public void run() {
                dropMode.remove(uuid);
                if (config.is_acting.contains(uuid) && waited++ < 100 && player.isOnline() && !player.isDead()) return;

                cancel();
                swapNow(player);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void swapNow(Player player) {
        UUID uuid = player.getUniqueId();

        try {
            if (!player.isOnline() || player.isDead() || !isEquipped(player)) return;

            ItemStack main = player.getInventory().getItemInMainHand().clone();
            ItemStack off = player.getInventory().getItemInOffHand().clone();

            player.getInventory().setItemInMainHand(off);
            player.getInventory().setItemInOffHand(main);
            player.updateInventory();

            swapEffect(player, config.isGun(off.getType()));
        } finally {
            config.is_swapping.remove(uuid);
        }
    }

    private void swapEffect(Player player, boolean firearm) {
        World world = player.getWorld();
        Location base = player.getLocation();
        double yaw = Math.toRadians(base.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Location chest = base.clone().add(0, 1.05, 0).add(forward.clone().multiply(0.45));

        for (int i = 0; i <= 8; i++) {
            double side = -0.45 + 0.9 * i / 8.0;
            world.spawnParticle(Particle.DUST, chest.clone().add(right.clone().multiply(side)), 1, 0, 0, 0, 0, STEEL);
        }
        world.spawnParticle(Particle.CRIT, chest.clone().add(right.clone().multiply(0.4)), 5, 0.08, 0.08, 0.08, 0.12);

        if (firearm) {
            world.playSound(base, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.3f);
            world.playSound(base, Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.2f);
            world.playSound(base, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.5f, 1.7f);
        } else {
            world.playSound(base, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.9f);
            world.playSound(base, Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.9f, 0.8f);
            world.playSound(base, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.4f);
        }
    }

    private void resetEffect(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation();
        Location center = base.clone().add(0, 1.0, 0);

        world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.4f);
        world.playSound(center, Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.9f, 1.3f);
        world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.7f);

        world.spawnParticle(Particle.ENCHANTED_HIT, center, 24, 0.45, 0.6, 0.45, 0.15);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            world.spawnParticle(Particle.DUST, base.clone().add(Math.cos(angle) * 1.1, 0.1, Math.sin(angle) * 1.1), 1, 0, 0, 0, 0, RESET);
        }
    }
}