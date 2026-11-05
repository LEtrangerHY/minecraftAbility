package org.core.coreSystem.cores.VOL3.Knight.Skill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Knight.Passive.bladeBond;
import org.core.coreSystem.cores.VOL3.Knight.coreSystem.Knight;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.models.coresObj.knightSwordModel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bladeBond blades;
    private final NamespacedKey keyQ;
    private final NamespacedKey slowKey;

    private final Map<UUID, BukkitTask> parrying = new HashMap<>();

    public Q(Knight config, JavaPlugin plugin, Cool cool, bladeBond blades) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blades = blades;
        this.keyQ = new NamespacedKey(plugin, "Q");
        this.slowKey = new NamespacedKey(plugin, "knight_parry_slow");
    }

    @Override
    public void Trigger(Player player) {
        if (isParrying(player)) {
            fan(player);
        } else {
            parry(player);
        }
    }

    public boolean isParrying(Player player) {
        return parrying.containsKey(player.getUniqueId());
    }

    public void cleanup(Player player) {
        end(player.getUniqueId());
    }

    private double amplify(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        return 1 + config.q_Skill_amp * level;
    }

    private DamageSource source(Player player) {
        return DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    private Material mainSword(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (bladeBond.isSword(main)) return main.getType();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (bladeBond.isSword(off)) return off.getType();
        return Material.IRON_SWORD;
    }

    private void parry(Player player) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        cool.updateCooldown(player, "Q", config.q_Reuse_Delay);
        slow(player, true);

        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.2f);
        world.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.6f);

        long duration = Math.max(1L, config.q_Parry_Duration / 50L);

        BukkitTask task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                Player owner = Bukkit.getPlayer(uuid);
                if (owner == null || owner.isDead()) {
                    end(uuid);
                    return;
                }

                if (ticks >= duration) {
                    end(uuid);
                    cool.updateCooldown(owner, "Q", 0L);
                    owner.playSound(owner.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1f);
                    return;
                }

                if (ticks % 4 == 0) stance(owner);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        parrying.put(uuid, task);
    }

    private void end(UUID uuid) {
        BukkitTask task = parrying.remove(uuid);
        if (task != null) task.cancel();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) slow(player, false);
    }

    private void slow(Player player, boolean apply) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) return;

        if (speed.getModifier(slowKey) != null) speed.removeModifier(slowKey);
        if (apply) {
            speed.addTransientModifier(new AttributeModifier(slowKey, -config.q_Parry_Slow,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
        }
    }

    private void stance(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation().add(0, 1.0, 0);
        Vector forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 1.0E-4) forward = new Vector(0, 0, 1);
        forward.normalize();

        for (int i = -3; i <= 3; i++) {
            Vector arc = forward.clone().rotateAroundY(Math.toRadians(i * 15));
            Location point = base.clone().add(arc.multiply(0.9));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0.05, 0, 0, i == 0 ? bladeBond.WHITE : bladeBond.GRAY);
        }
    }

    private void fan(Player player) {
        end(player.getUniqueId());
        cool.updateCooldown(player, "Q", config.q_Fan_Cool);

        boolean full = blades.count(player) >= config.blade_Max;
        int count = full ? config.q_Fan_Full_Count : config.q_Fan_Count;
        if (full) blades.recallOldest(player, count);

        World world = player.getWorld();
        Material material = mainSword(player);
        double damage = bladeBond.swordDamage(material) * config.blade_Throw_Ratio * amplify(player);
        DamageSource source = source(player);
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        double half = config.q_Fan_Angle / 2.0;

        swingEchoGuard.swingMainHand(player);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.1f);

        for (int i = 0; i < count; i++) {
            double offset = count == 1 ? 0 : -half + config.q_Fan_Angle * i / (count - 1);
            Vector direction = look.clone().rotateAroundY(Math.toRadians(offset));
            blades.launch(player, material, eye, direction, config.q_Fan_Speed, config.q_Fan_Range,
                    damage, source, true, null, null);
        }

        bladeBond.wear(player, EquipmentSlot.HAND, 1);
    }

    public void onParry(Player player, LivingEntity attacker) {
        end(player.getUniqueId());
        cool.updateCooldown(player, "Q", config.q_Parry_Cool);

        World world = player.getWorld();
        Location chest = player.getLocation().add(0, 1.1, 0);
        world.playSound(chest, Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
        world.playSound(chest, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
        world.playSound(chest, Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);
        world.spawnParticle(Particle.SWEEP_ATTACK, chest, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.ENCHANTED_HIT, chest, 16, 0.4, 0.4, 0.4, 0.3);

        Material material = mainSword(player);
        double damage = bladeBond.swordDamage(material) * config.blade_Throw_Ratio * amplify(player);

        swingEchoGuard.swingMainHand(player);
        homing(player, attacker, material, damage);
        bladeBond.wear(player, EquipmentSlot.HAND, 1);

        if (attacker.getWorld().equals(world)
                && attacker.getLocation().distanceSquared(player.getLocation()) <= config.q_Parry_Push_Range * config.q_Parry_Push_Range) {
            repel(player, attacker);
        }
    }

    private void repel(Player player, LivingEntity target) {
        Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
        if (push.lengthSquared() < 1.0E-4) push = player.getLocation().getDirection().setY(0);
        if (push.lengthSquared() < 1.0E-4) push = new Vector(0, 0, 1);
        push.normalize().multiply(config.q_Parry_Push).setY(config.q_Parry_Push_Lift);
        target.setVelocity(push);

        int epoch = blades.epoch(player);
        long delay = Math.max(1L, config.q_Parry_Stun_Delay / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!blades.isCurrent(player, epoch) || !player.isOnline() || target.isDead() || !target.isValid()) return;
            new Stun(target, config.q_Parry_Stun).applyEffect(player);
        }, delay);
    }

    private void homing(Player player, LivingEntity target, Material material, double damage) {
        World world = player.getWorld();
        int epoch = blades.epoch(player);
        Location start = player.getEyeLocation();

        Vector toward = target.getBoundingBox().getCenter().subtract(start.toVector());
        final Vector initial = toward.lengthSquared() < 1.0E-4 ? start.getDirection().normalize() : toward.normalize();

        Location spawn = start.clone().add(initial.clone().multiply(0.6));
        spawn.setDirection(initial);
        knightSwordModel model = new knightSwordModel(world, spawn, material, bladeBond.SCALE, false, false);
        DamageSource source = source(player);
        long duration = Math.max(1L, config.q_Homing_Duration / 50L);

        world.playSound(start, Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);

        new BukkitRunnable() {
            long ticks = 0;
            final Location cursor = spawn.clone();

            @Override
            public void run() {
                if (!blades.isCurrent(player, epoch) || !player.isOnline() || !model.isValid()
                        || target.isDead() || !target.isValid() || !target.getWorld().equals(world) || ticks++ >= duration) {
                    model.remove();
                    cancel();
                    return;
                }

                Vector aim = target.getBoundingBox().getCenter();
                Vector to = aim.clone().subtract(cursor.toVector());
                double distance = to.length();
                Vector facing = distance > 1.0E-4 ? to.clone().normalize() : initial.clone();

                if (distance <= config.q_Homing_Speed + 0.4) {
                    cancel();
                    RayTraceResult surface = target.getBoundingBox().rayTrace(cursor.toVector(), facing, distance + 1.0);
                    Vector point = surface != null ? surface.getHitPosition() : aim;
                    Location at = point.toLocation(world);

                    new ForceDamage(target, damage, source, false).applyEffect(player);
                    world.spawnParticle(Particle.ENCHANTED_HIT, at, 14, 0.2, 0.2, 0.2, 0.2);
                    world.playSound(at, Sound.ITEM_TRIDENT_HIT, 1f, 0.9f);

                    blades.embed(player, material, false, target, point, facing, model);
                    return;
                }

                Location previous = cursor.clone();
                cursor.add(facing.clone().multiply(config.q_Homing_Speed));
                Location tip = cursor.clone();
                tip.setDirection(facing);
                model.getDisplay().teleport(tip);
                bladeBond.streak(world, previous, facing, config.q_Homing_Speed);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}