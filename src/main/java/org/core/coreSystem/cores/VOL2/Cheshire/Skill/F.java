package org.core.coreSystem.cores.VOL2.Cheshire.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Cheshire.coreSystem.Cheshire;
import java.util.UUID;
import java.util.function.Consumer;

public class F implements SkillBase {

    private final Cheshire config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Consumer<Player> breakInvisibility;

    public F(Cheshire config, JavaPlugin plugin, Cool cool, Consumer<Player> breakInvisibility) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.breakInvisibility = breakInvisibility;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        NamespacedKey fKey = new NamespacedKey(plugin, "F");
        double fAmp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(fKey, PersistentDataType.LONG, 0L);

        if (!cool.isReloading(player, "GRIN WITHOUT A CAT")) {
            cool.setCooldown(player, 40000L, "GRIN WITHOUT A CAT", "boss");
            cool.setCooldown(player, 6000L, "F_DASH");

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
            config.invisState.add(uuid);

            Location castLoc = player.getLocation().add(0.0, 1.0, 0.0);
            castLoc.getWorld().playSound(castLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 0.5f);
            castLoc.getWorld().spawnParticle(Particle.SMOKE, castLoc, 40, 0.5, 0.5, 0.5, 0.1);
            castLoc.getWorld().spawnParticle(Particle.WITCH, castLoc, 30, 0.5, 0.5, 0.5, 0.1);

            new BukkitRunnable() {
                int tick = 0;
                @Override
                public void run() {
                    if (tick > 10 || player.isDead()) {
                        this.cancel();
                        return;
                    }
                    Location loc = player.getLocation().add(0.0, tick * 0.2, 0.0);
                    double radius = 1.5 - (tick * 0.1);

                    for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 4) {
                        double x = radius * Math.cos(theta + (tick * 0.5));
                        double z = radius * Math.sin(theta + (tick * 0.5));

                        player.spawnParticle(Particle.SMOKE, loc.clone().add(x, 0.0, z), 2, 0.0, 0.0, 0.0, 0.05);
                        player.spawnParticle(Particle.PORTAL, loc.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, 0.02);
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                breakInvisibility.accept(player);
            }, 40L);

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2, false, false));
            config.fUltBuffActive.put(uuid, true);
            config.fUltSmileCounts.put(uuid, 0);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                config.fUltBuffActive.remove(uuid);
                config.fUltSmileCounts.remove(uuid);
            }, 160L);

        } else if (!cool.isReloading(player, "F_DASH")) {
            cool.setCooldown(player, 6000L, "F_DASH");

            Vector dir = player.getLocation().getDirection().setY(0.0).normalize();
            player.setVelocity(dir.multiply(1.2).setY(0.25));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 1.2f);

            final Location startLoc = player.getLocation().add(0.0, 1.0, 0.0);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location endLoc = player.getLocation().add(0.0, 1.0, 0.0);
                double distance = startLoc.distance(endLoc);
                Vector step = endLoc.toVector().subtract(startLoc.toVector()).normalize().multiply(0.3);

                Location lineLoc = startLoc.clone();
                boolean isInvis = config.invisState.contains(uuid);

                for (double i = 0.0; i < distance; i += 0.3) {
                    lineLoc.add(step);
                    if (isInvis) {
                        player.spawnParticle(Particle.SMOKE, lineLoc, 2, 0.1, 0.1, 0.1, 0.0);
                    } else {
                        lineLoc.getWorld().spawnParticle(Particle.SMOKE, lineLoc, 2, 0.1, 0.1, 0.1, 0.0);
                    }
                }
            }, 3L);
        }
    }
}