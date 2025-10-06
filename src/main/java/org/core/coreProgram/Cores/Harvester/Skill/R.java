package org.core.coreProgram.Cores.Harvester.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Harvester.coreSystem.Harvester;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class R implements SkillBase {

    public final Harvester config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Harvester config, JavaPlugin plugin, Cool cool){
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        player.swingMainHand();
        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        Boolean invisibility = player.isInvisible();
        double slashLength = (player.isInvisible()) ? 4.4 : 6.6;
        double maxAngle = (player.isInvisible()) ? Math.toRadians(100) : Math.toRadians(26);
        long tickDelay = 0L;
        double maxTicks = (player.isInvisible()) ? 6 : 4;
        double innerRadius = 2.2;

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        damage = (player.isInvisible()) ? damage * 4 : damage;

        Location origin = player.getEyeLocation().add(0, -0.7, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(108, 108, 44), 0.6f);
        Particle.DustOptions dustOption_slash_gra = new Particle.DustOptions(Color.fromRGB(166, 166, 88), 0.6f);

        double finalDamage = damage;

        config.rskill_using.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(invisibility) player.sendActionBar(Component.text("Invisible shot!").color(NamedTextColor.YELLOW));

                if (ticks >= maxTicks || player.isDead()) {

                    config.rskill_using.remove(player.getUniqueId());

                    config.damaged.remove(player.getUniqueId());

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


                        if (distanceFromOrigin >= innerRadius) {
                            if (Math.random() < 0.26) {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                            }else{
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash_gra);
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.4, 0.4, 0.4)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                                config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                                if(invisibility) {
                                    world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.3, 0), 26, 0.4, 0.4, 0.4, 1);
                                    world.playSound(target.getLocation().clone().add(0, 1.3, 0), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);
                                }
                                ForceDamage forceDamage = new ForceDamage(target, finalDamage);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));
                            }
                        }

                        Block blockDown = particleLocation.clone().getBlock();
                        Block blockUp = particleLocation.clone().add(0, 1, 0).getBlock();
                        if(blockDown.getType() == Material.SHORT_GRASS || blockDown.getType() == Material.TALL_GRASS || blockDown.getType() == Material.WHEAT || blockDown.getType() == Material.POTATOES || blockDown.getType() == Material.CARROTS || blockDown.getType() == Material.BEETROOTS) {
                            breakBlockSafely(player, blockDown);
                        }
                        if(blockUp.getType() == Material.SHORT_GRASS || blockUp.getType() == Material.TALL_GRASS || blockUp.getType() == Material.WHEAT || blockUp.getType() == Material.POTATOES || blockUp.getType() == Material.CARROTS || blockUp.getType() == Material.BEETROOTS) {
                            breakBlockSafely(player, blockDown);
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
                Particle.BLOCK,
                block.getLocation().add(0.5, 0.5, 0.5),
                6,
                0.3, 0.3, 0.3,
                block.getBlockData()
        );

        block.breakNaturally(new ItemStack(Material.IRON_HOE));
    }
}
