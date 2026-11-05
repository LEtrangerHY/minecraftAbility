package org.core.coreSystem.cores.VOL6.Adam.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Adam.Passive.stackCharge;
import org.core.coreSystem.cores.VOL6.Adam.coreSystem.Adam;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.models.coresObj.adamSwordModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class F implements SkillBase {

    private static final BlockData CHAIN_BLOCK = Material.IRON_CHAIN.createBlockData();
    private static final Particle.DustOptions AURA_BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.7f);
    private static final Particle.DustOptions AURA_WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);
    private static final double GRIP_HEIGHT = 1.3;
    private static final double GRIP_FORWARD = 0.9;
    private static final double GRIP_SIDE = 0.4;
    private static final float SWORD_BREADTH = 1.0f;
    private static final float SWORD_THICKNESS = 0.4f;
    private static final double WAKE = Math.toRadians(50);

    private final Adam config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final stackCharge charge;
    private final NamespacedKey keyF;

    private final Map<UUID, BukkitTask> casting = new HashMap<>();
    private final Map<UUID, adamSwordModel> swords = new HashMap<>();
    private final Set<UUID> bracing = new HashSet<>();

    public F(Adam config, JavaPlugin plugin, Cool cool, stackCharge charge) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.charge = charge;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        int stacks = charge.count(player);
        if (stacks >= config.f_Auto_Stack || (stacks > 0 && charge.isReady(player))) {
            strike(player);
        } else {
            sweep(player);
        }
    }

    public void cleanup(Player player) {
        finish(player.getUniqueId());
    }

    public boolean isBracing(Player player) {
        return bracing.contains(player.getUniqueId());
    }

    private double amplify(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        return 1 + config.f_Skill_amp * level;
    }

    private DamageSource source(Player player) {
        return DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private double deal(Player player, LivingEntity target, double damage, DamageSource source, boolean noKnock) {
        double before = target.getHealth() + target.getAbsorptionAmount();
        new ForceDamage(target, damage, source, noKnock).applyEffect(player);
        double after = target.isDead() ? 0.0 : target.getHealth() + target.getAbsorptionAmount();
        return Math.max(0.0, before - after);
    }

    private double heal(Player player, double dealt, double healed) {
        if (dealt <= 0 || player.isDead()) return healed;

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        double max = attribute != null ? attribute.getValue() : 20.0;
        double amount = Math.min(dealt * config.f_Heal_Ratio, max * config.f_Heal_Cap - healed);
        if (amount <= 0) return healed;

        player.setHealth(Math.min(max, player.getHealth() + amount));
        return healed + amount;
    }

    private static Particle.DustOptions shade(double depth, float size) {
        double clamped = Math.max(0.0, Math.min(1.0, depth));
        int value = (int) Math.round(135 * (1.0 - clamped));
        return new Particle.DustOptions(Color.fromRGB(value, value, value), size);
    }

    private void sweep(Player player) {
        swingEchoGuard.swingMainHand(player);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.8;
        double maxAngle = Math.toRadians(45);
        int maxTicks = 3;
        double innerRadius = 2.6;
        double hitThreshold = Math.cos(maxAngle + 0.2);

        Set<Entity> hitEntities = new HashSet<>();
        double damage = config.f_Skill_Damage * amplify(player);
        DamageSource source = source(player);

        Location origin = player.getEyeLocation().add(0, -0.6, 0);
        Vector forward = player.getLocation().getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        new BukkitRunnable() {
            int ticks = 0;
            int gained = 0;
            double healed = 0;

            @Override
            public void run() {
                if (ticks < 2) world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);

                if (ticks > maxTicks || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                double progressAngle = -(((double) ticks / maxTicks) * (maxAngle * 2) - maxAngle);
                Vector current = forward.clone().rotateAroundAxis(up, progressAngle);

                for (Entity entity : world.getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player || hitEntities.contains(entity)) continue;

                    Vector toEntity = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector().subtract(origin.toVector());
                    if (toEntity.lengthSquared() > (slashLength + 1.0) * (slashLength + 1.0)) continue;

                    if (current.dot(toEntity.normalize()) > hitThreshold) {
                        hitEntities.add(entity);
                        healed = heal(player, deal(player, target, damage, source, true), healed);

                        if (gained < config.f_Stack_Per_Swing) {
                            gained++;
                            charge.add(player, 1);
                        }
                    }
                }

                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (double angle = maxAngle; angle >= -maxAngle; angle -= Math.toRadians(2)) {
                    double sin = Math.sin(angle + progressAngle);
                    double cos = Math.cos(angle + progressAngle);

                    double dirX = right.getX() * sin + forward.getX() * cos;
                    double dirY = right.getY() * sin + forward.getY() * cos;
                    double dirZ = right.getZ() * sin + forward.getZ() * cos;

                    for (double length = innerRadius; length <= slashLength; length += 0.1) {
                        double ratio = (length - innerRadius) / (slashLength - innerRadius);
                        Particle.DustOptions dust = shade(0.2 + 0.85 * ratio + random.nextDouble(-0.1, 0.1), 0.5f);
                        world.spawnParticle(Particle.DUST, origin.clone().add(dirX * length, dirY * length, dirZ * length), 1, 0, 0, 0, 0, dust);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Material swordOf(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return main.getType().name().endsWith("_SWORD") ? main.getType() : Material.IRON_SWORD;
    }

    private Location grip(Player player, float yaw, float pitch) {
        double rad = Math.toRadians(yaw);
        Location grip = player.getLocation().add(0, GRIP_HEIGHT, 0)
                .add(horizontal(yaw).multiply(GRIP_FORWARD))
                .add(new Vector(-Math.cos(rad), 0, -Math.sin(rad)).multiply(GRIP_SIDE));
        grip.setYaw(yaw);
        grip.setPitch(pitch);
        return grip;
    }

    private static Vector horizontal(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vector(-Math.sin(rad), 0, Math.cos(rad));
    }

    private void strike(Player player) {
        UUID uuid = player.getUniqueId();
        double bonus = 1 + config.f_Charge_Bonus * charge.progress(player);
        int stacks = charge.consume(player);
        charge.stop(player);
        if (stacks <= 0) {
            sweep(player);
            return;
        }
        finish(uuid);

        World world = player.getWorld();
        double t = config.stack_Max <= 1 ? 1.0 : Math.min(1.0, (stacks - 1) / (double) (config.stack_Max - 1));
        double radius = lerp(config.f_Strong_Radius_Min, config.f_Strong_Radius_Max, t);
        double inner = radius * config.f_Strong_Inner;
        double width = lerp(config.f_Strong_Width_Min, config.f_Strong_Width_Max, t);
        double ring = lerp(config.f_Strong_Ring_Min, config.f_Strong_Ring_Max, t);
        float size = (float) lerp(0.6, 1.0, t);
        float scale = (float) (radius / adamSwordModel.length(1.0f));
        double damage = config.f_Strong_Damage_Per_Stack * stacks * amplify(player) * bonus;
        DamageSource source = source(player);

        int windup = Math.max(1, (int) Math.round(lerp(config.f_Strong_Windup, config.f_Strong_Windup_Max, t)));
        int swing = Math.max(1, (int) Math.round(lerp(config.f_Strong_Ticks, config.f_Strong_Ticks_Min, t)));
        int linger = Math.max(0, (int) Math.round(lerp(config.f_Strong_Linger, config.f_Strong_Linger_Max, t)));
        float weight = (float) t;
        double end = -Math.asin(Math.min(1.0, GRIP_HEIGHT / radius));
        float finalPitch = (float) -Math.toDegrees(end);
        Set<UUID> struck = new HashSet<>();
        double[] healed = {0.0};

        float startYaw = player.getLocation().getYaw();
        adamSwordModel sword = new adamSwordModel(world, grip(player, startYaw, -90f), swordOf(player), scale * 0.35f, SWORD_BREADTH, SWORD_THICKNESS);
        swords.put(uuid, sword);
        bracing.add(uuid);

        long rooted = Math.round((windup + swing) * 50L * t);
        if (rooted > 0) new Stiff(player, rooted).applyEffect(player);

        world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.6f);
        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, (float) lerp(0.8, 0.7, t));
        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.3f + 0.7f * weight, 0.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f * weight, 0.6f);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            float yaw = startYaw;
            double angle = Math.PI / 2;
            Vector forward = null;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !sword.isValid()) {
                    finish(uuid);
                    return;
                }

                if (ticks < windup) {
                    if (ticks == 1) sword.resize(scale, Math.max(1, windup - 1));
                    yaw = player.getLocation().getYaw();
                    Location grip = grip(player, yaw, -90f);
                    sword.getDisplay().teleport(grip);
                    world.spawnParticle(Particle.BLOCK, grip.clone().add(0, adamSwordModel.length(scale) * (ticks + 1) / windup * 0.5, 0),
                            3, 0.25, 0.4, 0.25, 0, CHAIN_BLOCK);
                    aura(player, ticks, windup, scale, weight);
                } else if (ticks < windup + swing) {
                    if (forward == null) {
                        forward = horizontal(yaw);
                        swingEchoGuard.swingMainHand(player);
                        world.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 0.6f + 0.4f * weight, (float) lerp(0.8, 0.6, t));
                        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                    }

                    double progress = (double) (ticks - windup + 1) / swing;
                    double next = Math.PI / 2 + (end - Math.PI / 2) * progress;
                    Location grip = grip(player, yaw, (float) -Math.toDegrees(next));
                    sword.getDisplay().teleport(grip);

                    wake(world, grip, forward, next, inner, radius, size);
                    strikeSlice(player, world, grip, forward, angle, next, radius, width, damage, source, struck, healed, weight);
                    angle = next;
                } else if (ticks == windup + swing) {
                    bracing.remove(uuid);
                    if (forward == null) forward = horizontal(yaw);
                    Location grip = grip(player, yaw, finalPitch);
                    impact(player, world, grip, forward, radius, end, ring, damage, source, struck, healed, size, weight);
                } else if (ticks >= windup + swing + linger) {
                    finish(uuid);
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        casting.put(uuid, task);
    }

    private void aura(Player player, int ticks, int windup, float scale, float weight) {
        World world = player.getWorld();
        Location base = player.getLocation().add(0, GRIP_HEIGHT, 0);
        double height = adamSwordModel.length(scale) * Math.min(1.0, (ticks + 1) / (double) windup);
        int strands = 1 + Math.round(2 * weight);

        for (int i = 0; i < strands; i++) {
            double angle = ticks * 0.6 + i * 2 * Math.PI / strands;
            double y = (ticks * 0.45 + i * height / strands) % Math.max(0.5, height);
            Location point = base.clone().add(0.9 * Math.cos(angle), y, 0.9 * Math.sin(angle));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, Math.random() < 0.17 ? AURA_WHITE : AURA_BLACK);
        }
        if (weight >= 0.4f && ticks % 3 == 0) {
            world.spawnParticle(Particle.SQUID_INK, base.clone().add(0, height, 0), 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void finish(UUID uuid) {
        bracing.remove(uuid);
        BukkitTask task = casting.remove(uuid);
        if (task != null) task.cancel();

        adamSwordModel sword = swords.remove(uuid);
        if (sword != null) sword.remove();
    }

    private void wake(World world, Location grip, Vector forward, double current, double inner, double radius, float size) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double ground = grip.getY() - GRIP_HEIGHT;
        double tail = Math.min(Math.PI / 2, current + WAKE);
        int steps = Math.max(4, (int) Math.ceil((tail - current) * radius / 0.2));

        for (int i = 0; i <= steps; i++) {
            double angle = current + (tail - current) * i / steps;
            double age = (angle - current) / WAKE;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            for (double r = inner; r <= radius + 1.0E-6; r += 0.18) {
                double ratio = (r - inner) / Math.max(1.0E-6, radius - inner);
                double reach = r + random.nextDouble(-0.05, 0.05);
                Location point = grip.clone().add(forward.clone().multiply(cos * reach)).add(0, sin * reach, 0);
                if (point.getY() < ground) continue;

                boolean edge = ratio > 0.9;
                double depth = edge ? 1.0 : (0.15 + 0.9 * ratio) * (1.0 - 0.5 * age) + random.nextDouble(-0.08, 0.08);
                float particle = (float) (size * (edge ? 1.25 : 0.7 + 0.4 * ratio));
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, shade(depth, particle));
            }
        }
    }

    private void strikeSlice(Player player, World world, Location grip, Vector forward, double from, double to,
                             double radius, double width, double damage, DamageSource source, Set<UUID> struck, double[] healed, float weight) {
        Vector side = new Vector(-forward.getZ(), 0, forward.getX());
        double low = Math.min(from, to) - 0.12;
        double high = Math.max(from, to) + 0.12;
        double reach = radius + 1.5;
        Vector origin = grip.toVector();

        for (Entity entity : world.getNearbyEntities(grip, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity target) || entity.equals(player) || target.isDead()
                    || struck.contains(entity.getUniqueId())) continue;

            double half = entity.getWidth() / 2.0;
            double height = entity.getHeight();
            boolean hit = false;

            for (int k = 0; k <= 2 && !hit; k++) {
                Vector relative = entity.getLocation().toVector().add(new Vector(0, height * k / 2.0, 0)).subtract(origin);
                if (Math.abs(relative.dot(side)) > width + half) continue;

                double along = relative.dot(forward);
                double rise = relative.getY();
                if (Math.sqrt(along * along + rise * rise) > radius + half + 0.3) continue;

                double angle = Math.atan2(rise, along);
                if (angle >= low && angle <= high) hit = true;
            }

            if (!hit) continue;

            struck.add(entity.getUniqueId());
            healed[0] = heal(player, deal(player, target, damage, source, false), healed[0]);
            new Stun(target, config.f_Strong_Stun).applyEffect(player);
            world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, height * 0.6, 0), 12, 0.3, 0.3, 0.3, 0.2);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, height * 0.65, 0), 6 + Math.round(14 * weight), 0.4, 0.4, 0.4, 1);
            world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f + 0.5f * weight, 0.8f);
        }
    }

    private void impact(Player player, World world, Location grip, Vector forward, double radius, double end, double ring,
                        double damage, DamageSource source, Set<UUID> struck, double[] healed, float size, float weight) {
        Location point = grip.clone()
                .add(forward.clone().multiply(Math.cos(end) * radius))
                .add(0, Math.sin(end) * radius, 0);

        Block ground = point.clone().add(0, -0.2, 0).getBlock();
        BlockData data = ground.getType().isSolid() ? ground.getBlockData() : Material.DIRT.createBlockData();

        world.playSound(point, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.4f + 0.6f * weight, 0.7f);
        world.playSound(point, Sound.ENTITY_GENERIC_EXPLODE, 0.4f + 0.3f * weight, (float) lerp(1.2, 0.6, weight));
        world.playSound(point, Sound.BLOCK_CHAIN_BREAK, 1.6f, 0.8f);
        if (weight > 0) world.playSound(point, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.5f * weight, 0.7f);
        world.spawnParticle(Particle.EXPLOSION, point.clone().add(0, 0.3, 0), 1 + Math.round(weight), ring * 0.15, 0.1, ring * 0.15, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, point.clone().add(0, 0.3, 0), 6 + Math.round(14 * weight), ring * 0.3, 0.3, ring * 0.3, 0.02);
        world.spawnParticle(Particle.SQUID_INK, point.clone().add(0, 0.3, 0), 2 + Math.round(6 * weight), ring * 0.3, 0.1, ring * 0.3, 0.05);
        if (ground.getType().isSolid()) {
            world.spawnParticle(Particle.DUST_PILLAR, point, 10 + Math.round(30 * weight), ring * 0.35, 0.2, ring * 0.35, 0.2, data);
        }

        int points = Math.max(24, (int) (ring * 28));
        double[] scales = {0.55, 0.8, 1.0};
        for (int s = 0; s < scales.length; s++) {
            double r = ring * scales[s];
            Particle.DustOptions dust = shade(0.45 + 0.275 * s, size * (0.8f + 0.2f * s));
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2 * i / points;
                world.spawnParticle(Particle.DUST, point.clone().add(Math.cos(angle) * r, 0.12, Math.sin(angle) * r), 1, 0, 0, 0, 0, dust);
            }
        }
        world.spawnParticle(Particle.BLOCK, point.clone().add(0, 0.4, 0), (int) (30 + ring * 20), ring * 0.45, 0.3, ring * 0.45, 0, data);
        world.spawnParticle(Particle.BLOCK, point.clone().add(0, 0.6, 0), 14, 0.4, 0.4, 0.4, 0, CHAIN_BLOCK);

        for (Entity entity : world.getNearbyEntities(point, ring + 1.0, 2.5, ring + 1.0)) {
            if (!(entity instanceof LivingEntity target) || entity.equals(player) || target.isDead()
                    || struck.contains(entity.getUniqueId())) continue;

            Location at = entity.getLocation();
            double dx = at.getX() - point.getX();
            double dz = at.getZ() - point.getZ();
            double limit = ring + entity.getWidth() / 2.0;
            if (dx * dx + dz * dz > limit * limit || Math.abs(at.getY() - point.getY()) > 2.5) continue;

            struck.add(entity.getUniqueId());
            healed[0] = heal(player, deal(player, target, damage, source, false), healed[0]);
            new Stun(target, config.f_Strong_Stun).applyEffect(player);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, at.clone().add(0, 1.3, 0), 6 + Math.round(14 * weight), 0.4, 0.4, 0.4, 1);
            world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f + 0.5f * weight, 0.8f);
        }
    }
}