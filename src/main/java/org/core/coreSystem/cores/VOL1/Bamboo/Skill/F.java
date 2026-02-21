package org.core.coreSystem.cores.VOL1.Bamboo.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Bamboo.coreSystem.Bamboo;
import org.core.effect.crowdControl.Invulnerable;

import java.time.Duration;

public class F implements SkillBase {

    private final Bamboo config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final R rSkill;

    private static final Particle.DustOptions DUST_DASH = new Particle.DustOptions(Color.fromRGB(200, 255, 200), 0.6f);

    public F(Bamboo config, JavaPlugin plugin, Cool cool, R rSkill) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.rSkill = rSkill;
    }

    @Override
    public void Trigger(Player player) {
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);

        boolean isSpearActive = rSkill.isSessionActive(player);
        boolean isLeashIntact = rSkill.isLeashIntact(player);

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        boolean hasIron = offhandItem.getType() == Material.IRON_NUGGET && offhandItem.getAmount() > 6;

        if (isSpearActive && isLeashIntact && hasIron) {
            grappleManeuver(player, offhandItem);
        }
        else {
            if (isSpearActive && isLeashIntact && !hasIron) {
                Title title = Title.title(
                        Component.empty(),
                        Component.text("iron needed").color(NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
                );
                player.showTitle(title);
            }
            standardDash(player);
        }
    }

    private void standardDash(Player player) {
        cool.updateCooldown(player, "F", 10000L);

        config.isDashing.add(player.getUniqueId());

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(1.6);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.getWorld().playSound(startLocation, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player, false);
    }

    private void grappleManeuver(Player player, ItemStack offhandItem) {
        offhandItem.setAmount(offhandItem.getAmount() - 6);

        Location targetLoc = rSkill.retrieveSpearLocation(player);

        rSkill.triggerKillReset(player);

        if (targetLoc == null) {
            standardDash(player);
            return;
        }

        config.isDashing.add(player.getUniqueId());

        cool.updateCooldown(player, "F", 500L);
        config.reloaded.put(player.getUniqueId(), true);

        Location currentLoc = player.getLocation();
        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());

        double distance = currentLoc.distance(targetLoc);
        double speed = Math.min(2.5, 1.0 + (distance * 0.1));

        player.setVelocity(direction.normalize().multiply(speed));

        player.getWorld().playSound(currentLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.2f);
        player.getWorld().playSound(currentLoc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.5f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player, true);
    }

    public void detect(Player player, boolean isGrapple) {
        World world = player.getWorld();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 8 || player.isDead()) {
                    config.isDashing.remove(player.getUniqueId());

                    if (player.isOnline() && !player.isDead()) {
                        if (isGrapple) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30, 0, false, false));
                        }
                    }

                    cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DUST, pLoc, 10, 0.3, 0.3, 0.3, 0.0, DUST_DASH);
                if (isGrapple) {
                    world.spawnParticle(Particle.CRIT, pLoc, 5, 0.2, 0.2, 0.2, 0.1);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}