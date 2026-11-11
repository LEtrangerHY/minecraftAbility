package org.core.coreSystem.cores.VOL4.Undead.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL4.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;

public class F implements SkillBase {

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

    }

    private void firearm(Player player) {

    }
}