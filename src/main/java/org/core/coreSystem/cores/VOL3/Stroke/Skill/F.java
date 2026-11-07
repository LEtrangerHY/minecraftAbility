package org.core.coreSystem.cores.VOL3.Stroke.Skill;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Stroke.coreSystem.Stroke;

import java.util.UUID;

public class F implements SkillBase {
    private final Stroke config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Stroke config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        cool.setCooldown(player, config.f_Skill_Cool, "F");

        if (config.fTasks.containsKey(uuid)) {
            config.fTasks.get(uuid).cancel();
        }

        config.isFActive.put(uuid, true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 0, false, false, false));
        cool.setCooldown(player, 7000, "RUN", "boss");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= 140) {
                    config.isFActive.put(uuid, false);
                    config.fTasks.remove(uuid);
                    cool.updateCooldown(player, "RUN", 0);
                    this.cancel();
                    return;
                }

                if (ticks % 4 == 0) {
                    player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation().add(0, 0.5, 0), 10, 0.4, 0.4, 0.4, 0.05);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        config.fTasks.put(uuid, task);
    }
}