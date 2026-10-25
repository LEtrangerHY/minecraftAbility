package org.core.coreSystem.cores.VOL5.Apocalypse.Skill;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem.Apocalypse;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class F implements SkillBase {

    private final Apocalypse config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final R r;
    private final BatCombo combo;
    private final Consumer<Player> handsChanged;
    private final NamespacedKey keyF;
    private final Random random = new Random();
    private final Map<UUID, State> states = new HashMap<>();
    private final Set<UUID> drawn = new HashSet<>();

    private static final String BAR_COUNTER = "Parry - Counter";
    private static final Color EDGE = Color.fromRGB(20, 18, 16);
    private static final Color GRAD_DARK = Color.fromRGB(74, 64, 48);
    private static final Color GRAD_LIGHT = Color.fromRGB(130, 114, 82);
    private static final Color BIRCH_DARK = Color.fromRGB(190, 170, 122);
    private static final Color BIRCH = Color.fromRGB(214, 196, 150);
    private static final Color PALE = Color.fromRGB(228, 222, 210);
    private static final Particle.DustOptions SPLINTER = new Particle.DustOptions(BIRCH, 0.6f);
    private static final Particle.DustOptions STANCE = new Particle.DustOptions(Color.fromRGB(216, 204, 182), 0.4f);

    private record Look(double edgeGap, float edgeSize, double edgeArc, double denseWidth, double denseStep,
                        double denseArc, float denseOuter, float denseInner, double lineArc,
                        double[] lineOffsets, double[] lineFractions, float[] lineSizes) {}

    private static final Look FLURRY_LOOK = new Look(0.09, 0.62f, 0.1, 1.2, 0.1, 0.2, 0.56f, 0.44f, 0.1,
            new double[]{0.3, 0.75, 1.35, 2.05}, new double[]{1.0, 0.8, 0.6, 0.45},
            new float[]{0.45f, 0.42f, 0.39f, 0.36f});
    private static final Look COUNTER_LOOK = new Look(0.1, 0.66f, 0.1, 1.4, 0.1, 0.2, 0.56f, 0.46f, 0.1,
            new double[]{0.3, 0.75, 1.35, 2.05, 2.85}, new double[]{1.0, 0.85, 0.7, 0.55, 0.45},
            new float[]{0.45f, 0.42f, 0.39f, 0.36f, 0.34f});
    private static final Look GUARD_LOOK = new Look(0.05, 0.45f, 0.06, 0.25, 0.05, 0.09, 0.42f, 0.38f, 0.07,
            new double[]{0.12}, new double[]{1.0}, new float[]{0.35f});

    private enum Phase {PARRY, COUNTER, DASH}

    private static final class State {
        final Phase phase;
        LivingEntity target;
        BukkitTask task;

        State(Phase phase) {
            this.phase = phase;
        }
    }

    public F(Apocalypse config, JavaPlugin plugin, Cool cool, R r, BatCombo combo, Consumer<Player> handsChanged) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.r = r;
        this.combo = combo;
        this.handsChanged = handsChanged;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        State state = states.get(player.getUniqueId());
        if (state != null) {
            if (state.phase == Phase.COUNTER) startDash(player, state);
            return;
        }

        r.endWindow(player, true);
        draw(player);
        if (player.isSneaking()) {
            startParry(player);
        } else {
            windup(player);
            flurry(player, config.f_Skill_windup);
        }
    }

    public boolean isDashing(Player player) {
        State state = states.get(player.getUniqueId());
        return state != null && state.phase == Phase.DASH;
    }

    public boolean isDrawn(Player player) {
        return drawn.contains(player.getUniqueId());
    }

    public void onSneakRelease(Player player) {
        State state = states.get(player.getUniqueId());
        if (state == null || state.phase != Phase.PARRY) return;
        end(player);
        flurry(player, config.f_Skill_releaseDelay);
    }

    public void tryParry(Player player, EntityDamageByEntityEvent event) {
        UUID id = player.getUniqueId();
        State state = states.get(id);
        if (state == null || state.phase != Phase.PARRY) return;

        Entity damager = event.getDamager();
        LivingEntity attacker = null;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        } else if (damager instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null || attacker.equals(player)) return;

        event.setCancelled(true);
        Vector from = damager.getBoundingBox().getCenter();
        if (damager instanceof Projectile projectile) projectile.remove();
        end(player);
        parryEffect(player, from);

        long window = config.f_counter_window;
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (window / 50L), 0, false, false, false));

        State counter = new State(Phase.COUNTER);
        counter.target = attacker;
        counter.task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (states.get(id) != counter) return;
            states.remove(id);
            sheathe(player);
        }, window / 50L);
        states.put(id, counter);
        cool.setCooldown(player, window, BAR_COUNTER, "boss");
    }

    public void cleanup(Player player) {
        State state = states.get(player.getUniqueId());
        if (state != null && state.target != null && state.target.isValid()) {
            state.target.removePotionEffect(PotionEffectType.GLOWING);
        }
        end(player);
        sheathe(player);
    }

    private void draw(Player player) {
        UUID id = player.getUniqueId();
        if (drawn.contains(id)) return;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() != Apocalypse.BAT
                || inventory.getItemInOffHand().getType() != Apocalypse.SWORD) return;

        drawn.add(id);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!drawn.contains(id) || !player.isOnline()) return;
            if (!swapHands(player, Apocalypse.BAT, Apocalypse.SWORD)) return;
            handsChanged.accept(player);

            World world = player.getWorld();
            loud(world, player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.6f, 1);
            loud(world, player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.8f, 1);
        });
    }

    private void sheathe(Player player) {
        if (!drawn.remove(player.getUniqueId())) return;
        if (!swapHands(player, Apocalypse.SWORD, Apocalypse.BAT)) return;
        handsChanged.accept(player);

        World world = player.getWorld();
        loud(world, player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 0.8f, 1);
        loud(world, player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.7f, 0.7f, 1);
    }

    private boolean swapHands(Player player, Material main, Material off) {
        PlayerInventory inventory = player.getInventory();
        ItemStack actualMain = inventory.getItemInMainHand().clone();
        ItemStack actualOff = inventory.getItemInOffHand().clone();
        if (actualMain.getType() != main || actualOff.getType() != off) return false;

        inventory.setItemInMainHand(null);
        inventory.setItemInOffHand(null);
        inventory.setItemInOffHand(actualMain);
        inventory.setItemInMainHand(actualOff);
        player.updateInventory();
        return true;
    }

    private void sheatheAfter(Player player, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!states.containsKey(player.getUniqueId())) sheathe(player);
        }, delay);
    }

    private void end(Player player) {
        State state = states.remove(player.getUniqueId());
        if (state == null) return;
        if (state.task != null) state.task.cancel();
        if (state.phase == Phase.COUNTER) cool.updateCooldown(player, BAR_COUNTER, 0L);
    }

    private long fCooldown(Player player) {
        return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
    }

    private double amp(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        return 1 + config.f_Skill_amp * level;
    }

    private void loud(World world, Location at, Sound sound, float volume, float pitch) {
        loud(world, at, sound, volume, pitch, config.f_sound_layers);
    }

    private void loud(World world, Location at, Sound sound, float volume, float pitch, int copies) {
        float scaled = volume * (float) config.f_sound_range;
        for (int i = 0; i < copies; i++) {
            world.playSound(at, sound, SoundCategory.MASTER, scaled, pitch);
        }
    }

    private static Vector rightOf(Vector forward) {
        Vector flat = new Vector(-forward.getZ(), 0, forward.getX());
        if (flat.lengthSquared() < 1e-6) return new Vector(1, 0, 0);
        return flat.normalize();
    }

    private void startParry(Player player) {
        UUID id = player.getUniqueId();
        State state = new State(Phase.PARRY);
        states.put(id, state);

        World world = player.getWorld();
        loud(world, player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.9f, 0.6f, 1);
        loud(world, player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.25f, 1.9f, 1);

        state.task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (states.get(id) != state) {
                    cancel();
                    return;
                }
                if (!player.isOnline() || player.isDead()) {
                    states.remove(id);
                    cancel();
                    return;
                }
                if (!player.isSneaking()) {
                    onSneakRelease(player);
                    return;
                }
                if (ticks % 8 == 0) stance(player);
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void stance(Player player) {
        World world = player.getWorld();
        Location feet = player.getLocation();
        double offset = random.nextDouble() * Math.PI;
        for (int i = 0; i < 12; i++) {
            double a = offset + i * Math.PI / 6;
            world.spawnParticle(Particle.DUST, feet.clone().add(Math.cos(a) * 0.75, 0.1, Math.sin(a) * 0.75), 1, 0, 0, 0, 0, STANCE);
        }
    }

    private void parryEffect(Player player, Vector from) {
        World world = player.getWorld();
        Location chest = player.getEyeLocation().subtract(0, 0.4, 0);
        Vector toward = from.clone().subtract(chest.toVector());
        if (toward.lengthSquared() < 1e-6) toward = chest.getDirection();
        toward.normalize();

        Vector right = rightOf(toward);
        Vector up = right.clone().crossProduct(toward).normalize();
        double tilt = Math.toRadians(62);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        double guardHalf = Math.toRadians(40);
        drawBand(world, chest, toward, slant, guardHalf, -guardHalf, guardHalf, 0, 0.5, 1.05, GUARD_LOOK);

        Location guard = chest.clone().add(toward.clone().multiply(0.55));
        world.spawnParticle(Particle.CRIT, guard, 16, 0.12, 0.18, 0.12, 0.4);
        world.spawnParticle(Particle.SMALL_GUST, guard, 1, 0, 0, 0, 0);
        loud(world, guard, Sound.BLOCK_WOOD_HIT, 1.0f, 0.8f);
        loud(world, guard, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.2f);
        loud(world, guard, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.3f);
        loud(world, guard, Sound.ENTITY_BREEZE_WIND_BURST, 0.7f, 1.0f);
    }

    private void flurry(Player player, int windup) {
        cool.setCooldown(player, fCooldown(player), "F");

        int[] frames = config.f_Skill_frames;
        int start = windup;
        for (int i = 0; i < frames.length; i++) {
            int index = i;
            int length = frames[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> slash(player, index, length), start);
            start += length;
        }
        sweepProjectiles(player, start + 3);
        sheatheAfter(player, start + 3L);
    }

    private void windup(Player player) {
        World world = player.getWorld();
        Location feet = player.getLocation();
        loud(world, feet, Sound.ENTITY_BREEZE_INHALE, 0.5f, 1.0f, 1);
        loud(world, feet, Sound.ENTITY_BREEZE_CHARGE, 0.45f, 1.0f, 1);

        int windup = config.f_Skill_windup;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, windup + 1, 2, false, false, false));

        double offset = random.nextDouble() * Math.PI;
        for (int i = 0; i < 14; i++) {
            double a = offset + i * Math.PI / 7;
            world.spawnParticle(Particle.DUST, feet.clone().add(Math.cos(a) * 1.1, 0.1, Math.sin(a) * 1.1),
                    1, 0, 0, 0, 0, STANCE);
        }
        world.spawnParticle(Particle.SMALL_GUST, feet.clone().add(0, 0.2, 0), 2, 0.3, 0, 0.3, 0);
    }

    private void slash(Player player, int index, int frames) {
        if (!player.isOnline() || player.isDead()) return;
        swingEchoGuard.swingMainHand(player);

        Location eye = player.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = rightOf(forward);
        Vector up = new Vector(0, 1, 0);

        boolean falling = index % 2 == 0;
        double jitter = (random.nextDouble() * 2 - 1) * config.f_Skill_tiltJitter;
        double tilt = Math.toRadians(config.f_Skill_tilt + jitter) * (falling ? 1 : -1);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();

        double half = Math.toRadians(config.f_Skill_angle / 2.0);
        double reach = config.f_Skill_reach;
        double spacing = 2 * reach * Math.sin(Math.toRadians(config.f_Skill_tilt)) * Math.sin(half / 2);
        Location pivot = eye.clone().add(0, (1.5 - index) * spacing, 0);

        World world = player.getWorld();
        Location voice = midpoint(eye, pivot.clone().add(forward.clone().multiply(reach)));
        float pitch = (float) (0.9 + index * 0.05 + random.nextDouble() * 0.05);
        loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, pitch, config.f_sound_slashLayers);
        loud(world, voice, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        loud(world, voice, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.7f, 0.9f, 1);
        if (index == 0) loud(world, voice, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.75f);

        cut(player, pivot, forward, slant, half, config.f_Skill_inner, reach, falling, frames,
                config.f_Skill_damage, null, 0, false, index == 0, index == config.f_Skill_frames.length - 1, voice);
    }

    private void startDash(Player player, State counter) {
        UUID id = player.getUniqueId();
        LivingEntity target = counter.target;
        end(player);

        if (target == null || !target.isValid() || target.isDead() || !target.getWorld().equals(player.getWorld())
                || target.getLocation().distance(player.getLocation()) > config.f_dash_range) {
            if (target != null && target.isValid()) target.removePotionEffect(PotionEffectType.GLOWING);
            sheathe(player);
            return;
        }

        r.endWindow(player, true);
        cool.setCooldown(player, fCooldown(player), "F");

        State dash = new State(Phase.DASH);
        dash.target = target;
        states.put(id, dash);

        int charge = config.f_dash_charge;
        int maxTicks = charge + (int) Math.ceil(config.f_dash_range / config.f_dash_speed) + 4;
        new Invulnerable(player, (maxTicks + 2) * 50L).applyEffect(player);

        World world = player.getWorld();
        loud(world, player.getLocation(), Sound.ENTITY_BREEZE_INHALE, 0.5f, 1.0f, 1);
        loud(world, player.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 0.45f, 1.0f, 1);

        dash.task = new BukkitRunnable() {
            int tick = 0;
            double travelled = 0;
            double nextRing = config.f_dash_ringEvery;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    finishDash(player, dash);
                    cancel();
                    return;
                }
                boolean alive = target.isValid() && !target.isDead() && target.getWorld().equals(player.getWorld());
                if (!alive || tick > maxTicks) {
                    finishDash(player, dash);
                    cancel();
                    return;
                }

                if (tick < charge) {
                    player.setVelocity(new Vector());
                    player.lookAt(target, LookAnchor.EYES, LookAnchor.EYES);
                    gather(player, tick, charge);
                    tick++;
                    return;
                }

                Location from = player.getLocation();
                Vector body = from.toVector().add(new Vector(0, player.getHeight() / 2.0, 0));
                Vector toGoal = strikePoint(player, target).subtract(from.toVector());
                double remaining = toGoal.length();
                Vector heading = remaining > 1e-6 ? toGoal.clone().multiply(1.0 / remaining) : from.getDirection();

                if (tick == charge) launch(player, body, heading);
                tick++;

                double move = Math.min(config.f_dash_speed, remaining);
                Location reached = advance(player, from, heading, move);
                double step = reached.toVector().distance(from.toVector());

                streak(world, body, heading, step, travelled);
                while (nextRing <= travelled + step) {
                    Location ringAt = body.clone().add(heading.clone().multiply(nextRing - travelled)).toLocation(world);
                    shockRing(world, ringAt, heading, 0.3, 0.2, 16);
                    nextRing += config.f_dash_ringEvery;
                }
                travelled += step;

                Vector look = target.getEyeLocation().toVector()
                        .subtract(reached.toVector().add(new Vector(0, player.getEyeHeight(), 0)));
                if (look.lengthSquared() > 1e-6) reached.setDirection(look);
                player.teleport(reached);
                player.setVelocity(new Vector());
                player.setFallDistance(0f);
                loud(world, reached, Sound.ENTITY_BREEZE_WIND_BURST, 0.6f, 1.0f, 1);

                if (remaining <= config.f_dash_speed || step < move - 0.05) {
                    finishDash(player, dash);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector strikePoint(Player player, LivingEntity target) {
        Vector feet = target.getLocation().toVector();
        Vector away = player.getLocation().toVector().subtract(feet).setY(0);
        if (away.lengthSquared() < 1e-6) away = player.getLocation().getDirection().multiply(-1).setY(0);
        if (away.lengthSquared() < 1e-6) away = new Vector(0, 0, 1);
        double strike = config.f_counter_strike;
        return feet.add(away.normalize().multiply(strike));
    }

    private void gather(Player player, int tick, int total) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 1.0, 0);
        double ratio = (tick + 1.0) / total;
        double radius = 1.6 * (1 - ratio) + 0.4;
        Particle.DustOptions dust = new Particle.DustOptions(BIRCH, 0.5f);
        for (int i = 0; i < 12; i++) {
            double a = tick * 0.8 + i * Math.PI / 6;
            world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * radius, (i % 3 - 1) * 0.35,
                    Math.sin(a) * radius), 1, 0, 0, 0, 0, dust);
        }
        if (tick == 0) world.spawnParticle(Particle.SMALL_GUST, player.getLocation().add(0, 0.2, 0), 2, 0.3, 0, 0.3, 0);
    }

    private void launch(Player player, Vector body, Vector heading) {
        World world = player.getWorld();
        Location at = body.toLocation(world);
        shockRing(world, at, heading, 0.25, 0.3, 22);
        world.spawnParticle(Particle.EXPLOSION, at.clone().subtract(heading.clone().multiply(0.6)), 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.GUST, at, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.SMALL_GUST, at, 3, 0.4, 0.4, 0.4, 0);
        loud(world, at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.7f);
        loud(world, at, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        loud(world, at, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.7f);
        loud(world, at, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.6f);
    }

    private void streak(World world, Vector from, Vector heading, double length, double travelled) {
        if (length < 0.05) return;
        Vector right = rightOf(heading);
        Vector up = right.clone().crossProduct(heading).normalize();
        Particle.DustTransition core = new Particle.DustTransition(PALE, BIRCH, 0.8f);
        Particle.DustOptions coil = new Particle.DustOptions(BIRCH, 0.45f);
        for (double d = 0; d <= length; d += 0.18) {
            Location p = from.clone().add(heading.clone().multiply(d)).toLocation(world);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.02, 0.02, 0.02, 0, core);
            double twist = (travelled + d) * 1.7;
            for (int s = 0; s < 3; s++) {
                double a = twist + s * 2 * Math.PI / 3;
                Vector offset = right.clone().multiply(Math.cos(a) * 0.45).add(up.clone().multiply(Math.sin(a) * 0.45));
                world.spawnParticle(Particle.DUST, p.clone().add(offset), 1, 0, 0, 0, 0, coil);
            }
        }
    }

    private void shockRing(World world, Location at, Vector heading, double start, double speed, int points) {
        Vector right = rightOf(heading);
        Vector up = right.clone().crossProduct(heading).normalize();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Vector out = right.clone().multiply(Math.cos(a)).add(up.clone().multiply(Math.sin(a)));
            Location p = at.clone().add(out.clone().multiply(start));
            world.spawnParticle(Particle.CLOUD, p, 0, out.getX(), out.getY(), out.getZ(), speed);
        }
    }

    private Location advance(Player player, Location from, Vector heading, double move) {
        World world = player.getWorld();
        double half = player.getWidth() / 2.0;
        double height = player.getHeight();
        Location safe = from.clone();
        int broken = 0;
        int steps = (int) Math.ceil(move / 0.5);
        for (int i = 1; i <= steps; i++) {
            Location next = from.clone().add(heading.clone().multiply(Math.min(move, i * 0.5)));
            BoundingBox box = new BoundingBox(next.getX() - half, next.getY(), next.getZ() - half,
                    next.getX() + half, next.getY() + height, next.getZ() + half);
            broken += carve(player, box);
            if (blocked(world, box)) break;
            safe = next;
        }
        if (broken > 0) {
            loud(world, safe, Sound.ENTITY_GENERIC_EXPLODE, (float) Math.min(1.0, 0.4 + broken * 0.05), 1.8f, 1);
            loud(world, safe, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.7f, 1);
        }
        return safe;
    }

    private int carve(Player player, BoundingBox box) {
        World world = player.getWorld();
        int broken = 0;
        int minX = (int) Math.floor(box.getMinX() - 0.05);
        int maxX = (int) Math.floor(box.getMaxX() + 0.05);
        int minY = (int) Math.floor(box.getMinY() + 0.25);
        int maxY = (int) Math.floor(box.getMaxY() + 0.05);
        int minZ = (int) Math.floor(box.getMinZ() - 0.05);
        int maxZ = (int) Math.floor(box.getMaxZ() + 0.05);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isPassable() || !breakable(block)) continue;

                    BlockBreakEvent event = new BlockBreakEvent(block, player);
                    event.setDropItems(false);
                    event.setExpToDrop(0);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) continue;

                    BlockData data = block.getBlockData();
                    block.setType(Material.AIR);
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 10,
                            0.3, 0.3, 0.3, 0.12, data);
                    broken++;
                }
            }
        }
        return broken;
    }

    private boolean blocked(World world, BoundingBox box) {
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY() + 0.05);
        int maxY = (int) Math.floor(box.getMaxY() - 0.01);
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!world.getBlockAt(x, y, z).isPassable()) return true;
                }
            }
        }
        return false;
    }

    private void finishDash(Player player, State dash) {
        UUID id = player.getUniqueId();
        if (states.get(id) == dash) states.remove(id);
        if (Invulnerable.isInvulnerable(player)) new Invulnerable(player, 0).removeEffect(player);

        LivingEntity target = dash.target;
        boolean valid = target != null && target.isValid() && !target.isDead();
        if (valid) target.removePotionEffect(PotionEffectType.GLOWING);
        if (!player.isOnline() || player.isDead()) {
            sheathe(player);
            return;
        }

        player.setFallDistance(0f);
        Location arrival = player.getLocation().add(0, player.getHeight() / 2.0, 0);
        player.getWorld().spawnParticle(Particle.SMALL_GUST, arrival, 2, 0.3, 0.3, 0.3, 0);
        boolean reachable = valid && target.getWorld().equals(player.getWorld());
        counterSlash(player, reachable ? target : null);
        sheatheAfter(player, 7L);
    }

    private boolean breakable(Block block) {
        Material type = block.getType();
        if (type.isAir() || block.isLiquid()) return false;
        float hardness = type.getHardness();
        if (hardness < 0 || hardness >= 25) return false;
        return !(block.getState() instanceof TileState);
    }

    private void counterSlash(Player player, LivingEntity target) {
        swingEchoGuard.swingMainHand(player);

        Location eye = player.getEyeLocation();
        Vector forward = target != null
                ? target.getBoundingBox().getCenter().subtract(eye.toVector()).setY(0)
                : eye.getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) {
            double yaw = Math.toRadians(eye.getYaw());
            forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        forward.normalize();

        Vector right = rightOf(forward);
        Vector up = new Vector(0, 1, 0);
        double tilt = Math.toRadians(config.f_counter_tilt);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        Location pivot = eye.clone().subtract(0, config.f_counter_drop, 0);

        World world = player.getWorld();
        Location voice = midpoint(eye, pivot.clone().add(forward.clone().multiply(config.f_counter_reach)));
        loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.85f, config.f_sound_slashLayers);
        loud(world, voice, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        loud(world, voice, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.75f);

        cut(player, pivot, forward, slant, Math.toRadians(config.f_counter_angle / 2.0), config.f_counter_inner,
                config.f_counter_reach, true, 4, config.f_counter_damage, target,
                config.f_counter_knockback, true, true, true, voice);
    }

    private static Location midpoint(Location a, Location b) {
        return a.clone().add(b).multiply(0.5);
    }

    private static double angleAt(double half, boolean falling, double t) {
        return falling ? half - 2 * half * t : -half + 2 * half * t;
    }

    private static Location point(Location pivot, Vector forward, Vector slant, double angle, double radius) {
        return pivot.clone().add(forward.clone().multiply(Math.cos(angle) * radius)
                .add(slant.clone().multiply(Math.sin(angle) * radius)));
    }

    private void cut(Player player, Location pivot, Vector forward, Vector slant, double half, double inner,
                     double reach, boolean falling, int frames, double maxDamage,
                     LivingEntity forced, double knock, boolean big, boolean burst, boolean wake,
                     Location voice) {
        World world = player.getWorld();
        Vector normal = forward.clone().crossProduct(slant).normalize();
        LivingEntity aimed = forced != null ? forced : aimedTarget(player, reach);
        int hitFrame = (frames - 1) / 2;
        double motion = falling ? -1 : 1;
        double span = frames == 1 ? half : half / 2;

        new BukkitRunnable() {
            int frame = 0;

            @Override
            public void run() {
                if (frame >= frames || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                double center = frames == 1 ? 0 : angleAt(span, falling, frame / (double) (frames - 1));
                double lead = center + motion * span;
                drawBand(world, pivot, forward, slant, half, center - span, center + span, lead, inner, reach,
                        big ? COUNTER_LOOK : FLURRY_LOOK);

                if (frame == hitFrame) {
                    double gustAngle = Math.max(-half, Math.min(half, lead));
                    Location gust = point(pivot, forward, slant, gustAngle, reach * 0.8);
                    world.spawnParticle(Particle.SMALL_GUST, gust, big ? 3 : 2, 0.6, 0.15, 0.6, 0);
                    if (burst) world.spawnParticle(Particle.GUST, gust, big ? 2 : 1, 0.4, 0.1, 0.4, 0);
                    if (wake) groundWake(world, pivot, forward, slant, half, inner, reach, big ? 18 : 12);
                    boolean struck = forced != null && forced.isValid() && !forced.isDead()
                            && forced.getWorld().equals(world);
                    if (struck) strikeTarget(player, forced, slant, normal, voice);
                    resolve(player, pivot, forward, slant, normal, half, inner, reach,
                            reach - (big ? COUNTER_LOOK : FLURRY_LOOK).denseWidth(), maxDamage, aimed,
                            forced, knock, big && !struck, voice);
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity aimedTarget(Player player, double reach) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(eye, eye.getDirection(), reach, FluidCollisionMode.NEVER,
                true, 0.3, entity -> combo.isValidTarget(player, entity));
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) return null;
        return target;
    }

    private void drawBand(World world, Location pivot, Vector forward, Vector slant, double half,
                          double from, double to, double lead, double inner, double reach, Look look) {
        double lo = Math.max(-half, Math.min(from, to));
        double hi = Math.min(half, Math.max(from, to));
        if (hi <= lo) return;

        double width = Math.abs(to - from);

        for (int i = 0; i < 4; i++) {
            double rho = reach - i * look.edgeGap();
            Color color = i < 2 ? EDGE : i == 2 ? GRAD_DARK : GRAD_LIGHT;
            float size = (float) (look.edgeSize() - (i < 2 ? 0.02 * i : 0.04 + 0.02 * (i - 2)));
            Particle.DustOptions dust = new Particle.DustOptions(color, size);
            double arcStep = look.edgeArc() * (i < 2 ? 1 : 1.15) / rho;
            for (double a = lo + (i % 2 == 1 ? arcStep * 0.5 : 0); a <= hi; a += arcStep) {
                world.spawnParticle(Particle.DUST, point(pivot, forward, slant, a, rho), 1, 0, 0, 0, 0, dust);
            }
        }

        double denseStart = reach - 4 * look.edgeGap();
        int rows = Math.max(1, (int) Math.round((look.denseWidth() - 4 * look.edgeGap()) / look.denseStep()) + 1);
        for (int j = 0; j < rows; j++) {
            double rho = denseStart - j * look.denseStep();
            double t = rows == 1 ? 0 : j / (double) (rows - 1);
            float size = (float) (look.denseOuter() + (look.denseInner() - look.denseOuter()) * t);
            Particle.DustOptions birch = new Particle.DustOptions(BIRCH_DARK, size);
            Particle.DustOptions pale = new Particle.DustOptions(PALE, size);
            double paleChance = 0.2 + 0.4 * t;
            double arcStep = look.denseArc() / rho;
            for (double a = lo + (j % 2 == 1 ? arcStep * 0.5 : 0); a <= hi; a += arcStep) {
                world.spawnParticle(Particle.DUST, point(pivot, forward, slant, a, rho), 1, 0, 0, 0, 0,
                        random.nextDouble() < paleChance ? pale : birch);
            }
        }

        double bandEnd = reach - look.denseWidth();
        double wedgeLow = Math.min(from, to);
        double wedgeHigh = Math.max(from, to);
        boolean leadLow = Math.abs(lead - wedgeLow) < 1e-9;
        for (int k = 0; k < look.lineOffsets().length; k++) {
            double rho = bandEnd - look.lineOffsets()[k];
            if (rho < inner) break;

            double length = width * look.lineFractions()[k];
            double lineLow = Math.max(-half, leadLow ? wedgeLow : wedgeHigh - length);
            double lineHigh = Math.min(half, leadLow ? wedgeLow + length : wedgeHigh);
            if (lineHigh <= lineLow) continue;

            float size = look.lineSizes()[k];
            Particle.DustOptions line = k < 2
                    ? new Particle.DustTransition(BIRCH, PALE, size)
                    : new Particle.DustOptions(PALE, size);
            Particle particle = k < 2 ? Particle.DUST_COLOR_TRANSITION : Particle.DUST;
            double arcStep = look.lineArc() / rho;
            for (double a = lineLow; a <= lineHigh; a += arcStep) {
                world.spawnParticle(particle, point(pivot, forward, slant, a, rho), 1, 0, 0, 0, 0, line);
            }
        }
    }

    private void groundWake(World world, Location pivot, Vector forward, Vector slant, double half,
                            double inner, double reach, int points) {
        for (int i = 0; i < points; i++) {
            double a = -half + 2 * half * random.nextDouble();
            double rho = inner + (reach - inner) * random.nextDouble();
            Location p = point(pivot, forward, slant, a, rho);

            Block ground = null;
            for (int dy = 0; dy <= 3; dy++) {
                Block block = world.getBlockAt(p.getBlockX(), p.getBlockY() - dy, p.getBlockZ());
                if (!block.isPassable()) {
                    ground = block;
                    break;
                }
            }
            if (ground == null) continue;

            Location at = new Location(world, p.getX(), ground.getY() + 1.05, p.getZ());
            world.spawnParticle(Particle.BLOCK, at, 4, 0.25, 0.05, 0.25, 0.12, ground.getBlockData());
        }
    }

    private void resolve(Player player, Location pivot, Vector forward, Vector slant, Vector normal, double half,
                         double inner, double reach, double sweet, double maxDamage, LivingEntity aimed,
                         LivingEntity excluded, double knock, boolean heavy, Location voice) {
        World world = player.getWorld();
        double sigma = Math.toRadians(config.f_Skill_sigma);
        double amp = amp(player);
        double low = config.f_Skill_minDamage * amp;
        double high = maxDamage * amp;
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        LivingEntity primary = null;
        double best = -1;
        for (Entity entity : world.getNearbyEntities(pivot, reach + 2, reach + 2, reach + 2)) {
            if (!combo.isValidTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            if (target.equals(excluded)) continue;
            boolean isAimed = target.equals(aimed);

            BoundingBox box = target.getBoundingBox();
            Vector v = box.getCenter().subtract(pivot.toVector());
            double halfWidth = box.getWidthX() / 2.0;
            double along = v.dot(forward);
            double side = v.dot(slant);
            double off = v.dot(normal);
            double planar = Math.sqrt(along * along + side * side);

            double angle = Math.abs(Math.atan2(side, along));
            double slack = planar > 1e-3 ? Math.atan(halfWidth / planar) : Math.PI;
            double eff = Math.max(0, angle - slack);

            if (!isAimed) {
                if (planar - halfWidth > reach) continue;
                if (Math.abs(off) > config.f_Skill_band + halfWidth + box.getHeight() * 0.3) continue;
                if (eff > half) continue;
                if (!player.hasLineOfSight(target)) continue;
            }

            double centered = Math.exp(-(eff * eff) / (2 * sigma * sigma));
            double t = Math.max(0, Math.min(1, (planar + halfWidth - inner) / Math.max(0.1, sweet - inner)));
            double weight = centered * t * t * (3 - 2 * t);
            combo.skillHit(player, target, low + (high - low) * weight, source);

            if (knock > 0) {
                Vector away = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
                if (away.lengthSquared() > 1e-6) {
                    double push = knock * (0.35 + 0.65 * weight);
                    away.normalize().multiply(push);
                    target.setVelocity(new Vector(away.getX(), 0.15 + 0.2 * weight, away.getZ()));
                }
            }

            Location body = box.getCenter().toLocation(world);
            world.spawnParticle(Particle.CRIT, body, heavy ? 26 : (int) Math.round(8 + 10 * weight),
                    heavy ? 0.5 : 0.4, heavy ? 0.6 : 0.5, heavy ? 0.5 : 0.4, 0.5);
            world.spawnParticle(Particle.SWEEP_ATTACK, body, heavy ? 2 : 1, 0.2, 0.2, 0.2, 0);
            world.spawnParticle(Particle.DUST, body, heavy ? 10 : 6, 0.3, 0.4, 0.3, 0, SPLINTER);

            if (weight > best) {
                best = weight;
                primary = target;
            }
        }

        if (primary == null) return;
        Location at = primary.getBoundingBox().getCenter().toLocation(world);
        if (heavy) {
            heavyImpact(world, primary, at, voice);
        } else {
            loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.9f, config.f_sound_slashLayers);
            loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.9f, 0.85f);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f), 1L);
        }
    }

    private void strikeTarget(Player player, LivingEntity target, Vector slant, Vector normal, Location voice) {
        World world = player.getWorld();
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
        combo.skillHit(player, target, config.f_counter_damage * amp(player), source);

        Vector away = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
        if (away.lengthSquared() > 1e-6) {
            away.normalize().multiply(config.f_counter_knockback);
            target.setVelocity(new Vector(away.getX(), 0.35, away.getZ()));
        }

        Location body = target.getBoundingBox().getCenter().toLocation(world);
        world.spawnParticle(Particle.CRIT, body, 26, 0.5, 0.6, 0.5, 0.5);
        world.spawnParticle(Particle.SWEEP_ATTACK, body, 2, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.DUST, body, 10, 0.3, 0.4, 0.3, 0, SPLINTER);
        cutMark(world, body, slant, normal);
        heavyImpact(world, target, body, voice);
    }

    private void heavyImpact(World world, LivingEntity target, Location at, Location voice) {
        loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f, config.f_sound_slashLayers);
        loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        loud(world, voice, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.95f);
        world.spawnParticle(Particle.SMALL_GUST, at, 2, 0.3, 0.3, 0.3, 0);

        Block ground = target.getLocation().getBlock().getRelative(0, -1, 0);
        if (!ground.isPassable()) {
            world.spawnParticle(Particle.DUST_PILLAR, target.getLocation().add(0, 0.1, 0), 24,
                    0.8, 0.05, 0.8, 0, ground.getBlockData());
        }
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> loud(world, voice, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.9f), 1L);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> loud(world, voice, Sound.ENTITY_GENERIC_BIG_FALL, 0.8f, 0.8f, 1), 2L);
    }

    private void cutMark(World world, Location center, Vector along, Vector across) {
        Particle.DustOptions edge = new Particle.DustOptions(EDGE, 0.6f);
        Particle.DustOptions birch = new Particle.DustOptions(BIRCH_DARK, 0.5f);
        Particle.DustOptions pale = new Particle.DustOptions(PALE, 0.45f);
        for (double d = -1.4; d <= 1.4; d += 0.06) {
            double taper = 1 - Math.abs(d) / 1.4;
            Location p = center.clone().add(along.clone().multiply(d));
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, edge);
            if (taper > 0.2) {
                world.spawnParticle(Particle.DUST, p.clone().add(across.clone().multiply(0.08 * taper)), 1, 0, 0, 0, 0, birch);
                world.spawnParticle(Particle.DUST, p.clone().add(across.clone().multiply(-0.08 * taper)), 1, 0, 0, 0, 0, pale);
            }
        }
    }

    private void sweepProjectiles(Player player, int duration) {
        double half = Math.toRadians(config.f_Skill_angle / 2.0) + 0.25;
        double cosLimit = Math.cos(half);
        double reach = config.f_Skill_reach + 0.5;

        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> handled = new HashSet<>();

            @Override
            public void run() {
                if (ticks++ > duration || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                World world = player.getWorld();
                Location eye = player.getEyeLocation();
                Vector forward = eye.getDirection().normalize();
                for (Entity entity : world.getNearbyEntities(eye, reach, reach, reach)) {
                    if (!(entity instanceof Projectile projectile)) continue;
                    if (projectile.getShooter() instanceof Player shooter && shooter.equals(player)) continue;
                    if (projectile instanceof AbstractArrow arrow && arrow.isInBlock()) continue;

                    Vector v = projectile.getLocation().toVector().subtract(eye.toVector());
                    double distance = v.length();
                    if (distance > reach) continue;
                    if (distance > 0.5 && v.clone().normalize().dot(forward) < cosLimit) continue;
                    if (!handled.add(projectile.getUniqueId())) continue;

                    Location at = projectile.getLocation();
                    if (projectile instanceof Trident trident && trident.getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
                        trident.setVelocity(trident.getVelocity().multiply(-0.15));
                    } else {
                        projectile.remove();
                    }
                    world.spawnParticle(Particle.CRIT, at, 6, 0.1, 0.1, 0.1, 0.2);
                    loud(world, at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.6f, 1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}