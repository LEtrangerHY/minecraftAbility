package org.core.effect.crowdControl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.tag.DamageTypeTags;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForceDamage implements Effects, Listener {
    public static final Map<UUID, Double> FIXED = new HashMap<>();

    private final LivingEntity target;
    private final double damage;
    private final DamageSource damageSource;
    private final boolean isNonKnock;
    private final boolean isFixed;

    public ForceDamage(LivingEntity target, double damage, DamageSource damageSource, boolean isNonKnock) {
        this(target, damage, damageSource, isNonKnock, false);
    }

    public ForceDamage(LivingEntity target, double damage, DamageSource damageSource, boolean isNonKnock, boolean isFixed) {
        this.target = target;
        this.damage = damage;
        this.damageSource = damageSource;
        this.isNonKnock = isNonKnock;
        this.isFixed = isFixed;
    }

    @Override
    public void applyEffect(Entity entity) {
        if(target.isInvulnerable()) return;

        target.setNoDamageTicks(0);
        if (isFixed) FIXED.put(target.getUniqueId(), damage);
        try {
            target.damage(damage, damageSource);
        } finally {
            if (isFixed) FIXED.remove(target.getUniqueId());
        }
        if(isNonKnock) target.setVelocity(new Vector(0, 0, 0));
        target.setNoDamageTicks(10);
    }

    public static double rawDamage(LivingEntity target, double damage, DamageType type) {
        boolean effects = !DamageTypeTags.BYPASSES_EFFECTS.isTagged(type);

        if (effects && !DamageTypeTags.BYPASSES_RESISTANCE.isTagged(type)) {
            PotionEffect resistance = target.getPotionEffect(PotionEffectType.RESISTANCE);
            if (resistance != null) {
                double rate = 1.0 - 0.2 * (resistance.getAmplifier() + 1);
                if (rate <= 0) return 0;
                damage /= rate;
            }
        }

        if (effects && !DamageTypeTags.BYPASSES_ENCHANTMENTS.isTagged(type)) {
            int protection = 0;
            EntityEquipment equipment = target.getEquipment();
            if (equipment != null) {
                for (ItemStack armor : equipment.getArmorContents()) {
                    if (armor != null) protection += armor.getEnchantmentLevel(Enchantment.PROTECTION);
                }
            }
            damage /= 1.0 - Math.min(20, protection) / 25.0;
        }

        if (DamageTypeTags.BYPASSES_ARMOR.isTagged(type)) return damage;

        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = target.getAttribute(Attribute.ARMOR_TOUGHNESS);
        double armor = armorAttr != null ? armorAttr.getValue() : 0.0;
        double toughness = toughnessAttr != null ? toughnessAttr.getValue() : 0.0;

        double low = damage;
        double high = damage * 5.0;
        for (int i = 0; i < 50; i++) {
            double mid = (low + high) / 2.0;
            if (afterArmor(mid, armor, toughness) < damage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static double afterArmor(double damage, double armor, double toughness) {
        double value = armor - damage / (2.0 + toughness / 4.0);
        double reduce = value < armor * 0.2 ? armor * 0.2 : Math.min(value, 20.0);
        return damage * (1.0 - reduce / 25.0);
    }

    @EventHandler
    public void quitRemove(PlayerQuitEvent event){
        Player player = event.getPlayer();
        player.setNoDamageTicks(10);
    }

    @Override
    public void removeEffect(Entity entity) {
    }
}