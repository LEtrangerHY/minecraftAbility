package org.core.coreSystem.cores.VOL1.Saboteur.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.Saboteur;

public class F implements SkillBase {
    private final Saboteur config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Saboteur config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 10, 5, false, true);

        cool.setCooldown(player, 13000L, "Berserk");

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {

                config.isHackAway.put(player.getUniqueId(), true);
                player.addPotionEffect(poison);

                if (!player.isOnline() || player.isDead() || tick > 20 * 13 || !config.isHackAway.getOrDefault(player.getUniqueId(), false)) {
                    config.isHackAway.remove(player.getUniqueId());
                    cool.updateCooldown(player , "Berserk", 0L);

                    cool.setCooldown(player, 4000L, "R");
                    cool.setCooldown(player, 4000L, "Q");

                    cool.updateCooldown(player, "R", 4000L);
                    cool.updateCooldown(player, "Q", 4000L);

                    cancel();
                    return;
                }

                tick += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
