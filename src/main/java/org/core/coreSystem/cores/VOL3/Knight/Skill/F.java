package org.core.coreSystem.cores.VOL3.Knight.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Knight.Passive.bladeBond;
import org.core.coreSystem.cores.VOL3.Knight.coreSystem.Knight;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.models.coresObj.knightSwordModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class F implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bladeBond blades;
    private final NamespacedKey keyF;

    private static final double GRIP_HEIGHT = 1.2;
    private static final Particle.DustOptions TRAIL = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.3f);
    private static final Particle.DustOptions TRAIL_SHADE = new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1.1f);
    private static final Particle.DustOptions TRAIL_EDGE = new Particle.DustOptions(Color.fromRGB(235, 235, 235), 0.9f);

    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.NETHER_PORTAL,
            Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.AIR, Material.WATER, Material.LAVA
    );

    private final Map<UUID, BukkitTask> casting = new HashMap<>();
    private final Map<UUID, knightSwordModel> greatswords = new HashMap<>();

    public F(Knight config, JavaPlugin plugin, Cool cool, bladeBond blades) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blades = blades;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    public boolean isCasting(Player player) {
        return casting.containsKey(player.getUniqueId());
    }

    public void cleanup(Player player) {
        finish(player.getUniqueId());
    }

    private DamageSource source(Player player, DamageType type) {
        return DamageSource.builder(type)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        finish(uuid);

        long level = player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double amp = 1 + config.f_Skill_amp * level;

        long windup = Math.max(1L, config.f_Windup / 50L);
        long swing = Math.max(1L, config.f_Swing / 50L);
        long linger = 12L;

        new Stiff(player, config.f_Windup + config.f_Swing).applyEffect(player);
        swingEchoGuard.swingMainHand(player);

        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.7f);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.6f);

        List<bladeBond.Blade> gathered = blades.list(player);
        double total = 0;
        for (bladeBond.Blade blade : gathered) {
            total += bladeBond.swordDamage(blade.getMaterial());
        }

        int count = gathered.size();
        double charge = config.blade_Max <= 1 ? 1.0 : Math.max(0.0, Math.min(1.0, (count - 1) / (double) (config.blade_Max - 1)));
        float scale = (float) (config.f_Scale_Min + (config.f_Scale_Max - config.f_Scale_Min) * charge);
        double length = config.f_Length_Min + (config.f_Length_Max - config.f_Length_Min) * charge;
        double width = config.f_Width_Min + (config.f_Width_Max - config.f_Width_Min) * charge;
        double damage = total / config.f_Damage_Divide * amp;
        long stun = count >= config.f_Stun_From ? Math.min(config.f_Stun_Max, config.f_Stun_Per_Blade * count) : 0L;
        boolean breaking = count >= config.f_Break_From;
        int wear = (int) Math.max(1, Math.round(config.f_Wear * (double) count / config.blade_Max));

        int epoch = blades.epoch(player);
        for (bladeBond.Blade blade : gathered) pull(player, epoch, blade, amp);

        float startYaw = player.getLocation().getYaw();
        knightSwordModel greatsword = new knightSwordModel(world, grip(player, startYaw, -90f),
                Material.NETHERITE_SWORD, 1.0f, false, true);
        greatsword.darken();
        greatswords.put(uuid, greatsword);

        double reach = knightSwordModel.length(scale);
        float finalPitch = (float) Math.toDegrees(Math.asin(Math.min(1.0, GRIP_HEIGHT / reach)));

        BukkitTask task = new BukkitRunnable() {
            long ticks = 0;
            float yaw = startYaw;
            float pitch = -90f;
            Vector forward = null;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !greatsword.isValid()) {
                    finish(uuid);
                    return;
                }

                if (ticks < windup) {
                    if (ticks == 1) greatsword.resize(scale, (int) Math.max(1L, windup - 1));
                    yaw = player.getLocation().getYaw();
                    greatsword.getDisplay().teleport(grip(player, yaw, -90f));
                    aura(player, ticks, windup, scale);
                } else if (ticks < windup + swing) {
                    if (forward == null) {
                        yaw = player.getLocation().getYaw();
                        forward = horizontal(yaw);
                        world.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 1f, 0.6f);
                    }
                    float progress = (float) (ticks - windup + 1) / swing;
                    float next = -90f + (90f + finalPitch) * progress;
                    greatsword.getDisplay().teleport(grip(player, yaw, next));
                    fan(player, yaw, pitch, next, scale);
                    if (breaking) carve(player, yaw, pitch, next, scale, width);
                    pitch = next;
                } else if (ticks == windup + swing) {
                    if (forward == null) forward = horizontal(yaw);
                    impact(player, forward, damage, stun, breaking, length, width, scale, wear);
                } else if (ticks >= windup + swing + linger) {
                    finish(uuid);
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        casting.put(uuid, task);
    }

    private void finish(UUID uuid) {
        BukkitTask task = casting.remove(uuid);
        if (task != null) task.cancel();

        knightSwordModel greatsword = greatswords.remove(uuid);
        if (greatsword != null) greatsword.remove();
    }

    private Location grip(Player player, float yaw, float pitch) {
        Location grip = player.getLocation().add(0, GRIP_HEIGHT, 0).add(horizontal(yaw).multiply(0.3));
        grip.setYaw(yaw);
        grip.setPitch(pitch);
        return grip;
    }

    private static Vector horizontal(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vector(-Math.sin(rad), 0, Math.cos(rad));
    }

    private void aura(Player player, long ticks, long windup, float scale) {
        World world = player.getWorld();
        Location base = player.getLocation().add(0, GRIP_HEIGHT, 0);
        double height = knightSwordModel.length(scale) * Math.min(1.0, (ticks + 1) / (double) windup);

        for (int i = 0; i < 3; i++) {
            double angle = ticks * 0.6 + i * 2 * Math.PI / 3;
            double y = (ticks * 0.45 + i * height / 3) % Math.max(0.5, height);
            Location point = base.clone().add(0.9 * Math.cos(angle), y, 0.9 * Math.sin(angle));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, Math.random() < 0.17 ? bladeBond.WHITE : bladeBond.BLACK);
        }
        if (ticks % 3 == 0) {
            world.spawnParticle(Particle.SQUID_INK, base.clone().add(0, height, 0), 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void fan(Player player, float yaw, float fromPitch, float toPitch, float scale) {
        World world = player.getWorld();
        Location pivot = player.getLocation().add(0, GRIP_HEIGHT, 0).add(horizontal(yaw).multiply(0.3));
        Vector forward = horizontal(yaw);
        double reach = knightSwordModel.length(scale);
        double from = Math.toRadians(fromPitch);
        double to = Math.toRadians(toPitch);
        double step = Math.toRadians(2.5);

        for (double angle = from; angle <= to + 1.0E-6; angle += step) {
            Vector direction = forward.clone().multiply(Math.cos(angle)).add(new Vector(0, -Math.sin(angle), 0));
            for (double radius = 1.0; radius <= reach; radius += 0.4) {
                Location point = pivot.clone().add(direction.clone().multiply(radius));
                Particle.DustOptions dust;
                if (radius > reach - 0.6) dust = TRAIL_EDGE;
                else dust = Math.random() < 0.2 ? TRAIL_SHADE : TRAIL;
                world.spawnParticle(Particle.DUST, point, 1, 0.04, 0.04, 0.04, 0, dust);
            }
        }

        Vector tipDirection = forward.clone().multiply(Math.cos(to)).add(new Vector(0, -Math.sin(to), 0));
        world.spawnParticle(Particle.SWEEP_ATTACK, pivot.clone().add(tipDirection.multiply(reach * 0.75)), 1, 0, 0, 0, 0);
    }

    private void carve(Player player, float yaw, float fromPitch, float toPitch, float scale, double width) {
        Location pivot = player.getLocation().add(0, GRIP_HEIGHT, 0).add(horizontal(yaw).multiply(0.3));
        Vector forward = horizontal(yaw);
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        double reach = knightSwordModel.length(scale);
        double half = width / 2.0;
        double from = Math.toRadians(fromPitch);
        double to = Math.toRadians(toPitch);
        double step = Math.toRadians(3);

        Set<Block> cut = new HashSet<>();
        for (double angle = from; angle <= to + 1.0E-6; angle += step) {
            Vector direction = forward.clone().multiply(Math.cos(angle)).add(new Vector(0, -Math.sin(angle), 0));
            for (double radius = 0.8; radius <= reach; radius += 0.5) {
                Location point = pivot.clone().add(direction.clone().multiply(radius));
                for (double side = -half; side <= half + 1.0E-6; side += 0.5) {
                    Block block = point.clone().add(right.clone().multiply(side)).getBlock();
                    if (!cut.add(block)) continue;
                    if (block.getType().isAir() || UNBREAKABLE_BLOCKS.contains(block.getType())) continue;
                    breakBlockSafely(block);
                }
            }
        }
    }

    private void pull(Player player, int epoch, bladeBond.Blade blade, double amp) {
        World world = player.getWorld();
        Material material = blade.getMaterial();
        LivingEntity host = blade.getHost();
        knightSwordModel model = blades.detach(player, blade);

        double damage = bladeBond.swordDamage(material) / config.f_Pull_Divide * amp;
        DamageSource source = source(player, DamageType.MAGIC);
        Set<UUID> struck = new HashSet<>();

        if (host != null && bladeBond.isTarget(player, host)) {
            struck.add(host.getUniqueId());
            new ForceDamage(host, damage, source, true).applyEffect(player);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, host.getLocation().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3, 0.2);
            world.playSound(host.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8f, 0.8f);
        }

        if (!model.isValid()) {
            model.remove();
            blades.heal(player);
            return;
        }

        long duration = Math.max(1L, config.f_Pull_Duration / 50L);
        Location start = model.getDisplay().getLocation();

        new BukkitRunnable() {
            long ticks = 0;
            Location previous = start.clone();

            @Override
            public void run() {
                if (!blades.isCurrent(player, epoch) || !player.isOnline() || player.isDead()
                        || !model.isValid() || !player.getWorld().equals(start.getWorld())) {
                    model.remove();
                    cancel();
                    return;
                }
                ticks++;

                Location hand = player.getLocation().add(0, GRIP_HEIGHT + 1.5, 0);
                double t = Math.min(1.0, (double) ticks / duration);
                Location next = start.clone().add(hand.toVector().subtract(start.toVector()).multiply(t * t));

                Vector move = next.toVector().subtract(previous.toVector());
                double distance = move.length();
                if (distance > 1.0E-4) {
                    BoundingBox area = BoundingBox.of(previous, next).expand(0.6);
                    for (Entity entity : world.getNearbyEntities(area)) {
                        if (!bladeBond.isTarget(player, entity) || struck.contains(entity.getUniqueId())) continue;

                        BoundingBox box = entity.getBoundingBox().expand(0.3);
                        if (box.contains(previous.toVector()) || box.rayTrace(previous.toVector(), move, distance) != null) {
                            struck.add(entity.getUniqueId());
                            new ForceDamage((LivingEntity) entity, damage, source, true).applyEffect(player);
                            world.spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.1);
                        }
                    }
                    next.setDirection(move.clone().multiply(-1));
                    bladeBond.trail(world, next, move.clone().multiply(-1));
                }

                model.getDisplay().teleport(next);
                previous = next;

                if (ticks >= duration) {
                    model.remove();
                    cancel();
                    blades.heal(player);
                    world.spawnParticle(Particle.DUST, hand, 10, 0.3, 0.3, 0.3, 0, bladeBond.BLACK);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void impact(Player player, Vector forward, double damage, long stun, boolean breaking,
                        double length, double width, float scale, int wear) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        double half = width / 2.0;

        DamageSource arcane = source(player, DamageType.MAGIC);

        Location middle = origin.clone().add(forward.clone().multiply(length / 2.0));
        for (Entity entity : world.getNearbyEntities(middle, length / 2.0 + 2.0, 4.0, length / 2.0 + 2.0)) {
            if (!bladeBond.isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;

            Vector relative = target.getLocation().toVector().subtract(origin.toVector());
            double along = relative.dot(forward);
            double side = relative.dot(right);
            double margin = target.getBoundingBox().getWidthX() / 2.0;

            if (along < -margin || along > length + margin) continue;
            if (Math.abs(side) > half + margin) continue;
            if (relative.getY() < -2.0 || relative.getY() > 3.0) continue;

            if (stun > 0) new Stun(target, stun).applyEffect(player);
            new ForceDamage(target, damage, arcane, true).applyEffect(player);

            world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.3, 0), 20, 0.4, 0.4, 0.4, 1);
            world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.8f);
        }

        if (breaking) {
            for (double along = 1.0; along <= length; along += 0.5) {
                for (double side = -half; side <= half; side += 0.5) {
                    Location column = origin.clone().add(forward.clone().multiply(along)).add(right.clone().multiply(side));
                    for (int dy = 0; dy <= 2; dy++) {
                        Block block = column.clone().add(0, dy, 0).getBlock();
                        if (block.getType().isAir() || UNBREAKABLE_BLOCKS.contains(block.getType())) continue;
                        breakBlockSafely(block);
                    }
                }
            }
        }

        for (double along = 0.5; along <= length; along += 0.35) {
            Location line = origin.clone().add(forward.clone().multiply(along)).add(0, 0.15, 0);
            for (double side = -half; side <= half + 1.0E-6; side += half / 2.0) {
                world.spawnParticle(Particle.DUST, line.clone().add(right.clone().multiply(side)), 1, 0.05, 0.05, 0.05, 0, bladeBond.BLACK);
            }
            if (Math.random() < 0.3) world.spawnParticle(Particle.SQUID_INK, line, 2, 0.3, 0.1, 0.3, 0.05);
            if (Math.random() < 0.17) world.spawnParticle(Particle.DUST, line.clone().add(0, 0.6, 0), 1, 0.2, 0.4, 0.2, 0, bladeBond.WHITE);
        }

        Location tip = origin.clone().add(forward.clone().multiply(Math.min(length, knightSwordModel.length(scale))));
        world.spawnParticle(Particle.EXPLOSION, tip, 2, 0.6, 0.2, 0.6, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, tip, 20, 1.0, 0.3, 1.0, 0.02);

        Block ground = tip.clone().subtract(0, 0.5, 0).getBlock();
        if (ground.getType().isSolid()) {
            world.spawnParticle(Particle.DUST_PILLAR, tip, 40, 1.2, 0.2, 1.2, 0.2, ground.getBlockData());
        }

        world.playSound(origin, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1f, 0.7f);
        world.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
        world.playSound(origin, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.5f, 0.7f);
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);

        bladeBond.wear(player, EquipmentSlot.HAND, wear);
    }

    public void breakBlockSafely(Block block) {
        if (UNBREAKABLE_BLOCKS.contains(block.getType())) {
            return;
        }

        block.getWorld().spawnParticle(
                Particle.BLOCK_CRUMBLE,
                block.getLocation().add(0.5, 0.5, 0.5),
                7,
                0.3, 0.3, 0.3,
                block.getBlockData()
        );

        block.breakNaturally(new ItemStack(Material.NETHERITE_SWORD));
    }
}