package org.core.coreSystem.cores.VOL3.Stroke.Skill;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Stroke.coreSystem.Stroke;
import org.core.effect.crowdControl.Invulnerable;

import java.util.UUID;

public class Q implements SkillBase {
    private final Stroke config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Stroke config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    //Q
    @Override
    public void Trigger(Player player) {
        cool.setCooldown(player, config.q_Skill_Cool, "Q");
        startBaseLoader(player, 0);
    }

    private void startBaseLoader(Player player, int step) {
        UUID uuid = player.getUniqueId();

        config.qStep.put(uuid, step);
        config.qInputDelay.put(uuid, System.currentTimeMillis() + 400);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false, false));

        String title = switch (step) {
            case 0 -> "BASE 1 [W]";
            case 1 -> "BASE 2 [A]";
            case 2 -> "BASE 3 [S]";
            case 3 -> "HOME RUN [D]";
            default -> "BASE";
        };

        BarColor color = switch (step) {
            case 0 -> BarColor.WHITE;
            case 1 -> BarColor.GREEN;
            case 2 -> BarColor.BLUE;
            case 3 -> BarColor.RED;
            default -> BarColor.WHITE;
        };

        BossBar bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        config.qBars.put(uuid, bar);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 1.0f + (step * 0.2f));

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    endSkillState(player);
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    endSkillState(player);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    dashToPoint(player, "W", -1);
                    this.cancel();
                    return;
                }

                bar.setProgress(1.0 - ((double) ticks / maxTicks));
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        config.qTasks.put(uuid, task);
    }

    public void startMovementSampling(Player player, Location startLoc) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !config.qStep.containsKey(uuid)) {
                    config.qIsSampling.put(uuid, false);
                    return;
                }

                Location currentLoc = player.getLocation();
                Vector movement = currentLoc.toVector().subtract(startLoc.toVector()).setY(0);

                config.qIsSampling.put(uuid, false);

                if (movement.lengthSquared() < 0.0001) return;

                Vector forward = startLoc.getDirection().setY(0).normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

                double f = movement.dot(forward);
                double r = movement.dot(right);

                String input;
                if (Math.abs(f) > Math.abs(r)) {
                    input = f > 0 ? "W" : "S";
                } else {
                    input = r > 0 ? "D" : "A";
                }

                handleMovement(player, input);
            }
        }.runTaskLater(plugin, 2L);
    }

    private void handleMovement(Player player, String input) {
        UUID uuid = player.getUniqueId();
        if (!config.qStep.containsKey(uuid)) return;

        int step = config.qStep.get(uuid);
        boolean isCorrectSequence = false;

        if (step == 0 && input.equals("W")) isCorrectSequence = true;
        else if (step == 1 && input.equals("A")) isCorrectSequence = true;
        else if (step == 2 && input.equals("S")) isCorrectSequence = true;
        else if (step == 3 && input.equals("D")) isCorrectSequence = true;

        endSkillState(player);

        if (isCorrectSequence) {
            dashToPoint(player, input, step + 1);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            dashToPoint(player, input, -1);
        }
    }

    private void dashToPoint(Player player, String input, int nextStep) {
        Vector forward = player.getLocation().getDirection().setY(0).normalize();
        Vector dir = switch (input) {
            case "W" -> forward;
            case "A" -> new Vector(forward.getZ(), 0, -forward.getX()).normalize();
            case "S" -> forward.clone().multiply(-1);
            case "D" -> new Vector(-forward.getZ(), 0, forward.getX()).normalize();
            default -> forward;
        };

        double maxDist = 7.0;
        Location start = player.getLocation();

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                start.clone().add(0, 0.5, 0), dir, maxDist, FluidCollisionMode.NEVER, true
        );

        double dist;
        if (result != null && result.getHitBlock() != null) {
            dist = Math.max(0, start.distance(result.getHitPosition().toLocation(player.getWorld())) - 0.8);
        } else {
            dist = maxDist;
        }

        Location targetLoc = start.clone().add(dir.clone().multiply(dist));

        new Invulnerable(player, 15L).applyEffect(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                double currentDist = player.getLocation().distanceSquared(targetLoc);

                if (currentDist < 1.0 || ticks > 10 || dist <= 0.1) {
                    player.setVelocity(new Vector(0, -0.1, 0));
                    this.cancel();

                    if (nextStep != -1) {
                        if (nextStep == 4) {
                            doHomeSteal(player);
                        } else {
                            startBaseLoader(player, nextStep);
                        }
                    }
                    return;
                }

                player.setVelocity(dir.clone().multiply(1.5).setY(0.1));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 5, 0.2, 0.1, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void doHomeSteal(Player player) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHp = maxHealthAttr.getValue();
        double healAmount = maxHp * 0.25;

        double currentHp = player.getHealth();
        double newHp = Math.min(maxHp, currentHp + healAmount);

        player.setHealth(newHp);

        if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            double shieldAmount = newHp * 0.25;
            double currentAbsorption = player.getAbsorptionAmount();
            double targetAbsorption = currentAbsorption + shieldAmount;

            int amplifier = Math.max(0, (int) Math.ceil(targetAbsorption / 4.0) - 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, PotionEffect.INFINITE_DURATION, amplifier, false, false, false));

            player.setAbsorptionAmount(targetAbsorption);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.2);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    private void endSkillState(Player player) {
        UUID uuid = player.getUniqueId();

        if (config.qTasks.containsKey(uuid)) {
            config.qTasks.get(uuid).cancel();
            config.qTasks.remove(uuid);
        }
        if (config.qBars.containsKey(uuid)) {
            config.qBars.get(uuid).removeAll();
            config.qBars.remove(uuid);
        }

        config.qStep.remove(uuid);
        config.qIsSampling.put(uuid, false);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }
}