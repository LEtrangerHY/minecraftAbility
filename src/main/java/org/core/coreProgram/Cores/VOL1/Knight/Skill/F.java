package org.core.coreProgram.Cores.VOL1.Knight.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Stun;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL1.Knight.coreSystem.Knight;

import java.util.HashSet;
import java.util.Set;

public class F implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Knight config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        double slashLength = 7.7;
        double maxAngle = Math.toRadians(70);
        long tickDelay = 0L;
        int maxTicks = 5;
        double innerRadius = 3.3;

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        Location origin = player.getEyeLocation().add(0, -0.4, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.7f);
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemMeta meta = mainHand.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.Damageable && mainHand.getType().getMaxDurability() > 0) {
                        org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                        int newDamage = damageable.getDamage() + 77;
                        damageable.setDamage(newDamage);
                        mainHand.setItemMeta(meta);

                        if (newDamage >= mainHand.getType().getMaxDurability()) {
                            player.getInventory().setItemInMainHand(null);
                        }
                    }
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(2)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = origin.clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(origin);

                        for(int i = -1; i < 2; i++) {
                            Block block = particleLocation.clone().add(0, i, 0).getBlock();
                            if(block.getType() != Material.AIR && block.getType() != Material.WATER && block.getType() != Material.LAVA) {
                                breakBlockSafely(player, block);
                            }
                        }

                        if (distanceFromOrigin >= innerRadius) {
                            if(Math.random() < 0.17){
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions_gra);
                            }else{
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions);
                            }

                            for (Entity entity : world.getNearbyEntities(particleLocation, 0.7, 0.7, 0.7)) {
                                if (entity instanceof LivingEntity target && entity != player && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                                    config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 1);
                                    world.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 1);
                                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().clone().add(0, 1.3, 0), 20, 0.4, 0.4, 0.4, 1);
                                    Stun stun = new Stun(target, 700);
                                    stun.applyEffect(player);

                                    ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                    forceDamage.applyEffect(player);
                                    target.setVelocity(new Vector(0, 0, 0));
                                }
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, tickDelay, 1L);
    }

    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW
    );

    public void breakBlockSafely(Player player, Block block) {
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
