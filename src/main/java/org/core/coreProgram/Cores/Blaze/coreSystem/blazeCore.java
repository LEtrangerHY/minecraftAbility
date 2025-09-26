package org.core.coreProgram.Cores.Blaze.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.Debuff.Burn;
import org.core.Effect.ForceDamage;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.Blaze.Passive.BlueFlame;
import org.core.coreProgram.Cores.Blaze.Skill.F;
import org.core.coreProgram.Cores.Blaze.Skill.Q;
import org.core.coreProgram.Cores.Blaze.Skill.R;

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
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;

            maxHealth.setBaseValue(newMax);

            if (healFull) {
                player.setHealth(newMax);
            } else if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }
    }

    @EventHandler
    public void projectileEvent(ProjectileHitEvent event) {
        Entity victim = event.getHitEntity();
        Projectile fireball = event.getEntity();

        if (!(fireball.getShooter() instanceof Player shooter)) {
            return;
        }

        if (fireball.getType() == EntityType.FIREBALL || tag.Blaze.contains(shooter)) {
            if (victim instanceof LivingEntity livingVictim) {
                if (Math.random() < 0.6) {
                    Burn burn = new Burn(livingVictim, 4000L);
                    burn.applyEffect(shooter);

                    PotionEffect wither = new PotionEffect(
                            PotionEffectType.WITHER,
                            20 * 4,
                            3,
                            false,
                            false
                    );
                    livingVictim.addPotionEffect(wither);
                }
            }
        }
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

                        double damage = 0.4;

                        if(config.BurstBlaze.getOrDefault(player.getUniqueId(), false)){
                            double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
                            damage = 0.4 * (1 + amp);
                            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.5);
                            cool.setCooldown(player, 2000L, "blaze");
                        }else {
                            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.25);
                            cool.setCooldown(player, 4000L, "blaze");
                        }

                        World world = player.getWorld();
                        Location origin = player.getLocation().add(0, 1.3, 0);
                        Vector forward = origin.getDirection().normalize();

                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

                        double maxDistance = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? 11.0 : 9.0;
                        double coneAngle = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? 360.0 : 60.0;

                        if(config.BurstBlaze.getOrDefault(player.getUniqueId(), false)){
                            player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 0.6, 0), 130, 0.1, 0.1, 0.1, 0.8);
                            for (Entity entity : world.getNearbyEntities(player.getLocation(), 13, 13, 13)) {
                                if (entity instanceof LivingEntity target && entity != player) {

                                    if (Math.random() < 0.4) {
                                        Burn burn = new Burn(target, 2000L);
                                        burn.applyEffect(player);
                                        PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 40, 3, false, false);
                                        ((LivingEntity) target).addPotionEffect(wither);
                                    }

                                    world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                                }
                            }
                        }

                        double finalDamage = damage;

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
                                    Vector dir = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? forward.clone().setY(0).rotateAroundY(Math.toRadians(angle)): forward.clone().rotateAroundY(Math.toRadians(angle));
                                    Location particleLoc = origin.clone().add(dir.multiply(distance));

                                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 4, 0.2, 0.1, 0.2, 0.04);

                                    double dist = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? 2 : 0.6;

                                    for (Entity entity : world.getNearbyEntities(particleLoc, dist, dist, dist)) {
                                        if (entity instanceof LivingEntity && entity != player) {

                                            LivingEntity target = (LivingEntity) entity;
                                            ForceDamage forceDamage = new ForceDamage(target, finalDamage);
                                            forceDamage.applyEffect(player);

                                            double per = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? 1.0 : 0.4;
                                            long burnTime = (config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) ? 6000L : 4000L;

                                            if (Math.random() < per) {
                                                Burn burn = new Burn(target, burnTime);
                                                burn.applyEffect(player);
                                                PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, (int) (20 * (burnTime / 1000)), 3, false, false);
                                                ((LivingEntity) target).addPotionEffect(wither);
                                            }

                                            world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                                        }
                                    }

                                    if(config.BurstBlaze.getOrDefault(player.getUniqueId(), false)) {
                                        for (int i = -3; i < 4; i++) {
                                            Block block = particleLoc.clone().add(0, i, 0).getBlock();
                                            if (block.isBurnable() || block.getType() == Material.ICE || block.getType() == Material.SNOW || block.getType() == Material.BLUE_ICE || block.getType() == Material.FROSTED_ICE || block.getType() == Material.PACKED_ICE || block.getType() == Material.POWDER_SNOW || block.getType() == Material.SNOW_BLOCK) {
                                                if(block.getType() == Material.BLUE_ICE) {
                                                    if(Math.random() < 0.06) {
                                                        block.setType(Material.FIRE);
                                                    }
                                                }else{
                                                    block.setType(Material.FIRE);
                                                }
                                                if(Math.random() < 0.2) {
                                                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_BURN, 1, 1);
                                                }
                                            }
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
