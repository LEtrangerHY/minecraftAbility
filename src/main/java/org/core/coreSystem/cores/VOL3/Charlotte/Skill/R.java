package org.core.coreSystem.cores.VOL3.Charlotte.Skill;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Charlotte.coreSystem.Charlotte;

public class R implements SkillBase {

    private final Charlotte config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Charlotte config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

    }
}
