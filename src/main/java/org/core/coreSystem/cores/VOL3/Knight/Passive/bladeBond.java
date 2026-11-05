package org.core.coreSystem.cores.VOL3.Knight.Passive;

import com.google.common.collect.Multimap;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL3.Knight.coreSystem.Knight;
import org.core.effect.crowdControl.ForceDamage;
import org.core.models.coresObj.knightSwordModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class bladeBond {

    public static final float SCALE = 0.9f;
    public static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.7f);
    public static final Particle.DustOptions GRAY = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
    public static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);

    private static final double EXPOSED = 0.45;

    private final Knight config;
    private final JavaPlugin plugin;
    private final Map<UUID, LinkedList<Blade>> blades = new HashMap<>();
    private final Map<UUID, Integer> epochs = new HashMap<>();

    public bladeBond(Knight config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public static class Blade {
        private final Material material;
        private final boolean origin;
        private final long embeddedAt;
        private final knightSwordModel model;
        private LivingEntity host;
        private Vector offset;
        private Vector facing;
        private Location point;
        private Vector velocity;
        private int airborne;

        private Blade(Material material, boolean origin, knightSwordModel model) {
            this.material = material;
            this.origin = origin;
            this.model = model;
            this.embeddedAt = System.currentTimeMillis();
        }

        public Material getMaterial() {
            return material;
        }

        public boolean isOrigin() {
            return origin;
        }

        public LivingEntity getHost() {
            return host;
        }

        public Vector getFacing() {
            if (host == null) return facing.clone();
            return facing.clone().rotateAroundY(-Math.toRadians(host.getBodyYaw()));
        }

        public Location getPoint() {
            if (host == null) return point.clone();
            Vector rotated = offset.clone().rotateAroundY(-Math.toRadians(host.getBodyYaw()));
            return host.getLocation().add(rotated);
        }

        public Location getExposed() {
            return getPoint().subtract(getFacing().multiply(EXPOSED));
        }

        public Location getCenter() {
            if (host != null) return host.getBoundingBox().getCenter().toLocation(host.getWorld());
            return point.clone().subtract(facing.clone().multiply(0.5));
        }
    }

    public int epoch(Player owner) {
        return epochs.getOrDefault(owner.getUniqueId(), 0);
    }

    public boolean isCurrent(Player owner, int epoch) {
        return epoch(owner) == epoch;
    }

    public List<Blade> list(Player owner) {
        LinkedList<Blade> list = blades.get(owner.getUniqueId());
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public int count(Player owner) {
        LinkedList<Blade> list = blades.get(owner.getUniqueId());
        return list == null ? 0 : list.size();
    }

    public List<Blade> hostedBy(Player owner, Entity entity) {
        List<Blade> result = new ArrayList<>();
        for (Blade blade : list(owner)) {
            if (blade.host != null && blade.host.equals(entity)) result.add(blade);
        }
        return result;
    }

    public List<Blade> near(Player owner, Location location, double radius) {
        List<Blade> result = new ArrayList<>();
        for (Blade blade : list(owner)) {
            Location exposed = blade.getExposed();
            if (!exposed.getWorld().equals(location.getWorld())) continue;
            if (exposed.distanceSquared(location) <= radius * radius) result.add(blade);
        }
        return result;
    }

    public Blade embed(Player owner, Material material, boolean origin, LivingEntity host, Vector hit, Vector direction, knightSwordModel model) {
        World world = host.getWorld();
        double yaw = Math.toRadians(host.getBodyYaw());
        knightSwordModel sword = model != null ? model : new knightSwordModel(world, hit.toLocation(world), material, SCALE, false, false);

        Blade blade = new Blade(material, origin, sword);
        blade.host = host;
        blade.offset = hit.clone().subtract(host.getLocation().toVector()).rotateAroundY(yaw);
        blade.facing = direction.clone().normalize().rotateAroundY(yaw);

        place(blade);
        add(owner, blade);
        return blade;
    }

    public Blade embed(Player owner, Material material, boolean origin, Location hit, Vector direction, knightSwordModel model) {
        knightSwordModel sword = model != null ? model : new knightSwordModel(hit.getWorld(), hit, material, SCALE, false, false);

        Blade blade = new Blade(material, origin, sword);
        blade.point = hit.clone();
        blade.facing = direction.clone().normalize();

        place(blade);
        add(owner, blade);
        return blade;
    }

    private void place(Blade blade) {
        double sink = blade.host != null ? config.blade_Sink_Entity : config.blade_Sink_Block;
        Location tip = blade.getPoint().add(blade.getFacing().multiply(sink));
        tip.setDirection(blade.getFacing());
        blade.model.getDisplay().teleport(tip);
    }

    private void add(Player owner, Blade blade) {
        LinkedList<Blade> list = blades.computeIfAbsent(owner.getUniqueId(), k -> new LinkedList<>());
        list.addLast(blade);

        while (list.size() > config.blade_Max) {
            Blade oldest = list.removeFirst();
            recallEffect(owner, oldest);
            oldest.model.remove();
        }
    }

    public void recallOldest(Player owner, int amount) {
        LinkedList<Blade> list = blades.get(owner.getUniqueId());
        if (list == null) return;

        for (int i = 0; i < amount && !list.isEmpty(); i++) {
            Blade oldest = list.removeFirst();
            recallEffect(owner, oldest);
            oldest.model.remove();
        }
        if (list.isEmpty()) blades.remove(owner.getUniqueId());
    }

    public void recall(Player owner, Blade blade) {
        if (!detachFromField(owner, blade)) return;
        recallEffect(owner, blade);
        blade.model.remove();
    }

    public void discard(Player owner, Blade blade) {
        detachFromField(owner, blade);
        blade.model.remove();
    }

    public knightSwordModel detach(Player owner, Blade blade) {
        detachFromField(owner, blade);
        return blade.model;
    }

    private boolean detachFromField(Player owner, Blade blade) {
        LinkedList<Blade> list = blades.get(owner.getUniqueId());
        if (list == null) return false;
        boolean removed = list.remove(blade);
        if (list.isEmpty()) blades.remove(owner.getUniqueId());
        return removed;
    }

    public void clear(Player owner) {
        epochs.merge(owner.getUniqueId(), 1, Integer::sum);
        LinkedList<Blade> list = blades.remove(owner.getUniqueId());
        if (list == null) return;
        for (Blade blade : list) blade.model.remove();
    }

    public void heal(Player owner) {
        if (!owner.isOnline() || owner.isDead()) return;
        owner.heal(config.blade_Heal);

        World world = owner.getWorld();
        world.spawnParticle(Particle.DUST, owner.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0, WHITE);
        world.playSound(owner.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.4f);
    }

    private void recallEffect(Player owner, Blade blade) {
        Location at = blade.getExposed();
        at.getWorld().spawnParticle(Particle.ENCHANTED_HIT, at, 10, 0.2, 0.2, 0.2, 0.1);
        heal(owner);
    }

    private void tick() {
        if (blades.isEmpty()) return;
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, LinkedList<Blade>>> iterator = blades.entrySet().iterator();
        while (iterator.hasNext()) {
            LinkedList<Blade> list = iterator.next().getValue();

            Map<LivingEntity, List<Blade>> fallen = new HashMap<>();
            for (Blade blade : list) {
                if (blade.host != null && (blade.host.isDead() || !blade.host.isValid())) {
                    fallen.computeIfAbsent(blade.host, k -> new ArrayList<>()).add(blade);
                }
            }
            for (List<Blade> group : fallen.values()) burst(group);

            list.removeIf(blade -> {
                if (!blade.model.isValid() || now - blade.embeddedAt >= config.blade_Duration) {
                    blade.model.remove();
                    return true;
                }
                if (blade.velocity != null) return !fall(blade);
                if (blade.host != null) place(blade);
                return false;
            });

            if (list.isEmpty()) iterator.remove();
        }
    }

    private void burst(List<Blade> group) {
        LivingEntity host = group.get(0).host;
        Location base = host.getLocation().add(0, host.getHeight() * 0.6, 0);
        World world = base.getWorld();
        int size = group.size();
        double offset = Math.random() * Math.PI * 2;

        world.spawnParticle(Particle.CRIT, base, 14, 0.25, 0.25, 0.25, 0.25);
        world.spawnParticle(Particle.POOF, base, 5, 0.2, 0.2, 0.2, 0.02);
        world.playSound(base, Sound.ITEM_TRIDENT_RETURN, 0.8f, 0.7f);
        world.playSound(base, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.8f);

        for (int i = 0; i < size; i++) {
            Blade blade = group.get(i);
            double angle = offset + 2 * Math.PI * i / size + (Math.random() - 0.5) * 0.5;
            double spread = config.blade_Burst_Speed * (0.75 + Math.random() * 0.5);
            double lift = config.blade_Burst_Lift * (0.85 + Math.random() * 0.3);
            Vector velocity = new Vector(Math.cos(angle) * spread, lift, Math.sin(angle) * spread);

            blade.host = null;
            blade.point = base.clone();
            blade.velocity = velocity;
            blade.facing = velocity.clone().normalize();
            blade.airborne = 0;
        }
    }

    private boolean fall(Blade blade) {
        World world = blade.point.getWorld();
        if (++blade.airborne > 100 || blade.point.getY() < world.getMinHeight()) {
            blade.model.remove();
            return false;
        }

        Vector velocity = blade.velocity;
        double length = velocity.length();
        if (length > 1.0E-4) {
            RayTraceResult hit = world.rayTraceBlocks(blade.point, velocity, length, FluidCollisionMode.NEVER, true);
            if (hit != null) {
                land(blade, hit);
                return true;
            }
            blade.facing = velocity.clone().normalize();
        }

        blade.point.add(velocity);
        velocity.setY(velocity.getY() - config.blade_Gravity);

        Location tip = blade.point.clone();
        tip.setDirection(blade.facing);
        blade.model.getDisplay().teleport(tip);
        world.spawnParticle(Particle.DUST, tip, 1, 0, 0, 0, 0, Math.random() < 0.3 ? WHITE : GRAY);
        return true;
    }

    private void land(Blade blade, RayTraceResult hit) {
        World world = blade.point.getWorld();
        Location point = hit.getHitPosition().toLocation(world);
        Vector facing = blade.velocity.clone().normalize().add(new Vector(0, -0.8, 0)).normalize();

        blade.velocity = null;
        blade.point = point;
        blade.facing = facing;
        place(blade);

        Block block = hit.getHitBlock();
        if (block != null) world.spawnParticle(Particle.BLOCK, point, 8, 0.15, 0.05, 0.15, block.getBlockData());
        world.playSound(point, Sound.ITEM_TRIDENT_HIT_GROUND, 0.8f, 1.1f);
    }

    public void launch(Player owner, Material material, Location start, Vector direction, double speed, double range,
                       double damage, DamageSource source, boolean origin, Consumer<Blade> onEmbed, Runnable onMiss) {
        World world = owner.getWorld();
        int epoch = epoch(owner);
        Vector heading = direction.clone().normalize();

        Location spawn = start.clone().add(heading.clone().multiply(0.6));
        spawn.setDirection(heading);
        knightSwordModel model = new knightSwordModel(world, spawn, material, SCALE, false, false);

        new BukkitRunnable() {
            final Location cursor = start.clone();
            double traveled = 0;

            @Override
            public void run() {
                if (!isCurrent(owner, epoch) || !owner.isOnline() || owner.isDead() || !model.isValid()) {
                    model.remove();
                    cancel();
                    return;
                }

                double step = Math.min(speed, range - traveled);
                if (step <= 0) {
                    model.remove();
                    cancel();
                    if (onMiss != null) onMiss.run();
                    return;
                }

                RayTraceResult hit = world.rayTrace(cursor, heading, step, FluidCollisionMode.NEVER, true, 0.3, e -> isTarget(owner, e));
                if (hit != null) {
                    cancel();
                    Location point = hit.getHitPosition().toLocation(world);
                    Blade blade;

                    if (hit.getHitEntity() instanceof LivingEntity target) {
                        new ForceDamage(target, damage, source, false).applyEffect(owner);
                        world.spawnParticle(Particle.ENCHANTED_HIT, point, 12, 0.2, 0.2, 0.2, 0.2);
                        world.playSound(point, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
                        blade = embed(owner, material, origin, target, hit.getHitPosition(), heading, model);
                    } else {
                        world.playSound(point, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1f);
                        blade = embed(owner, material, origin, point, heading, model);
                    }

                    if (onEmbed != null) onEmbed.accept(blade);
                    return;
                }

                Location previous = cursor.clone();
                cursor.add(heading.clone().multiply(step));
                traveled += step;

                Location tip = cursor.clone();
                tip.setDirection(heading);
                model.getDisplay().teleport(tip);
                streak(world, previous, heading, step);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void streak(World world, Location from, Vector heading, double distance) {
        Vector unit = heading.clone().normalize();
        for (double d = 0; d < distance; d += 0.25) {
            Location point = from.clone().add(unit.clone().multiply(d));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, Math.random() < 0.17 ? WHITE : BLACK);
        }
    }

    public static void trail(World world, Location tip, Vector facing) {
        Location tail = tip.clone().subtract(facing.clone().normalize().multiply(knightSwordModel.length(SCALE)));
        world.spawnParticle(Particle.DUST, tail, 1, 0, 0, 0, 0, Math.random() < 0.17 ? WHITE : BLACK);
    }

    public List<LivingEntity> lineTargets(Player owner, Location from, Location to) {
        List<LivingEntity> result = new ArrayList<>();
        World world = from.getWorld();
        Vector start = from.toVector();
        Vector direction = to.toVector().subtract(start);
        double length = direction.length();

        BoundingBox area = BoundingBox.of(from, to).expand(config.bond_Width + 2.0);
        for (Entity entity : world.getNearbyEntities(area)) {
            if (!isTarget(owner, entity)) continue;

            BoundingBox box = entity.getBoundingBox().expand(config.bond_Width);
            boolean crossed = box.contains(start)
                    || (length > 1.0E-4 && box.rayTrace(start, direction, length) != null);
            if (crossed) result.add((LivingEntity) entity);
        }
        return result;
    }

    private boolean linked(Blade a, Blade b) {
        Location pa = a.getPoint();
        Location pb = b.getPoint();
        return pa.getWorld().equals(pb.getWorld()) && pa.distanceSquared(pb) <= config.bond_Range * config.bond_Range;
    }

    public void bond(Player owner, Blade from, double damage, DamageSource source, Set<UUID> struck) {
        for (Blade other : list(owner)) {
            if (other == from || !linked(from, other)) continue;
            wave(owner, from.getPoint(), other, damage, source, struck);
        }
    }

    private void wave(Player owner, Location from, Blade toward, double damage, DamageSource source, Set<UUID> struck) {
        World world = from.getWorld();
        int epoch = epoch(owner);

        new BukkitRunnable() {
            Location cursor = from.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (!isCurrent(owner, epoch) || !owner.isOnline() || ticks++ > 40) {
                    cancel();
                    return;
                }

                Location end = toward.getPoint();
                if (!end.getWorld().equals(world)) {
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.SWEEP_ATTACK, cursor, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.ENCHANTED_HIT, cursor, 7, 0.3, 0.1, 0.3, 0);

                Vector to = end.toVector().subtract(cursor.toVector());
                double remaining = to.length();
                boolean arrived = remaining <= config.bond_Speed;
                Location next = arrived ? end : cursor.clone().add(to.multiply(config.bond_Speed / remaining));

                for (LivingEntity target : lineTargets(owner, cursor, next)) {
                    if (struck.add(target.getUniqueId())) strike(owner, target, damage, source);
                }

                if (arrived) {
                    world.spawnParticle(Particle.SWEEP_ATTACK, end, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.ENCHANTED_HIT, end, 7, 0.3, 0.1, 0.3, 0);
                    cancel();
                    return;
                }

                cursor = next;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void strike(Player owner, LivingEntity target, double damage, DamageSource source) {
        new ForceDamage(target, damage, source, true).applyEffect(owner);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.1);
    }

    public static boolean isTarget(Player owner, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity.equals(owner)) return false;
        if (living.isDead() || entity instanceof ArmorStand || entity.isInvulnerable()) return false;
        return !(entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR);
    }

    public static boolean isSword(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SWORD");
    }

    public static double swordDamage(Material material) {
        double damage = 1.0;
        Multimap<Attribute, AttributeModifier> defaults = material.getDefaultAttributeModifiers(EquipmentSlot.HAND);
        for (AttributeModifier modifier : defaults.get(Attribute.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) damage += modifier.getAmount();
        }
        return damage;
    }

    public static void wear(Player player, EquipmentSlot slot, int amount) {
        boolean off = slot == EquipmentSlot.OFF_HAND;
        ItemStack item = off ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        if (!isSword(item) || item.getType().getMaxDurability() <= 0) return;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int newDamage = damageable.getDamage() + amount;
        damageable.setDamage(newDamage);
        item.setItemMeta(meta);

        if (newDamage >= item.getType().getMaxDurability()) {
            if (off) player.getInventory().setItemInOffHand(null);
            else player.getInventory().setItemInMainHand(null);
        }
    }
}