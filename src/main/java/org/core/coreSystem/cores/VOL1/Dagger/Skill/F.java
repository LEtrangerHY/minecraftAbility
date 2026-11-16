package org.core.coreSystem.cores.VOL1.Dagger.Skill;

import org.core.effect.effectPlugin.swingEchoGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.basicAttack;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.bloodStroke;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class F implements SkillBase {
    private static final Particle.DustOptions EDGE = new Particle.DustOptions(Color.fromRGB(255, 96, 96), 0.75f);
    private static final Particle.DustOptions CORE = new Particle.DustOptions(Color.fromRGB(178, 12, 24), 0.65f);
    private static final Particle.DustOptions DEEP = new Particle.DustOptions(Color.fromRGB(64, 0, 10), 0.6f);

    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bloodStroke bloodStroker;
    private final basicAttack attack;

    public F(Dagger config, JavaPlugin plugin, Cool cool, bloodStroke bloodStroker, basicAttack attack) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.bloodStroker = bloodStroker;
        this.attack = attack;
    }

    @Override
    public void Trigger(Player player) {
        if(!config.f_using.getOrDefault(player.getUniqueId(), false) && !config.f_dash.getOrDefault(player.getUniqueId(), false)){

            Title title = Title.title(
                    Component.empty(),
                    Component.text("left click to slash").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 0L);
            X_slash_on(player);
        }else if(!config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_dash.getOrDefault(player.getUniqueId(), false)){
            X_finish(player);
        }else{

            Title title = Title.title(
                    Component.empty(),
                    Component.text("dash is not set").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 0L);
        }
    }

    public void X_slash_on(Player player){
        UUID uuid = player.getUniqueId();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        cool.setCooldown(player, 10000L, "X-SLASH", "boss");

        config.f_using.put(uuid, true);
        config.f_slash.put(uuid, 0);
        config.f_wave_live.remove(uuid);
        config.f_wave_target.put(uuid, new LivingEntity[2]);
        config.f_wave_mark.put(uuid, new Location[2]);
        config.f_wave_cross.remove(uuid);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(!config.f_using.getOrDefault(uuid, false)){
                    this.cancel();
                    return;
                }

                if(ticks % 10 == 0){
                    player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 13, 0.5, 0.1, 0.5, 0);
                }

                if(ticks >= 200){
                    config.f_slash.put(uuid, 2);
                }

                if(config.f_slash.getOrDefault(uuid, 0) >= 2 && config.f_wave_live.getOrDefault(uuid, 0) <= 0){
                    X_slash_off(player);
                    this.cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void X_slash_off(Player player){
        UUID uuid = player.getUniqueId();

        cool.updateCooldown(player, "X-SLASH", 50L, "boss");

        LivingEntity[] targets = config.f_wave_target.remove(uuid);
        Location[] marks = config.f_wave_mark.remove(uuid);
        boolean crossed = config.f_wave_cross.remove(uuid);
        config.f_using.remove(uuid);
        config.f_slash.remove(uuid);
        config.f_wave_live.remove(uuid);

        ArrayList<LivingEntity> objects = new ArrayList<>();
        ArrayList<Location> spots = new ArrayList<>();
        double power = 1.0;

        if(targets != null && marks != null){
            if(targets[0] != null && targets[0] == targets[1]){
                objects.add(targets[0]);
                spots.add(marks[1]);
                power = crossed ? config.f_Cross_Dash_Rate : config.f_Double_Rate;
            }else{
                for(int i = 0; i < targets.length; i++){
                    if(targets[i] == null) continue;
                    objects.add(targets[i]);
                    spots.add(marks[i]);
                }
            }
        }

        if(!objects.isEmpty()){
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
            config.dash_object.put(uuid, objects);
            config.dash_mark.put(uuid, spots);
            config.dash_power.put(uuid, power);
            config.f_dash.put(uuid, true);

            Title title = Title.title(
                    Component.empty(),
                    Component.text(objects.size() + " set" + (power > 1.0 ? " x" + power : "")).color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

        }else{
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            config.f_dash.remove(uuid);
            config.dash_object.remove(uuid);
            config.dash_mark.remove(uuid);
            config.dash_power.remove(uuid);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("no slash").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.setCooldown(player, 10000L, "F");
        }
    }

    public void wave(Player player){
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        int slot = config.f_slash.getOrDefault(uuid, 0);
        boolean cross = slot == 0 && attack.isDual(player);

        config.f_slash.put(uuid, cross ? 2 : slot + 1);
        config.f_wave_live.merge(uuid, 1, Integer::sum);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector right = forward.getCrossProduct(new Vector(0, 1, 0));
        if(right.lengthSquared() < 1e-6){
            double yaw = Math.toRadians(eye.getYaw());
            right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        }
        right.normalize();
        Vector up = right.getCrossProduct(forward).normalize();

        double tilt = Math.toRadians(config.f_Wave_Tilt);
        Vector back = right.clone().multiply(Math.cos(tilt)).subtract(up.clone().multiply(Math.sin(tilt))).normalize();
        Vector slash = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        List<Vector> sides = cross ? List.of(back, slash) : List.of(slot == 0 ? back : slash);

        double damage = config.f_Skill_Damage * (cross ? config.f_Cross_Rate : 1.0);
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        world.playSound(eye, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.4f);
        if(cross) world.playSound(eye, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.6f);

        Location origin = eye.clone().add(0, -0.1, 0).add(forward.clone().multiply(0.6));

        new BukkitRunnable() {
            int tick = 0;
            Location center = origin.clone();

            @Override
            public void run() {
                if(tick >= config.f_Wave_Ticks || !player.isOnline() || !config.f_using.getOrDefault(uuid, false)){
                    end();
                    return;
                }

                double step = config.f_Wave_Speed;
                RayTraceResult block = world.rayTraceBlocks(center, forward, step, FluidCollisionMode.NEVER, true);
                if(block != null) step = block.getHitPosition().distance(center.toVector());

                Location next = center.clone().add(forward.clone().multiply(step));
                LivingEntity target = sweep(player, next, forward, sides, step);
                draw(world, next, forward, sides);

                if(target != null){
                    hit(player, target, damage, source, slot, cross);
                    end();
                    return;
                }

                if(block != null){
                    world.spawnParticle(Particle.SMOKE, next, 10, 0.2, 0.2, 0.2, 0.02);
                    world.spawnParticle(Particle.DUST, next, 8, 0.25, 0.25, 0.25, 0, DEEP);
                    world.playSound(next, Sound.BLOCK_CHAIN_HIT, 0.6f, 1.2f);
                    end();
                    return;
                }

                center = next;
                tick++;
            }

            private void end(){
                config.f_wave_live.computeIfPresent(uuid, (k, v) -> v > 1 ? v - 1 : null);
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity sweep(Player player, Location center, Vector forward, List<Vector> sides, double step){
        double radius = config.f_Wave_Radius;
        double half = Math.toRadians(config.f_Wave_Angle / 2.0);
        double reach = radius + step + 1.5;
        int samples = Math.max(1, (int) Math.ceil(step / 0.4));

        LivingEntity best = null;
        double bestDepth = Double.MAX_VALUE;

        for(Entity entity : center.getWorld().getNearbyEntities(center, reach, reach, reach)){
            if(!attack.isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;

            BoundingBox box = target.getBoundingBox();
            Vector to = box.getCenter().subtract(center.toVector());

            for(Vector side : sides){
                Vector normal = forward.getCrossProduct(side).normalize();
                if(Math.abs(to.dot(normal)) > config.f_Wave_Band + extent(box, normal)) continue;

                double width = Math.max(extent(box, side), extent(box, forward));
                double x = to.dot(side);
                double depth = -1;

                for(int i = samples; i >= 0; i--){
                    double back = step * i / samples;
                    double y = to.dot(forward) + back;
                    double distance = Math.sqrt(x * x + y * y);
                    if(distance > radius + width || distance < radius - config.f_Wave_Thickness - width) continue;
                    double allowance = distance > width ? Math.asin(Math.min(1.0, width / distance)) : Math.PI;
                    if(Math.abs(Math.atan2(x, y)) > half + allowance) continue;
                    depth = step - back;
                    break;
                }

                if(depth < 0) continue;
                if(depth < bestDepth){
                    bestDepth = depth;
                    best = target;
                }
                break;
            }
        }
        return best;
    }

    private double extent(BoundingBox box, Vector axis){
        return box.getWidthX() / 2.0 * Math.abs(axis.getX())
                + box.getHeight() / 2.0 * Math.abs(axis.getY())
                + box.getWidthZ() / 2.0 * Math.abs(axis.getZ());
    }

    private void hit(Player player, LivingEntity target, double damage, DamageSource source, int slot, boolean cross){
        UUID uuid = player.getUniqueId();

        LivingEntity[] targets = config.f_wave_target.get(uuid);
        Location[] marks = config.f_wave_mark.get(uuid);
        Location spot = aimPoint(target);
        if(targets != null && marks != null){
            targets[slot] = target;
            marks[slot] = spot;
            if(cross){
                targets[1] = target;
                marks[1] = spot;
                config.f_wave_cross.add(uuid);
            }
        }

        config.f_damaging.put(uuid, true);
        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
        forceDamage.applyEffect(player);
        config.f_damaging.remove(uuid);

        World world = target.getWorld();
        world.spawnParticle(Particle.DUST, spot, 14, 0.25, 0.35, 0.25, 0, EDGE);
        world.spawnParticle(Particle.DUST, spot, 14, 0.3, 0.4, 0.3, 0, CORE);
        world.spawnParticle(Particle.SWEEP_ATTACK, spot, cross ? 2 : 1, 0.1, 0.1, 0.1, 0);
        world.spawnParticle(Particle.CRIT, spot, 8, 0.2, 0.3, 0.2, 0.3);
        world.playSound(spot, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        world.playSound(spot, Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.6f);
    }

    private void draw(World world, Location center, Vector forward, List<Vector> sides){
        double radius = config.f_Wave_Radius;
        double half = Math.toRadians(config.f_Wave_Angle / 2.0);
        int steps = Math.max(8, (int) Math.ceil(2 * half * radius / 0.12));

        for(Vector side : sides){
            for(int i = 0; i <= steps; i++){
                double a = -half + 2 * half * i / steps;
                double taper = Math.cos(Math.PI / 2 * a / half);
                double depth = config.f_Wave_Thickness * taper;
                Vector dir = forward.clone().multiply(Math.cos(a)).add(side.clone().multiply(Math.sin(a)));

                world.spawnParticle(Particle.DUST, center.clone().add(dir.clone().multiply(radius)), 1, 0, 0, 0, 0, EDGE);
                if(taper > 0.2){
                    world.spawnParticle(Particle.DUST, center.clone().add(dir.clone().multiply(radius - depth * 0.35)), 1, 0, 0, 0, 0, EDGE);
                }
                if(taper > 0.35){
                    world.spawnParticle(Particle.DUST, center.clone().add(dir.clone().multiply(radius - depth * 0.65)), 1, 0, 0, 0, 0, CORE);
                }
                if(taper > 0.55 && i % 2 == 0){
                    world.spawnParticle(Particle.DUST, center.clone().add(dir.clone().multiply(radius - depth)), 1, 0, 0, 0, 0, DEEP);
                }
                if(i % 5 == 0){
                    world.spawnParticle(Particle.SMOKE, center.clone().add(dir.clone().multiply(radius - depth - 0.2)), 1, 0.03, 0.03, 0.03, 0);
                }
            }
        }
    }

    private Location aimPoint(LivingEntity target){
        return target.getLocation().add(0, target.getHeight() * 0.6, 0);
    }

    public void X_finish(Player player){
        UUID uuid = player.getUniqueId();

        ArrayList<LivingEntity> objects = config.dash_object.get(uuid);
        ArrayList<Location> marks = config.dash_mark.get(uuid);
        if(objects == null || marks == null || objects.isEmpty() || marks.isEmpty()){
            config.dash_object.remove(uuid);
            config.dash_mark.remove(uuid);
            config.dash_power.remove(uuid);
            config.f_dash.remove(uuid);
            cool.updateCooldown(player, "F", 10000L);
            return;
        }

        LivingEntity target = objects.removeFirst();
        Location mark = marks.removeFirst();
        double power = config.dash_power.getOrDefault(uuid, 1.0);

        swingEchoGuard.swingMainHand(player);
        if(attack.isDual(player)) swingEchoGuard.swingOffHand(player);

        Location startLocation = player.getLocation();
        Location goal = (target.isValid() && !target.isDead() && target.getWorld() == startLocation.getWorld())
                ? aimPoint(target)
                : mark;

        Vector direction = startLocation.getDirection();
        if(goal != null && goal.getWorld() == startLocation.getWorld()){
            Vector aim = goal.toVector().subtract(startLocation.clone().add(0, player.getHeight() * 0.5, 0).toVector());
            if(aim.lengthSquared() > 1e-4) direction = aim;
        }
        direction = direction.normalize().multiply(config.f_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        player.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1.0, 0), 7, 0.1, 0.2, 0.1, 0.5);

        Invulnerable invulnerable = new Invulnerable(player, 500);
        invulnerable.applyEffect(player);

        detect(player, target, power);

        Title title = Title.title(
                Component.empty(),
                Component.text(objects.size()).color(NamedTextColor.DARK_RED),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
        );
        player.showTitle(title);

        if(objects.isEmpty()){
            config.dash_object.remove(uuid);
            config.dash_mark.remove(uuid);
            config.dash_power.remove(uuid);
            config.f_dash.remove(uuid);

            Title overTitle = Title.title(
                    Component.empty(),
                    Component.text("over").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(overTitle);

            cool.updateCooldown(player, "F", 10000L);
        }else{
            cool.updateCooldown(player, "F", 50L);
        }
    }

    public void detect(Player player, LivingEntity target, double power){
        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage_2 * (1 + amp) * power;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private int ticks = 0;
            private Vector last = body(player);

            @Override
            public void run() {

                if (ticks > config.f_Dash_Ticks || player.isDead() || !player.isOnline()
                        || !target.isValid() || target.isDead() || target.getWorld() != player.getWorld()) {
                    this.cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 7, 0.4, 0.4, 0.4, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 20, 0.5, 0.1, 0.5, 0);

                Vector now = body(player);
                Vector path = now.clone().subtract(last);
                double length = path.length();

                BoundingBox zone = target.getBoundingBox().clone().expand(
                        player.getWidth() / 2.0 + 0.3, player.getHeight() / 2.0 + 0.3, player.getWidth() / 2.0 + 0.3);
                boolean touched = zone.contains(now)
                        || (length > 1e-4 && zone.rayTrace(last, path.normalize(), length) != null);

                if (touched) {
                    player.setVelocity(new Vector(0, 0, 0));

                    config.f_damaging.put(player.getUniqueId(), true);
                    bloodStroker.damageStroke(player, target);
                    ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                    forceDamage.applyEffect(player);
                    config.f_damaging.remove(player.getUniqueId());

                    Location spot = aimPoint(target);
                    World world = target.getWorld();
                    world.spawnParticle(Particle.DUST, spot, 18, 0.3, 0.4, 0.3, 0, EDGE);
                    world.spawnParticle(Particle.DUST, spot, 18, 0.35, 0.45, 0.35, 0, CORE);
                    world.spawnParticle(Particle.SWEEP_ATTACK, spot, power > 1.0 ? 2 : 1, 0.15, 0.15, 0.15, 0);
                    world.playSound(spot, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.9f);
                    world.playSound(spot, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);

                    this.cancel();
                    return;
                }

                last = now;
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private Vector body(Player player){
        return player.getLocation().add(0, player.getHeight() / 2.0, 0).toVector();
    }
}