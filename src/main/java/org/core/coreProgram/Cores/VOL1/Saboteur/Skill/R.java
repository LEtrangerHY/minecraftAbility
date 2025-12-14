package org.core.coreProgram.Cores.VOL1.Saboteur.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.core.Effect.Stun;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL1.Saboteur.coreSystem.Saboteur;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class R implements SkillBase {
    private final Saboteur config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Saboteur config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        if(config.isHackAway.getOrDefault(player.getUniqueId(), false)){
            cool.updateCooldown(player, "R", config.r_Skill_Cool_HACK);
            if(config.trapType.getOrDefault(player.getUniqueId(), 1) == 2) {
                hackTypeThrow(player);
            }else{
                hackTypeSpike(player);
            }
            return;
        }

        if(config.trapType.getOrDefault(player.getUniqueId(), 1) == 2) {
            trapTypeThrow(player);
        }else{
            trapTypeSpike(player);
        }
    }

    public void trapThrowActive(Player player, Location startLoc, LivingEntity target) {
        World world = player.getWorld();

        Vector direction = target.getLocation().clone().add(0, 1.2, 0).toVector()
                .subtract(startLoc.toVector())
                .normalize()
                .multiply(2.4);

        Item shard = world.dropItem(startLoc, new ItemStack(Material.IRON_NUGGET));
        shard.setVelocity(direction);
        shard.setPickupDelay(1000);
        shard.setGravity(false);

        world.playSound(startLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = 6 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(shard)
                .build();

        new BukkitRunnable() {
            int life = 60;

            @Override
            public void run() {
                if (shard.isDead() || !shard.isValid()) {
                    config.trapTarget.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Location loc = shard.getLocation();

                world.spawnParticle(Particle.CRIT, loc, 2, 0, 0, 0, 0.01);
                world.spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0.05, new Particle.DustOptions(Color.GRAY, 1));

                for (Entity nearby : shard.getNearbyEntities(0.6, 0.6, 0.6)) {
                    if (nearby instanceof LivingEntity hit && nearby != player) {

                        world.playSound(loc, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                        world.spawnParticle(Particle.BLOCK, hit.getLocation().add(0, 1.0, 0),
                                14, 0.3, 0.3, 0.3, Material.REDSTONE_BLOCK.createBlockData());

                        PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 260, 5, false, true);
                        hit.addPotionEffect(poison);

                        Stun stun = new Stun(hit, 2000);
                        stun.applyEffect(player);

                        ForceDamage forceDamage = new ForceDamage(hit, damage, source);
                        forceDamage.applyEffect(player);
                        hit.setVelocity(new Vector(0, 0, 0));

                        shard.remove();
                        cancel();
                        return;
                    }
                }

                if (!loc.clone().add(direction.clone().multiply(0.5)).getBlock().isPassable()) {
                    world.playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, loc, 10, 0.3, 0.3, 0.3, Material.IRON_BLOCK.createBlockData());
                    shard.remove();
                    cancel();
                    return;
                }

                if (life-- <= 0) {
                    shard.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void trapTypeThrow(Player player){
        World world = player.getWorld();
        Particle.DustOptions poisonGreen = new Particle.DustOptions(Color.fromRGB(64, 253, 20), 1.0f);

        Block targetBlock = getCustomTargetBlock(player, 24);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            world.playSound(player.getLocation().clone(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            cool.updateCooldown(player, "R", 500L);
            return;
        }

        Location particleLoc = targetBlock.getLocation().add(0.5, 1.5, 0.5);
        config.trapThrowPos.putIfAbsent(player.getUniqueId(), new ArrayList<>());

        List<Location> list = config.trapThrowPos.get(player.getUniqueId());
        if (list.contains(particleLoc)) {
            list.remove(particleLoc);
            player.sendActionBar(Component.text("Trap removed : throw").color(NamedTextColor.YELLOW));
            world.playSound(player.getLocation().clone(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            cool.updateCooldown(player, "R", 500L);
            return;
        }

        player.sendActionBar(Component.text("Trap set! : throw").color(NamedTextColor.GREEN));

        player.playSound(targetBlock.getLocation().clone(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        if(list.size() < 4) {
            list.add(particleLoc);
        }else{
            list.removeFirst();
            list.add(particleLoc);
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !config.trapThrowPos.get(player.getUniqueId()).contains(particleLoc)) {
                    config.trapThrowPos.get(player.getUniqueId()).remove(particleLoc);
                    cancel();
                    return;
                }

                if (config.trapActive.getOrDefault(player.getUniqueId(), false)) {

                    LivingEntity target = config.trapTarget.get(player.getUniqueId());
                    if (target != null && target.isValid() && !target.isDead()) {
                        trapThrowActive(player, particleLoc.clone(), target);
                    }

                    config.trapThrowPos.get(player.getUniqueId()).remove(particleLoc);
                    cancel();
                    return;
                }

                player.spawnParticle(Particle.DUST, particleLoc, 4, 0.2, 0.2, 0.2, 0.08, poisonGreen);
                player.spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 6, 0.3, 0.3, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void trapTypeSpike(Player player){
        World world = player.getWorld();
        Particle.DustOptions poisonGreen = new Particle.DustOptions(Color.fromRGB(64, 253, 20), 1.0f);
        Particle.DustOptions activeFalse = new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.0f);

        Block targetBlock = getCustomTargetBlock(player, 8);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = 0.6 * (1 + amp);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            world.playSound(player.getLocation().clone(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            cool.updateCooldown(player, "R", 500L);
            return;
        }

        Location particleLoc = targetBlock.getLocation().add(0.5, 1.1, 0.5);
        config.trapSpikePos.putIfAbsent(player.getUniqueId(), new ArrayList<>());

        List<Location> list = config.trapSpikePos.get(player.getUniqueId());
        if (list.contains(particleLoc)) {
            list.remove(particleLoc);
            player.sendActionBar(Component.text("Trap removed : spike").color(NamedTextColor.YELLOW));
            world.playSound(player.getLocation().clone(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            cool.updateCooldown(player, "R", 500L);
            return;
        }

        player.sendActionBar(Component.text("Trap set! : spike").color(NamedTextColor.GREEN));

        player.playSound(targetBlock.getLocation().clone(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        if(list.size() < 3) {
            list.add(particleLoc);
        }else{
            list.removeFirst();
            list.add(particleLoc);
        }

        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {

            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !config.trapSpikePos.get(player.getUniqueId()).contains(particleLoc)) {
                    config.trapSpikePos.get(player.getUniqueId()).remove(particleLoc);
                    cancel();
                    return;
                }

                if(tick >= 8) {
                    List<Entity> nearbyEntities = (List<Entity>) particleLoc.getNearbyEntities(3.5, 0.7, 3.5);

                    if (nearbyEntities.isEmpty()) {
                        config.trapSpikeDamage.remove(player.getUniqueId());
                    } else {
                        config.trapSpikeDamage.put(player.getUniqueId(), true);
                    }

                    if(!config.trapSpikeDamage.containsKey(player.getUniqueId())) {
                        player.spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 30, 1.5, 0, 1.5, 0);
                        if(config.trapActive.getOrDefault(player.getUniqueId(), false)){
                            player.spawnParticle(Particle.DUST, particleLoc, 10, 1.5, 0, 1.5, 0.08, poisonGreen);
                        }
                    }else{
                        world.spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 30, 1.5, 0, 1.5, 0);
                        if(config.trapActive.getOrDefault(player.getUniqueId(), false)){
                            world.spawnParticle(Particle.DUST, particleLoc, 10, 1.5, 0, 1.5, 0.08, poisonGreen);
                        }
                    }

                    player.spawnParticle(Particle.DUST, particleLoc, 4, 0.2, 0.2, 0.2, 0.08, poisonGreen);

                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof LivingEntity target && entity != player) {

                            if (tick % 10 == 0) {
                                world.playSound(target.getLocation().clone(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);

                                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 0.1, 0), 13, 0.2, 0.2, 0.2,
                                        blood);

                                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 10, 2, false, false);
                                target.addPotionEffect(slowness);

                                if(config.trapActive.getOrDefault(player.getUniqueId(), false)){
                                    world.playSound(target.getLocation().clone(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1.0f, 1.0f);
                                    PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 260, 5, false, true);
                                    target.addPotionEffect(poison);
                                }

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));
                            }
                        }
                    }
                }else{
                    player.spawnParticle(Particle.DUST, particleLoc, 4, 0.2, 0.2, 0.2, 0.08, activeFalse);
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void hackTypeThrow(Player player){

        World world = player.getWorld();

        player.swingMainHand();

        Particle.DustOptions poisonGreen = new Particle.DustOptions(Color.fromRGB(64, 253, 20), 1.0f);
        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
        BlockData iron = Material.IRON_BLOCK.createBlockData();

        Location playerLocation = player.getLocation().add(0, 1.5, 0);
        Vector direction = playerLocation.getDirection().normalize().multiply(2.3);

        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        Item item = world.dropItem(playerLocation, new ItemStack(Material.IRON_NUGGET));
        item.setVelocity(direction);
        item.setPickupDelay(1000);
        item.setGravity(false);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage_Throw_HACK * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(item)
                .build();

        int n = config.q_skillCount_Hack.getOrDefault(player.getUniqueId(), 0);
        if(n < 4) {
            config.q_skillCount_Hack.put(player.getUniqueId(), n + 1);
        }

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
                        world.playSound(target.getLocation().clone(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                        world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 14, 0.3, 0.3, 0.3,
                                blood);

                        item.remove();
                        Bukkit.getScheduler().cancelTask(this.hashCode());
                        return;
                    }
                }

                if (!loc.clone().add(direction).getBlock().isPassable()) {
                    world.playSound(loc.clone(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK,loc.clone(), 14, 0.3, 0.3, 0.3,
                            iron);
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

    public void hackTypeSpike(Player player){

        World world = player.getWorld();

        player.swingMainHand();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        double slashLength = 3.;
        double maxAngle = Math.toRadians(45);
        double maxTicks = 4;
        double innerRadius = 1.6;

        config.r_damaged_Sweep_Hack.put(player.getUniqueId(), new HashSet<>());
        config.r_skillUsing_Sweep_Hack.put(player.getUniqueId(), true);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage_Spike_HACK * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Random rand = new Random();
        int randomTilt = rand.nextInt(6);

        double tiltAngle = switch (randomTilt) {
            case 0 -> Math.toRadians(6);
            case 1 -> Math.toRadians(-6);
            case 2 -> Math.toRadians(9);
            case 3 -> Math.toRadians(-9);
            case 4 -> Math.toRadians(12);
            case 5 -> Math.toRadians(-12);
            default -> Math.toRadians(0);
        };

        boolean tiltPosZ = Math.random() > 0.5;

        Location playerLoc = player.getLocation().clone().add(0, 1.2, 0);

        Vector direction = playerLoc.getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(64, 253, 20), 0.6f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead() || !player.isOnline()) {

                    int n = config.q_skillCount_Hack.getOrDefault(player.getUniqueId(), 0);
                    if(n < 4) {
                        config.q_skillCount_Hack.put(player.getUniqueId(), n + 1);
                    }

                    config.r_damaged_Sweep_Hack.remove(player.getUniqueId());
                    config.r_skillUsing_Sweep_Hack.remove(player.getUniqueId());

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
                            if (entity instanceof LivingEntity target && entity != player && !config.r_damaged_Sweep_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.3, 0), 20, 0.4, 0.4, 0.4, 1);

                                PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 20 * 4, 5, false, true);
                                target.addPotionEffect(poison);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.r_damaged_Sweep_Hack.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
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
