package org.core.coreSystem.cores.VOL3.Darmes.Skill;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Darmes.coreSystem.Darmes;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashSet;
import java.util.UUID;

public class F implements SkillBase {

    private final Darmes config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyF;

    public F(Darmes config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
    private static final Particle.DustOptions DUST_SLASH_GRA = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.6f);

    @Override
    public void Trigger(Player player) {
        Slam(player);
    }

    public void Slam(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        player.swingMainHand();

        world.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.6f);

        double slashLength = 4.8;
        double maxTicks = 6;
        double innerRadius = 2.0;

        HashSet<Entity> damagedSet = new HashSet<>();

        long level = player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double ampMultiplier = 1 + (config.f_Skill_amp * level);
        double damage = config.f_Skill_Damage * ampMultiplier;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, -0.4, 0);

        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        double dirX = direction.getX();
        double dirZ = direction.getZ();

        double rightX = -dirZ;
        double rightZ = dirX;

        double originX = origin.getX();
        double originY = origin.getY();
        double originZ = origin.getZ();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks < 4)
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 0.8f);

                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = Math.toRadians(80) - (ticks * Math.toRadians(110) / maxTicks);
                double cosP = Math.cos(progress);
                double sinP = Math.sin(progress);

                for (double length = innerRadius; length <= slashLength; length += 0.18) {
                    for (double angle = -Math.toRadians(25); angle <= Math.toRadians(25); angle += Math.toRadians(4.0)) {

                        double fComp = cosP * length;
                        double yComp = sinP * length;
                        double rComp = Math.sin(angle) * 0.45;

                        double pX = originX + (dirX * fComp) + (rightX * rComp);
                        double pY = originY + yComp;
                        double pZ = originZ + (dirZ * fComp) + (rightZ * rComp);

                        Particle.DustOptions opt = (Math.random() < 0.15) ? DUST_SLASH : DUST_SLASH_GRA;
                        world.spawnParticle(Particle.DUST, pX, pY, pZ, 3, 0.4, 0.4, 0.4, 0, opt);
                    }
                }

                if (ticks == maxTicks - 1) {
                    Location impactLoc = player.getLocation().add(direction.clone().multiply(2.5));

                    world.playSound(impactLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.6f);
                    world.playSound(impactLoc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.2f);
                    world.spawnParticle(Particle.EXPLOSION, impactLoc.add(0, 0.2, 0), 3, 1.0, 0.1, 1.0, 0);
                    world.spawnParticle(Particle.CRIT, impactLoc.clone().add(0, 0.5, 0), 20, 1.0, 0.5, 1.0, 0.1);

                    Material groundMat = impactLoc.clone().subtract(0, 0.1, 0).getBlock().getType();
                    if (groundMat.isSolid()) {
                        world.spawnParticle(Particle.BLOCK, impactLoc, 45, 1.2, 0.4, 1.2, groundMat.createBlockData());
                    }

                    for (Entity e : world.getNearbyEntities(impactLoc, 2.0, 1.8, 2.0)) {
                        if (!(e instanceof LivingEntity target) || e == player) continue;

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);
                        damagedSet.add(target);

                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
                    }

                    int hitCount = damagedSet.size();
                    if (hitCount > 0) {

                        double currentBlow = config.blowParameter.getOrDefault(uuid, 0.0);
                        double nextBlow = Math.min(3.3, currentBlow + (hitCount * config.blowParapeterInc));
                        config.blowParameter.put(uuid, nextBlow);

                        int gainedAbs = Math.min(3, hitCount);
                        int currentAbsorption = player.getAbsorptionAmount() > 0 ? (int) player.getAbsorptionAmount() : 0;
                        int newAbsorption = Math.min(currentAbsorption + gainedAbs, 12);

                        int amplifier = Math.max(0, (newAbsorption / 2) - 1);

                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, amplifier, false, false, false));
                        player.setAbsorptionAmount(newAbsorption);

                        double defaultHeal = Math.min(6.0, hitCount * 1.0);

                        double finalHeal = defaultHeal * ampMultiplier;

                        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            double maxHealth = maxHealthAttr.getValue();
                            player.setHealth(Math.min(maxHealth, player.getHealth() + finalHeal));
                        }

                        long currentQCool = cool.getRemainCooldown(player, "Q");
                        if (currentQCool > 0) {
                            long reduction = Math.min(3, hitCount) * 1000L;
                            long newQCool = Math.max(0L, currentQCool - reduction);
                            cool.updateCooldown(player, "Q", newQCool);
                        }

                        world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
                        world.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}