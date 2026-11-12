package org.core.coreSystem.cores.VOL4.Undead.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL4.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;

import java.util.UUID;

public class F implements SkillBase {

    private static final String BAR = "REVENANT";

    private static final Particle.DustOptions BRIGHT = new Particle.DustOptions(Color.fromRGB(255, 226, 226), 0.8f);
    private static final Particle.DustOptions CRIMSON = new Particle.DustOptions(Color.fromRGB(214, 30, 40), 0.9f);
    private static final Particle.DustOptions DEEP = new Particle.DustOptions(Color.fromRGB(110, 10, 18), 0.7f);

    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final handSwap swap;

    public F(Undead config, JavaPlugin plugin, Cool cool, handSwap swap) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.swap = swap;
    }

    @Override
    public void Trigger(Player player) {
        if (swap.isFirearm(player)) {
            firearm(player);
        } else {
            melee(player);
        }
    }

    private void melee(Player player) {
        empower(player);
    }

    private void firearm(Player player) {
        empower(player);
    }

    private void empower(Player player) {
        UUID uuid = player.getUniqueId();
        Double removed = config.restore_debuff_rate.remove(uuid);
        double debuff = removed == null ? 0.0 : removed;

        double ratio = Math.min(config.f_Skill_Cost_Max, config.f_Skill_Cost_Ratio * debuff);
        player.setHealth(Math.max(0.5, player.getHealth() * (1.0 - ratio)));

        Double charge = Double.valueOf(debuff);
        config.f_Empower.put(uuid, charge);
        cool.setCooldown(player, config.f_Skill_Window * 50L, BAR, "boss");

        double scale = config.f_Skill_Scale + config.f_Skill_Scale_Step * debuff;
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 1.0, 0);

        world.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 1.4f, 0.7f);
        world.playSound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.7f);
        world.playSound(player, Sound.ITEM_TRIDENT_RETURN, 1.2f, 0.6f);
        world.playSound(player, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.9f, 0.6f);

        world.spawnParticle(Particle.DUST, center, (int) (14 + 10 * debuff), 0.35, 0.6, 0.35, 0, CRIMSON);
        world.spawnParticle(Particle.DAMAGE_INDICATOR, center, 4, 0.3, 0.4, 0.3, 0.1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    config.f_Empower.remove(uuid);
                    cool.updateCooldown(player, BAR, 0L);
                    cancel();
                    return;
                }

                if (config.f_Empower.get(uuid) != charge) {
                    if (!config.f_Empower.containsKey(uuid)) cool.updateCooldown(player, BAR, 0L);
                    cancel();
                    return;
                }

                if (ticks >= config.f_Skill_Window) {
                    config.f_Empower.remove(uuid);
                    cool.updateCooldown(player, BAR, 0L);
                    world.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.4f);
                    world.spawnParticle(Particle.SMOKE, hand(player), 6, 0.05, 0.05, 0.05, 0.02);
                    cancel();
                    return;
                }

                if (ticks < 6) {
                    double radius = 0.4 + 0.25 * ticks * scale;
                    for (int i = 0; i < 18; i++) {
                        double angle = Math.PI * 2 * i / 18;
                        world.spawnParticle(Particle.DUST, player.getLocation().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius),
                                1, 0, 0, 0, 0, ticks < 3 ? CRIMSON : DEEP);
                    }
                }

                if (ticks % 2 == 0) {
                    Location at = hand(player);
                    world.spawnParticle(Particle.DUST, at, 2, 0.06, 0.06, 0.06, 0, ticks % 4 == 0 ? BRIGHT : CRIMSON);
                }

                if (ticks % 20 == 0) world.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.2f);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Location hand(Player player) {
        Location eye = player.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        return eye.add(eye.getDirection().multiply(0.45)).add(right.multiply(0.35)).add(0, -0.45, 0);
    }
}