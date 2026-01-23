package org.core.coreSystem.cores.VOL2.Rose.Skill;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Rose.Passive.bloodPetal;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;

public class R implements SkillBase {

    private final Rose config;
    private final JavaPlugin plugin;
    private final Cool cool;
    // petal은 이제 직접 쓰지 않지만 구조상 유지 (필요 없다면 제거 가능)
    private final bloodPetal petal;

    public R(Rose config, JavaPlugin plugin, Cool cool, bloodPetal petal) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.petal = petal;
    }

    @Override
    public void Trigger(Player player) {
        player.swingOffHand();

        Entity target = getTargetEntity(player, 3.0);

        if (target != null) {
            config.atk.put(player.getUniqueId(), "SKILL_R");

            try {
                player.attack(target);
            } finally {
                config.atk.remove(player.getUniqueId());
            }

            handleDurability(player);
        }
    }

    private void handleDurability(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemMeta meta = offHand.getItemMeta();
        if (meta instanceof Damageable damageable && offHand.getType().getMaxDurability() > 0) {
            int newDamage = damageable.getDamage() + 1;
            damageable.setDamage(newDamage);
            offHand.setItemMeta(meta);

            if (newDamage >= offHand.getType().getMaxDurability()) {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    private Entity getTargetEntity(Player player, double range) {
        Location eyeLoc = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc,
                eyeLoc.getDirection(),
                range,
                0.5,
                entity -> entity != player && entity instanceof LivingEntity
        );
        return (result != null) ? result.getHitEntity() : null;
    }
}