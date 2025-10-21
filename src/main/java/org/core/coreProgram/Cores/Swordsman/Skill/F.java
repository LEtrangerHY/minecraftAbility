package org.core.coreProgram.Cores.Swordsman.Skill;

import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Swordsman.Passive.Laido;
import org.core.coreProgram.Cores.Swordsman.coreSystem.Swordsman;

public class F implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Laido laido;

    public F(Swordsman config, JavaPlugin plugin, Cool cool, Laido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
    }

    @Override
    public void Trigger(Player player){

    }
}
