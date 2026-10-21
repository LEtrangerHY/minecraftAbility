package org.core.coreSystem.cores.VOL2.Cheshire.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;

import java.util.ArrayList;
import java.util.List;

public class chesInventory extends absInventory {

    private final Core plugin;

    public chesInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Cheshire.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.NETHERITE_HOE;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("CHESHIRE");
            case "R" -> Component.text("Scratch/PhantomScythe");
            case "Q" -> Component.text("Bite/LethalBite");
            case "F" -> Component.text("GRIN WITHOUT A CAT");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.NETHERITE_HOE;
            case "Q" -> Material.FERMENTED_SPIDER_EYE;
            case "F" -> Material.ENDER_EYE;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 기동형 암살자").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 메인핸드에 네더라이트 괭이 장착, 오프핸드는 장착 금지.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 무기 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
            return lore;
        }

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
                lore.add(Component.text("시스템 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 전방 부채꼴 범위에 할퀴기 피해를 입힌다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("강화 : 투명화 상태에서 시전 시, 은신이 풀리며 돌진 후 360도 회전베기를 시전한다.").color(NamedTextColor.LIGHT_PURPLE));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 단일 타격 및 4초간 이속 60% 감소와 지속 피해를 부여한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("강화 : 미소 존재 시, 본체와 함께 미소의 위치에서도 시전되며 대상의 남은 체력 13% 마법 피해를 추가로 입힌다.").color(NamedTextColor.LIGHT_PURPLE));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 효과").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 6초 쿨타임으로 전방 짧은 대시를 사용한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("쿨타임 셋 : 시전 즉시 2초 투명화 및 8초 유지시간 동안 이속 60% 대폭 증가.").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("유지 시간 중 R 스킬 적중 시 적 위치에 미소를 1개 생성하며 최대 2개 까지 생성 가능하다.").color(NamedTextColor.AQUA));
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
        if (skill.equals("main")) return 0L;
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
        long hpBonus = 0;
        switch (skill) {
            case "R": requireExpList = requireExpOfR; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; hpBonus = 3; break;
            default: return;
        }

        int requiredExp = Math.toIntExact(requireExpList.get(Math.toIntExact(skillLevel)));
        int totalExp = player.getTotalExperience();

        if (totalExp >= requiredExp) {
            deductExp(player, requiredExp);
            if (hpBonus > 0) applyAdditionalHealth(player, hpBonus);

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