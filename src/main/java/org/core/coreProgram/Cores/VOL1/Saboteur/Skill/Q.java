package org.core.coreProgram.Cores.VOL1.Saboteur.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL1.Saboteur.coreSystem.Saboteur;

import java.util.*;

public class Q implements SkillBase {
    private final Saboteur config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Saboteur config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        if(config.isHackAway.getOrDefault(player.getUniqueId(), false)){
            cool.updateCooldown(player, "Q", config.q_Skill_Cool_HACK);
            hackDash(player);
            return;
        }

        trapSetting(player);
    }

    public void trapSetting(Player player){
        World world = player.getWorld();
        Block targetBlock = getCustomTargetBlock(player, 10);
        Particle.DustOptions activeFalse = new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.0f);
        Particle.DustOptions activeTrue = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            world.playSound(player.getLocation().clone(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            cool.updateCooldown(player, "Q", 500L);
            return;
        }

        world.playSound(targetBlock.getLocation().clone(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

        Location particleLoc = targetBlock.getLocation().add(0.5, 1.1, 0.5);
        world.spawnParticle(Particle.DUST, particleLoc, 60, 0.7, 0, 0.7, 0.08, activeFalse);

        config.trapPedalPos.remove(player.getUniqueId());
        config.trapPedalPos.put(player.getUniqueId(), particleLoc);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || tick > 240 || !config.trapPedalPos.containsValue(particleLoc)) {
                    config.trapPedalPos.remove(player.getUniqueId(), particleLoc);
                    cancel();
                    return;
                }

                if(tick > 26) {
                    player.spawnParticle(Particle.DUST, particleLoc, 30, 0.7, 0, 0.7, 0.08, activeTrue);
                }

                List<Entity> nearbyEntities = (List<Entity>) particleLoc.getNearbyEntities(1.5, 0.4, 1.5);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && tick > 26) {

                        cool.setCooldown(player, 4000L, "R");
                        cool.setCooldown(player, 4000L, "Q");

                        cool.updateCooldown(player, "R", 4000L);
                        cool.updateCooldown(player, "Q", 4000L);

                        world.playSound(targetBlock.getLocation().clone(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);

                        world.spawnParticle(Particle.SPIT, target.getLocation().clone().add(0, 0.2, 0), 20, 0.2, 0.3, 0.2, 0.5);
                        target.setVelocity(new Vector(0, 0, 0));

                        if(!config.trapThrowPos.getOrDefault(player.getUniqueId(), new ArrayList<>()).isEmpty()) {
                            config.trapTarget.put(player.getUniqueId(), target);
                        }

                        config.trapPedalPos.remove(player.getUniqueId(), particleLoc);

                        trapActive(player);

                        cancel();
                        return;
                    }
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void trapActive(Player player){
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                config.trapActive.put(player.getUniqueId(), true);

                if (!player.isOnline() || player.isDead() || tick > 80 || !config.trapActive.getOrDefault(player.getUniqueId(), false)) {
                    config.trapActive.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void hackDash(Player player){

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_Dash_HACK);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 400);
        invulnerable.applyEffect(player);

        detect(player);
        if(config.trapType.getOrDefault(player.getUniqueId(), 1) == 1) {
            hackDashSpike(player, player.getLocation().clone().add(0, 1.2, 0), config.q_skillCount_Hack.getOrDefault(player.getUniqueId(), 0));
        }
        cool.updateCooldown(player, "Q", config.q_Skill_Cool_HACK);
    }

    public void detect(Player player){

        World world = player.getWorld();

        config.q_skillUsing_Hack.put(player.getUniqueId(), true);

        config.q_damaged_Hack.put(player.getUniqueId(), new HashSet<>());

        double damage = config.q_Skill_Damage_HACK;

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        damage = damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);

        double finalDamage = damage;
        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 6 || player.isDead()) {
                    config.q_skillUsing_Hack.remove(player.getUniqueId());
                    config.q_damaged_Hack.remove(player.getUniqueId());

                    if(config.trapType.getOrDefault(player.getUniqueId(), 1) == 2){
                        hackDashThrow(player, config.q_skillCount_Hack.getOrDefault(player.getUniqueId(), 0) * 2);
                    }

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1, 0), 120, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.q_damaged_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        ForceDamage forceDamage = new ForceDamage(target, finalDamage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));

                        config.q_damaged_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void hackDashThrow(Player player, int count) {

        World world = player.getWorld();

        Location origin = player.getLocation().add(0, 1.5, 0);
        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
        BlockData iron = Material.IRON_BLOCK.createBlockData();

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage_Throw_HACK * (1 + amp);

        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        Vector forward = player.getLocation().getDirection().normalize();

        config.q_skillCount_Hack.remove(player.getUniqueId());

        double angleStep = 360.0 / count;

        Particle.DustOptions poisonGreen = new Particle.DustOptions(Color.fromRGB(64, 253, 20), 1.0f);

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(i * angleStep);

            Vector dir = forward.clone();
            double x = dir.getX() * Math.cos(angle) - dir.getZ() * Math.sin(angle);
            double z = dir.getX() * Math.sin(angle) + dir.getZ() * Math.cos(angle);
            dir.setX(x);
            dir.setZ(z);
            dir.normalize().multiply(2.3);

            Item item = world.dropItem(origin, new ItemStack(Material.IRON_NUGGET));
            item.setVelocity(dir);
            item.setPickupDelay(1000);
            item.setGravity(false);

            DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                    .withCausingEntity(player)
                    .withDirectEntity(item)
                    .build();

            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                int life = 80;

                @Override
                public void run() {
                    if (item.isDead() || !item.isValid()) {
                        Bukkit.getScheduler().cancelTask(this.hashCode());
                        return;
                    }

                    Location loc = item.getLocation();

                    world.spawnParticle(Particle.CRIT, loc, 2, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0.05, poisonGreen);

                    for (Entity nearby : item.getNearbyEntities(0.5, 0.5, 0.5)) {
                        if (nearby instanceof LivingEntity target && nearby != player) {

                            ForceDamage forceDamage = new ForceDamage(target, damage, source);
                            forceDamage.applyEffect(player);

                            PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 20 * 4, 5, false, true);
                            target.addPotionEffect(poison);

                            world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1, 1);
                            world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.2, 0),
                                    14, 0.3, 0.3, 0.3, blood);

                            item.remove();
                            Bukkit.getScheduler().cancelTask(this.hashCode());
                            return;
                        }
                    }

                    if (!loc.clone().add(dir.clone().multiply(0.5)).getBlock().isPassable()) {
                        world.playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1, 1);
                        world.spawnParticle(Particle.BLOCK, loc, 14, 0.3, 0.3, 0.3, iron);
                        item.remove();
                        Bukkit.getScheduler().cancelTask(this.hashCode());
                        return;
                    }

                    if (life-- <= 0) {
                        item.remove();
                        Bukkit.getScheduler().cancelTask(this.hashCode());
                    }
                }
            }, 1L, 1L);
        }
    }

    public void hackDashSpike(Player player, Location playerLoc, int count){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        double slashLength = 3.3;
        double maxAngle = Math.toRadians(105);
        double maxTicks = 3;
        double innerRadius = 1.6;

        config.q_damaged_Sweep_Hack.put(player.getUniqueId(), new HashSet<>());
        config.q_skillUsing_Sweep_Hack.put(player.getUniqueId(), true);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage_Spike_HACK * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

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

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(32, 120, 10), 0.6f);

        world.spawnParticle(Particle.SWEEP_ATTACK, playerLoc.clone().add(0, 1.0, 0), 10, 2, 2, 2, 1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead() || !player.isOnline()) {

                    config.q_damaged_Sweep_Hack.remove(player.getUniqueId());

                    int n = count - 1;

                    if(n > 0) {

                        Location lastLoc = player.getLocation().clone().add(0, 1.2, 0);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            hackDashSpike(player, lastLoc.clone(), n);
                        }, 1L);
                    }else{
                        config.q_skillCount_Hack.remove(player.getUniqueId());
                        config.q_skillUsing_Sweep_Hack.remove(player.getUniqueId());
                    }

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= slashLength; length += 0.2) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(3)) {
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
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.4, 0.4, 0.4)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.q_damaged_Sweep_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.3, 0), 20, 0.4, 0.4, 0.4, 1);

                                PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 20 * 4, 5, false, true);
                                target.addPotionEffect(poison);

                                ForceDamage forceDamage = new ForceDamage(target, damage / 2, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.q_damaged_Sweep_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static Block getCustomTargetBlock(Player player, int range) {
        RayTraceResult result = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);

        if (result == null || result.getHitBlock() == null)
            return null;

        Block hitBlock = result.getHitBlock();

        if (hitBlock.getType() == Material.WATER) {
            Block below = hitBlock.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) return below;
            else return null;
        }

        if (hitBlock.getType().isSolid())
            return hitBlock;

        return null;
    }

}
