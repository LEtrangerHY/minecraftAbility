package org.core.coreSystem.cores.VOL2.Rose.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;

public class Q implements SkillBase {

    private final Rose config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Rose config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

    }
}
