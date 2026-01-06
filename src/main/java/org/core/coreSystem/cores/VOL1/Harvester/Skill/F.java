package org.core.coreSystem.cores.VOL1.Harvester.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.Harvester;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class F implements SkillBase {

    public final Harvester config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Harvester config, JavaPlugin plugin, Cool cool){
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        Slash(player, player.getLocation().clone().add(0, 0.8, 0));

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(-config.f_Skill_dash);

        player.setVelocity(direction);
    }

    public void Slash(Player player, Location playerLoc) {

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        double slashLength = 4.4;
        double maxAngle = Math.toRadians(105);
        double maxTicks = 6;
        double innerRadius = 2.2;

        config.f_damaged.put(player.getUniqueId(), new HashSet<>());
        config.fskill_using.put(player.getUniqueId(), true);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        Random rand = new Random();
        int randomTilt = rand.nextInt(6);

        double tiltAngle = switch (randomTilt) {
            case 0 -> Math.toRadians(4);
            case 1 -> Math.toRadians(-4);
            case 2 -> Math.toRadians(6);
            case 3 -> Math.toRadians(-6);
            case 4 -> Math.toRadians(8);
            case 5 -> Math.toRadians(-8);
            default -> Math.toRadians(0);
        };

        boolean tiltPosZ = Math.random() > 0.5;

        Vector direction = playerLoc.getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(144, 108, 44), 0.5f);
        Particle.DustOptions dustOption_slash_gra = new Particle.DustOptions(Color.fromRGB(166, 144, 108), 0.5f);

        double finalDamage = damage;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        world.spawnParticle(Particle.SWEEP_ATTACK, playerLoc.clone().add(0, 1.0, 0), 10, 2, 2, 2, 1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(ticks == 2 || ticks == 4 || ticks == 6){
                    world.spawnParticle(Particle.SWEEP_ATTACK, playerLoc.clone().add(0, 1.0, 0), 10, 2, 2, 2, 1);
                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                    world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);
                }

                if (ticks >= maxTicks || player.isDead() || !player.isOnline()) {

                    config.f_damaged.remove(player.getUniqueId());

                    int r = config.repeat.getOrDefault(player.getUniqueId(), 0);

                    if(r < config.grass.getOrDefault(player.getUniqueId(), 0) / 2 && player.isOnline()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        config.repeat.put(player.getUniqueId(), r + 1);
                        Slash(player, playerLoc.clone());
                        }, 2L);
                    }else{
                        config.fskill_using.remove(player.getUniqueId());
                        config.repeat.remove(player.getUniqueId());
                        config.grass.remove(player.getUniqueId());
                    }

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= slashLength; length += 0.2) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(4)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        double cosTilt = Math.cos(tiltAngle);
                        double sinTilt = Math.sin(tiltAngle);

                        double tiltedY_Z = particleOffset.getY() * cosTilt - particleOffset.getZ() * sinTilt;
                        double tiltedZ_Y = particleOffset.getY() * sinTilt + particleOffset.getZ() * cosTilt;

                        double tiltedY_X = particleOffset.getY() * cosTilt + particleOffset.getX() * sinTilt;
                        double tiltedX_Y = particleOffset.getY() * sinTilt + particleOffset.getX() * cosTilt;

                        if(tiltPosZ) {
                            particleOffset.setY(tiltedY_Z);
                            particleOffset.setZ(tiltedZ_Y);
                        }else{
                            particleOffset.setY(tiltedY_X);
                            particleOffset.setX(tiltedX_Y);
                        }

                        Location particleLocation = playerLoc.clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(playerLoc);

                        if (distanceFromOrigin >= innerRadius) {
                            if (Math.random() < 0.26) {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                            } else {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash_gra);
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.4, 0.4, 0.4)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.3, 0), 20, 0.4, 0.4, 0.4, 1);

                                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, (int) maxTicks, 5, false, false);
                                target.addPotionEffect(slowness);

                                player.heal(finalDamage / 2);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }

                        Block blockDown = particleLocation.clone().getBlock();
                        Block blockUp = particleLocation.clone().add(0, 1, 0).getBlock();
                        if (blockDown.getType() == Material.SHORT_GRASS || blockDown.getType() == Material.TALL_GRASS || blockDown.getType() == Material.WHEAT || blockDown.getType() == Material.POTATOES || blockDown.getType() == Material.CARROTS || blockDown.getType() == Material.BEETROOTS) {
                            breakBlockSafely(player, blockDown);
                            if(config.grass.getOrDefault(player.getUniqueId(), 0) < 26){
                                int g = config.grass.getOrDefault(player.getUniqueId(), 0);
                                config.grass.put(player.getUniqueId(), g + 1);
                            }
                        }
                        if (blockUp.getType() == Material.SHORT_GRASS || blockUp.getType() == Material.TALL_GRASS || blockUp.getType() == Material.WHEAT || blockUp.getType() == Material.POTATOES || blockUp.getType() == Material.CARROTS || blockUp.getType() == Material.BEETROOTS) {
                            breakBlockSafely(player, blockDown);
                            if(config.grass.getOrDefault(player.getUniqueId(), 0) < 26){
                                int g = config.grass.getOrDefault(player.getUniqueId(), 0);
                                config.grass.put(player.getUniqueId(), g + 1);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.GRASS_BLOCK
    );

    public void breakBlockSafely(Player player, Block block) {
        if (UNBREAKABLE_BLOCKS.contains(block.getType())) {
            return;
        }

        block.getWorld().spawnParticle(
                Particle.BLOCK,
                block.getLocation().add(0.5, 0.5, 0.5),
                6,
                0.3, 0.3, 0.3,
                block.getBlockData()
        );

        block.breakNaturally(new ItemStack(Material.IRON_HOE));
    }
}
