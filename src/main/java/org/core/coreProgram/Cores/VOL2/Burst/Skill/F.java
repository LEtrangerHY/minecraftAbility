package org.core.coreProgram.Cores.VOL2.Burst.Skill;

import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.core.coreProgram.Cores.VOL2.Burst.coreSystem.Burst;

public class F implements SkillBase {
    private final Burst config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Burst config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

    }
}
