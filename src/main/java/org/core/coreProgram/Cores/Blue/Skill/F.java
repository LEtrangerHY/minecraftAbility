package org.core.coreProgram.Cores.Blue.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Blue.coreSystem.Blue;

public class F implements SkillBase {
    private final Blue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Blue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

    }
}
