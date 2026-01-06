package org.core.coreSystem.cores.VOL1.Blaze.coreSystem;

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
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;

import java.util.ArrayList;
import java.util.List;

public class blazeInventory extends absInventory {

    private final Core plugin;

    public blazeInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Blaze.contains(player);
    }

    @Override
    protected boolean isCoreItemClicked(Player player, ItemStack clicked){
        return clicked.getType() == Material.SOUL_TORCH;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "R" -> Component.text("Shooter");
            case "Q" -> Component.text("Burstblazer");
            case "F" -> Component.text("DIABOLICA");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.FIRE_CHARGE;
            case "Q" -> Material.SOUL_LANTERN;
            case "F" -> Material.WITHER_SKELETON_SKULL;
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
                lore.add(Component.text("대상 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("선딜레이 후 화염구 7개를 투사한다.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 지속시간").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 플레이어").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("지속시간 : 13초 동안 시듦 피해를 입는 대신, 일반 공격의 방사 범위를 360°로 확장시키고, 피해량을 150% 상승시킨다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 장판").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 플레이어/적 오브젝트/바이옴").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("바이옴 : 범위 내의 바이옴을 [창백한 정원] 으로 변경한다").color(NamedTextColor.GREEN));
                lore.add(Component.text("플레이어 : 6초 동안 시듦 피해를 입고, 13초 동안 이동 속도가 45% 증가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("적 오브젝트 : 발 밑에 불기둥을 생성하여 피해를 가한다.").color(NamedTextColor.GREEN));
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

    public List<Long> requireExpOfR = List.of(44L, 64L, 108L, 204L, 334L, 664L);
    public List<Long> requireExpOfQ = List.of(44L, 64L, 108L, 204L, 334L, 664L);
    public List<Long> requireExpOfF = List.of(44L, 64L, 108L, 204L, 334L, 664L);

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
            case "R": requireExpList = requireExpOfR; applyAdditionalHealth(player, 1); break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; break;
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
