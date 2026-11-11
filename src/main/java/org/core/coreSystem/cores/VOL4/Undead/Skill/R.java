package org.core.coreSystem.cores.VOL4.Undead.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL4.Undead.Passive.basicAttack;
import org.core.coreSystem.cores.VOL4.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;

public class R implements SkillBase {

    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final handSwap swap;
    private final basicAttack attack;

    public R(Undead config, JavaPlugin plugin, Cool cool, handSwap swap, basicAttack attack) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.swap = swap;
        this.attack = attack;
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
        attack.combo(player);
    }

    private void firearm(Player player) {
        attack.burst(player);
    }
}