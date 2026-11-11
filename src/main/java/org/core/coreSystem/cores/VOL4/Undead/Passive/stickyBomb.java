package org.core.coreSystem.cores.VOL4.Undead.Passive;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;
import org.core.models.coresObj.undeadBombModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class stickyBomb {

    private final Undead config;
    private final JavaPlugin plugin;

    private final Map<UUID, Bomb> bombs = new HashMap<>();

    private final class Bomb {
        final UUID owner;
        final LivingEntity target;
        final Vector local;
        final boolean head;
        final undeadBombModel model;
        Location position;
        boolean stuck = false;
        int primed = -1;
        BukkitRunnable task;

        Bomb(UUID owner, LivingEntity target, Vector local, boolean head, undeadBombModel model, Location position) {
            this.owner = owner;
            this.target = target;
            this.local = local;
            this.head = head;
            this.model = model;
            this.position = position;
        }
    }

    public stickyBomb(Undead config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void attach(Player player, LivingEntity target) {
        Bomb old = bombs.get(target.getUniqueId());
        if (old != null) detonate(old, false);

        World world = target.getWorld();
        Location base = target.getLocation();
        double height = target.getHeight();
        double eye = target.getEyeHeight();

        Vector toward = player.getLocation().toVector().subtract(base.toVector()).setY(0);
        if (toward.lengthSquared() < 1.0E-6) toward = player.getLocation().getDirection().multiply(-1).setY(0);
        toward.normalize().multiply(target.getWidth() / 2 + 0.04);

        double y = Math.max(0.2, Math.min(height - 0.15, player.getLocation().getY() + 1.1 - base.getY()));
        Vector offset = toward.setY(y);

        boolean head = y >= eye - 0.25;
        Vector local = toLocal(target, offset, head);

        Location hand = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.4))
                .add(rightOf(player).multiply(0.3)).add(0, -0.35, 0);

        undeadBombModel model = new undeadBombModel(world, hand);

        Bomb bomb = new Bomb(player.getUniqueId(), target, local, head, model, hand);
        bombs.put(target.getUniqueId(), bomb);

        world.playSound(player, Sound.ENTITY_SNOWBALL_THROW, 1.1f, 0.6f);
        world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);
        world.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.9f, 1.3f);

        bomb.task = new BukkitRunnable() {
            int fuse = config.gun_q_Skill_Fuse;
            int blink = 0;
            int flight = 0;
            float spin = player.getLocation().getYaw();

            @Override
            public void run() {
                if (bombs.get(target.getUniqueId()) != bomb) {
                    cancel();
                    return;
                }

                if (!target.isValid() || target.isDead() || !target.getWorld().equals(world)) {
                    detonate(bomb, false);
                    return;
                }

                if (!bomb.stuck) {
                    flight++;
                    double u = Math.min(1.0, (double) flight / config.gun_q_Skill_Throw_Ticks);
                    Location goal = target.getLocation().add(toWorld(target, bomb.local, bomb.head));

                    Location at = hand.clone().add(goal.toVector().subtract(hand.toVector()).multiply(u));
                    at.add(0, Math.sin(Math.PI * u) * config.gun_q_Skill_Throw_Arc, 0);
                    spin += 70f;
                    at.setYaw(spin);
                    at.setPitch((float) (360 * u));

                    bomb.position = at;
                    bomb.model.teleport(at);

                    if (u >= 1.0) {
                        bomb.stuck = true;
                        follow(bomb);
                        world.playSound(bomb.position, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.1f, 1.5f);
                        world.playSound(bomb.position, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.3f);
                        world.playSound(bomb.position, Sound.BLOCK_LEVER_CLICK, 1.0f, 1.6f);
                        world.playSound(bomb.position, Sound.BLOCK_NOTE_BLOCK_BIT, 0.9f, 1.4f);
                    }
                    return;
                }

                if (bomb.primed >= 0) {
                    follow(bomb);
                    bomb.model.toggle();
                    world.playSound(bomb.position, Sound.BLOCK_NOTE_BLOCK_BIT, 1.2f, 2.0f);
                    if (bomb.primed-- <= 0) detonate(bomb, true);
                    return;
                }

                if (fuse-- <= 0) {
                    detonate(bomb, false);
                    return;
                }

                follow(bomb);

                int interval = fuse > 40 ? 10 : fuse > 20 ? 5 : 2;
                if (++blink >= interval) {
                    blink = 0;
                    if (bomb.model.toggle()) {
                        world.playSound(bomb.position, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 2.0f);
                        world.playSound(bomb.position, Sound.BLOCK_NOTE_BLOCK_BIT, 0.6f, 1.8f);
                    }
                }
            }
        };
        bomb.task.runTaskTimer(plugin, 1L, 1L);
    }

    public void trigger(Player player, LivingEntity target) {
        Bomb bomb = bombs.get(target.getUniqueId());
        if (bomb == null || !bomb.stuck || bomb.primed >= 0 || !bomb.owner.equals(player.getUniqueId())) return;

        bomb.primed = config.gun_q_Skill_Trigger_Delay;
        player.getWorld().playSound(bomb.position, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.2f, 1.8f);
    }

    public void clear(Player player) {
        List<Bomb> owned = new ArrayList<>();
        for (Bomb bomb : bombs.values()) {
            if (bomb.owner.equals(player.getUniqueId())) owned.add(bomb);
        }
        for (Bomb bomb : owned) remove(bomb);
    }

    private void follow(Bomb bomb) {
        LivingEntity target = bomb.target;
        Location base = target.getLocation();
        Vector offset = toWorld(target, bomb.local, bomb.head);

        Location at = base.clone().add(offset);

        Vector outward = offset.clone();
        if (bomb.head) outward.setY(outward.getY() - target.getEyeHeight());
        else outward.setY(0);
        if (outward.lengthSquared() > 1.0E-6) at.setDirection(outward);

        bomb.position = at;
        bomb.model.teleport(at);
    }

    private void detonate(Bomb bomb, boolean triggered) {
        remove(bomb);

        Player owner = plugin.getServer().getPlayer(bomb.owner);
        Location center = bomb.position;
        World world = center.getWorld();
        double radius = config.gun_q_Skill_Radius;

        blast(world, center, radius);

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.8f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);
        world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 1.2f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.6f);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);

        if (owner == null) return;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(owner)
                .withDirectEntity(owner)
                .build();

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || entity == owner || entity instanceof ArmorStand || target.isDead()) continue;
            if (target.isInvulnerable() || entity.hasMetadata("NPC")) continue;

            Vector away = target.getBoundingBox().getCenter().subtract(center.toVector());
            if (entity != bomb.target && away.length() > radius) continue;

            Vector push = away.clone().setY(0);
            if (push.lengthSquared() < 1.0E-6) push = owner.getLocation().getDirection().setY(0);
            if (push.lengthSquared() > 1.0E-6) push.normalize().multiply(config.gun_q_Skill_Knockback);
            push.setY(config.gun_q_Skill_Lift);

            double buff = config.restore_buff_rate.getOrDefault(owner.getUniqueId(), 0.0);

            config.is_damaging.add(owner.getUniqueId());
            try {
                new ForceDamage(target, config.gun_q_Skill_Damage * (1.0 + buff), source, true).applyEffect(owner);
            } finally {
                config.is_damaging.remove(owner.getUniqueId());
            }
            if (triggered) {
                new Stun(target, config.gun_q_Skill_Stun).applyEffect(owner);
            } else {
                target.setVelocity(push);
            }
        }
    }

    private void blast(World world, Location center, double radius) {
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0, Color.fromRGB(255, 236, 200));
        world.spawnParticle(Particle.EXPLOSION, center, 4, radius * 0.25, radius * 0.2, radius * 0.25, 0);
        world.spawnParticle(Particle.FLAME, center, 36, 0.3, 0.3, 0.3, 0.3);
        world.spawnParticle(Particle.LAVA, center, 8, 0.4, 0.4, 0.4, 0);
        world.spawnParticle(Particle.CRIT, center, 30, 0.4, 0.4, 0.4, 0.9);
        world.spawnParticle(Particle.LARGE_SMOKE, center, 24, 0.5, 0.5, 0.5, 0.1);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 6) {
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 10, radius * 0.4, 0.4, radius * 0.4, 0.03);
                    cancel();
                    return;
                }

                double r = radius * (tick + 1) / 6.0;
                int points = (int) (14 + r * 7);
                for (int i = 0; i < points; i++) {
                    double angle = Math.PI * 2 * i / points;
                    Location at = center.clone().add(Math.cos(angle) * r, -0.3, Math.sin(angle) * r);
                    world.spawnParticle(Particle.LARGE_SMOKE, at, 1, 0.05, 0.05, 0.05, 0.02);
                    if (tick < 3) world.spawnParticle(Particle.FLAME, at, 1, 0.02, 0.02, 0.02, 0.01);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void remove(Bomb bomb) {
        if (bombs.get(bomb.target.getUniqueId()) == bomb) bombs.remove(bomb.target.getUniqueId());
        if (bomb.task != null && !bomb.task.isCancelled()) bomb.task.cancel();
        bomb.model.remove();
    }

    private Vector rightOf(Player player) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        return new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    private Vector toLocal(LivingEntity target, Vector world, boolean head) {
        Location base = target.getLocation();
        Vector v = world.clone();
        if (head) {
            double eye = target.getEyeHeight();
            v.setY(v.getY() - eye);
            v.rotateAroundY(Math.toRadians(base.getYaw()));
            v.rotateAroundX(-Math.toRadians(base.getPitch()));
            v.setY(v.getY() + eye);
        } else {
            v.rotateAroundY(Math.toRadians(target.getBodyYaw()));
        }
        return v;
    }

    private Vector toWorld(LivingEntity target, Vector local, boolean head) {
        Location base = target.getLocation();
        Vector v = local.clone();
        if (head) {
            double eye = target.getEyeHeight();
            v.setY(v.getY() - eye);
            v.rotateAroundX(Math.toRadians(base.getPitch()));
            v.rotateAroundY(-Math.toRadians(base.getYaw()));
            v.setY(v.getY() + eye);
        } else {
            v.rotateAroundY(-Math.toRadians(target.getBodyYaw()));
        }
        return v;
    }
}