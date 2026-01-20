package org.core.coreSystem.cores.VOL2.Lavender.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.ArrayList;
import java.util.List;

public class lavInventory extends absInventory {

    private final Core plugin;

    public lavInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Lavender.contains(player);
    }

    @Override
    protected boolean isCoreItemClicked(Player player, ItemStack clicked){
        return clicked.getType() == Material.IRON_SWORD;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "R" -> Component.text("Delusion");
            case "Q" -> Component.text("Rush");
            case "F" -> Component.text("Flowering");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.AMETHYST_CLUSTER;
            case "Q" -> Material.FEATHER;
            case "F" -> Material.AMETHYST_BLOCK;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();
        long level = getSkillLevel(player, skill);
        long playerLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        long maxLevel = switch ((int) playerLevel){
            case 6, 7, 8, 9 -> 5;
            case 10 -> 6;
            default -> 3;
        };

        lore.add(Component.text("Lv." + level + "/" + maxLevel).color(NamedTextColor.YELLOW));

        Component requireXp;

        switch (skill) {
            case "R":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfR.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트/지형").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("전방으로 찌르기 공격을 시전하며 칼날을 전개한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("전개되는 칼날이 벽에 충돌할 시 최대 6회까지 반사되며, 3초 후 전개된 칼날이 회수된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("칼날에 피격된 적에게 피해를 가하고 3초간 기절시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 전개되어있던 칼날을 회수한다").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 효과").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("전방으로 돌진하며, R 스킬 쿨타임을 초기화한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("칼날이 전개되어 있다면, 칼날의 회수하며 칼날이 반사된 횟수만큼 해당 스킬의 쿨타임을 감소한다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 효과").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("전방으로 돌진한 후 자신을 중심으로 6초 동안 유지되는 3*3*3 공간을 형성한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("공간 형성 후 자신 중심 반경 3칸 내의 대상을 자신의 좌표로 순간이동 시키고 3초간 고정시킨다.").color(NamedTextColor.GREEN));
                break;
            default:
                break;
        }

        lore.add(Component.text("------------").color(NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(Component.text("우클릭을 통해 강화").color(NamedTextColor.AQUA));

        return lore;
    }

    @Override
    protected Long getSkillLevel(Player player, String skill) {
        return player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, skill), PersistentDataType.LONG, 0L);
    }

    public List<Long> requireExpOfR = List.of(22L, 77L, 170L, 257L, 307L, 617L);
    public List<Long> requireExpOfQ = List.of(22L, 77L, 170L, 257L, 307L, 617L);
    public List<Long> requireExpOfF = List.of(22L, 77L, 170L, 257L, 307L, 617L);

    @Override
    protected void reinforceSkill(Player player, String skill, Long skillLevel, Inventory customInv) {
        if (skillLevel >= 6 || !contains(player)) return;

        long level = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        if (skillLevel == 3 && level < 6L){
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("승급 필요 : CORE LEVEL -> 6")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        if (skillLevel == 5 && level < 10L){
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("승급 필요 : CORE LEVEL -> 10")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        long current = player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, skill), PersistentDataType.LONG, 0L);

        List<Long> requireExpList;
        switch (skill) {
            case "R": requireExpList = requireExpOfR; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; applyAdditionalHealth(player, 2); break;
            default: return;
        }

        int requiredExp = Math.toIntExact(requireExpList.get(Math.toIntExact(skillLevel)));
        int totalExp = player.getTotalExperience();

        if (totalExp >= requiredExp) {
            deductExp(player, requiredExp);

            player.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, skill),
                    PersistentDataType.LONG,
                    current + 1
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 1);
            customInvReroll(player, customInv);
            player.sendMessage(
                    Component.text("스킬 레벨업 성공!")
                            .color(NamedTextColor.GREEN)
            );
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("경험치(Minecraft EXP) 부족 " + requiredExp + "Exp 필요")
                            .color(NamedTextColor.RED)
            );
        }
    }

    private void applyAdditionalHealth(Player player, long addHP) {

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;

            maxHealth.setBaseValue(newMax);

        }
    }

    private void deductExp(Player player, int expToDeduct) {
        int newTotalExp = player.getTotalExperience() - expToDeduct;
        if (newTotalExp < 0) newTotalExp = 0;
        player.setTotalExperience(newTotalExp);

        int level = 0;
        int remainingExp = newTotalExp;
        while (remainingExp >= getExpToNextLevel(level)) {
            remainingExp -= getExpToNextLevel(level);
            level++;
        }

        player.setLevel(level);
        if (level < 1000) {
            player.setExp(remainingExp / (float)getExpToNextLevel(level));
        } else {
            player.setExp(0);
        }
    }

    private int getExpToNextLevel(int level) {
        if (level >= 0 && level <= 15) return 2 * level + 7;
        else if (level >= 16 && level <= 30) return 5 * level - 38;
        else return 9 * level - 158;
    }


    @Override
    protected InventoryWrapper getInventoryWrapper() {
        return new InventoryWrapper() {

        };
    }
}
