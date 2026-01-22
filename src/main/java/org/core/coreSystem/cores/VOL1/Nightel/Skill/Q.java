package org.core.coreSystem.cores.VOL1.Nightel.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Nightel.Passive.Chain;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.Nightel;

import java.util.*;

public class Q implements SkillBase {

    private final Nightel config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Chain chain;
    private final NamespacedKey keyQ;

    public Q(Nightel config, JavaPlugin plugin, Cool cool, Chain chain) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chain = chain;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        performTeleport(player);
    }

    private void performTeleport(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();

        double safeDistance = -1;

        for (double d = 6.0; d >= 0; d -= 0.2) {
            double x = start.getX() + direction.getX() * d;
            double y = start.getY() + direction.getY() * d;
            double z = start.getZ() + direction.getZ() * d;

            Block feetBlock = new Location(world, x, y, z).getBlock();
            Block headBlock = feetBlock.getRelative(0, 1, 0);

            if (feetBlock.isPassable() && headBlock.isPassable()) {
                safeDistance = d;
                break;
            }
        }

        if (safeDistance >= 0) {
            UUID uuid = player.getUniqueId();
            String lastSkill = config.chainSkill.getOrDefault(uuid, "");
            boolean same = config.chainSkill.containsKey(uuid) && lastSkill.equals("Q");

            chain.chainCount(player, config.q_Skill_Cool, "Q");

            Location targetLocation = start.clone().add(direction.clone().multiply(safeDistance));
            targetLocation.setDirection(start.toVector().subtract(targetLocation.toVector()));

            player.teleport(targetLocation);
            world.spawnParticle(Particle.SPIT, player.getLocation(), 33, 0.2, 0.3, 0.2, 0.6);

            performSlash(player, safeDistance, start, direction, same);
        } else {
            world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            player.sendActionBar(Component.text("failed").color(NamedTextColor.RED));
        }
    }

    private void performSlash(Player player, double maxDistance, Location start, Vector direction, boolean same) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        Set<Entity> damagedSet = new HashSet<>();
        config.damaged_1.put(uuid, damagedSet);

        int atk = same ? 6 : 3;
        double amp = config.q_SKill_amp * player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location end = start.clone().add(direction.clone().multiply(maxDistance));
        BoundingBox box = BoundingBox.of(start, end).expand(1.8);

        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof LivingEntity target && entity != player) {
                if (!damagedSet.contains(target)) {
                    Vector toEntity = target.getLocation().toVector().subtract(start.toVector());
                    double dot = toEntity.dot(direction);

                    if (dot >= -0.5 && dot <= maxDistance + 0.5) {
                        Vector projection = direction.clone().multiply(dot);
                        if (toEntity.subtract(projection).lengthSquared() <= 3.24) {
                            targets.add(target);
                            damagedSet.add(target);
                        }
                    }
                }
            }
        }

        if (!targets.isEmpty()) {
            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick >= atk || player.isDead()) {
                        this.cancel();
                        return;
                    }

                    for (LivingEntity target : targets) {
                        if (target.isValid()) {
                            Location tLoc = target.getLocation();
                            world.playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                            world.playSound(tLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1f);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source);
                            forceDamage.applyEffect(player);
                            target.setVelocity(new Vector(0, 0, 0));

                            Location effectLoc = tLoc.add(0, 1.2, 0);
                            world.spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 3, 0.6, 0.6, 0.6, 1);
                            if (same) {
                                world.spawnParticle(Particle.ENCHANTED_HIT, effectLoc, 11, 0.6, 0.6, 0.6, 1);
                            }
                        }
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 2L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
            }, atk * 2L + 1);
        } else {
            world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
        }
    }
}