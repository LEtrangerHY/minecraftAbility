package org.core.coreSystem.cores.VOL3.Claud.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Claud.coreSystem.Claud;

public class F implements SkillBase {
    private final Claud config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Claud config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

    }
}