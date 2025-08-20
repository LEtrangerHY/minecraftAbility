package org.core.coreProgram.Cores.Blaze.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.Debuff.Burn;
import org.core.Effect.ForceDamage;
import org.core.coreConfig;
import org.core.coreProgram.Abs.ConfigWrapper;
import org.core.coreProgram.Abs.SkillBase;
import org.core.coreProgram.Abs.absCore;
import org.core.coreProgram.Cores.Blaze.Passive.BlueFlame;
import org.core.coreProgram.Cores.Blaze.Skill.F;
import org.core.coreProgram.Cores.Blaze.Skill.Q;
import org.core.coreProgram.Cores.Blaze.Skill.R;

import java.util.HashSet;

import static org.bukkit.Bukkit.getLogger;

public class blazeCore extends absCore {
    private final Core plugin;
    private final Blaze config;

    public final BlueFlame blueFlame;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;


    public blazeCore(Core plugin, coreConfig tag, Blaze config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.blueFlame = new BlueFlame();

        this.Rskill = new R(config, plugin, cool, blueFlame);
        this.Qskill = new Q(config, plugin, cool, blueFlame);
        this.Fskill = new F(config, plugin, cool, blueFlame);


        getLogger().info("Blaze downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if (tag.Blaze.contains(event.getPlayer())) {
            if (!skillUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "blaze")) {
                            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 4000L, "blaze");

                        World world = player.getWorld();
                        Location origin = player.getLocation().add(0, 1.3, 0);
                        Vector forward = origin.getDirection().normalize();

                        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.25);
                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

                        double maxDistance = 9.0;
                        double coneAngle = 60.0;

                        new BukkitRunnable() {
                            double distance = 1.0;

                            @Override
                            public void run() {
                                if (distance > maxDistance || !player.isOnline()) {
                                    config.damaged.remove(player.getUniqueId());
                                    cancel();
                                    return;
                                }

                                for (double angle = -coneAngle / 2; angle <= coneAngle / 2; angle += 8) {
                                    Vector dir = forward.clone();
                                    dir.rotateAroundY(Math.toRadians(angle));
                                    Location particleLoc = origin.clone().add(dir.multiply(distance));

                                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 4, 0.2, 0.1, 0.2, 0.04);

                                    for (Entity entity : world.getNearbyEntities(particleLoc, 0.6, 0.6, 0.6)) {
                                        if (entity instanceof LivingEntity && entity != player) {

                                            LivingEntity target = (LivingEntity) entity;
                                            ForceDamage forceDamage = new ForceDamage(target, 0.4);
                                            forceDamage.applyEffect(player);

                                            if (Math.random() < 0.4) {
                                                Burn burn = new Burn(target, 4000L);
                                                burn.applyEffect(player);
                                            }

                                            world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                                        }
                                    }
                                }

                                distance += 0.4;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                        event.setCancelled(true);
                    }
                } else {
                    player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0);
                }
            } else {
                skillUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Blaze.contains(player);
    }

    @Override
    protected SkillBase getRSkill() {
        return Rskill;
    }

    @Override
    protected SkillBase getQSkill() {
        return Qskill;
    }

    @Override
    protected SkillBase getFSkill() {
        return Fskill;
    }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.SOUL_TORCH && (off.getType() == Material.SOUL_SAND || off.getType() == Material.SOUL_SOIL || off.getType() == Material.SOUL_LANTERN);
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isRCondition(Player player) {
        return canUseRSkill(player);
    }

    @Override
    protected boolean isQCondition(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.SOUL_TORCH &&
                (off.getType() == Material.SOUL_SAND || off.getType() == Material.SOUL_SOIL || off.getType() == Material.SOUL_LANTERN) &&
                canUseQSkill(player);
    }

    @Override
    protected boolean isFCondition(Player player) {
        return canUseFSkill(player);
    }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
            }

            @Override
            public void cooldownReset(Player player) {
                cool.updateCooldown(player, "R", config.frozenCool);
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getRcooldown(Player player) {
                return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool);
            }

            @Override
            public long getQcooldown(Player player) {
                return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(), config.q_Skill_Cool);
            }

            @Override
            public long getFcooldown(Player player) {
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
            }
        };
    }
}
