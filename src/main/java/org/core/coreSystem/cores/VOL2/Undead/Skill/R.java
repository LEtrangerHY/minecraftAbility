package org.core.coreSystem.cores.VOL2.Undead.Skill;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

public class R implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        Material handMat = player.getInventory().getItemInMainHand().getType();
        String coolKey = config.getWeaponCoolKey(handMat);

        if (coolKey == null) return;

        if (cool.isReloading(player, coolKey)) return;

        player.sendMessage("§a[" + handMat.name() + "] 스킬 시전!");

        cool.setCooldown(player, config.r_Skill_Cool, coolKey);

        cool.setCooldown(player, config.r_Skill_Cool, "R");
    }
}