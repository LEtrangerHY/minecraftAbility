package org.core.coreSystem.cores.VOL2.Stroke.coreSystem;

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
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.ArrayList;
import java.util.List;

public class strInventory extends absInventory {

    private final Core plugin;

    public strInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Stroke.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.STICK;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("Stroke");
            case "R" -> Component.text("HomeRun");
            case "Q" -> Component.text("Base");
            case "F" -> Component.text("RUN");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.WIND_CHARGE;
            case "Q" -> Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
            case "F" -> Material.FEATHER;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 기동형 브루저").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 메인핸드에 막대기 장착, 오프핸드는 장착 금지.").color(NamedTextColor.LIGHT_PURPLE));
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
                lore.add(Component.text("시스템 : 차징").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 최대 1.5초간 우클릭을 꾹 눌러 게이지를 차징한 후, 떼면 부채꼴 타격을 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("차징 시간에 비례하여 위력과 넉백 거리가 대폭 증가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("질주 : 차징 없이 전방으로 돌진하며 70% 위력의 즉발 스윙을 가한다.").color(NamedTextColor.LIGHT_PURPLE));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 기동 및 생존").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 커맨드").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 1초간 이동속도가 90% 감소하며 커맨드 입력 대기 상태에 돌입한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("커맨드 : 대기 중 W, A, S, D를 입력해 입력한 방향으로 무적 돌진을 사용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("홈스틸 : W -> A -> S -> D 순서로 정확히 연속 입력 성공").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("홈스틸 성공 시 전체 체력의 25%를 회복하고, 남은 체력의 25%만큼 흡수 효과를 부여받는다.").color(NamedTextColor.LIGHT_PURPLE));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 버프").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 10초 동안 유지되는 질주 상태에 돌입한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("유지 시간 동안 R 스킬이 즉발 돌진 타격으로 변형된다.").color(NamedTextColor.LIGHT_PURPLE));
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
            case "R": requireExpList = requireExpOfR; hpBonus = 2; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; hpBonus = 2; break;
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