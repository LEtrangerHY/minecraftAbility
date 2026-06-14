package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

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

    private final R_axe Axe;
    private final R_pickaxe Pickaxe;
    private final R_pistol Pistol;
    private final R_scythe Scythe;
    private final R_shovel Shovel;

    public R(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;

        this.Axe = new R_axe(config, plugin, cool);
        this.Pickaxe = new R_pickaxe(config, plugin, cool);
        this.Pistol = new R_pistol(config, plugin, cool);
        this.Scythe = new R_scythe(config, plugin, cool);
        this.Shovel = new R_shovel(config, plugin, cool);
    }

    @Override
    public void Trigger(Player player) {
        Material handMat = player.getInventory().getItemInMainHand().getType();
        String coolKey = config.getWeaponCoolKey(handMat);

        if (coolKey == null) return;

        if (cool.isReloading(player, coolKey)) return;

        cool.setCooldown(player, config.r_Skill_Cool, coolKey);
        cool.setCooldown(player, config.r_Skill_Cool, "R");

        switch (handMat) {
            case IRON_HORSE_ARMOR:
                Pistol.Trigger(player);
                break;
            case IRON_AXE:
                Axe.Trigger(player);
                break;
            case IRON_PICKAXE:
                Pickaxe.Trigger(player);
                break;
            case IRON_HOE:
                Scythe.Trigger(player);
                break;
            case IRON_SHOVEL:
                Shovel.Trigger(player);
                break;
            default:
                break;
        }
    }
}