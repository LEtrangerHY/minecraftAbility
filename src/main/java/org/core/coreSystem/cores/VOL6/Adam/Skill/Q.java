package org.core.coreSystem.cores.VOL6.Adam.Skill;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Adam.coreSystem.Adam;

public class Q implements SkillBase {

    private final Adam config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Adam config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

    }
}