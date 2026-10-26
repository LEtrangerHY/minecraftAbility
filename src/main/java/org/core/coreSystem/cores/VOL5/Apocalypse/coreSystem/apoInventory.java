package org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem;

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

public class apoInventory extends absInventory {

    private final Core plugin;

    public apoInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Apocalypse.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.WOODEN_SWORD;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("APOCALYPSE");
            case "R" -> Component.text("GaleStrike");
            case "Q" -> Component.text("Wallop");
            case "F" -> Component.text("REVELATION");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.WIND_CHARGE;
            case "Q" -> Material.STICK;
            case "F" -> Material.ENDER_EYE;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 브루저").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 메인핸드에 나무막대기, 오프핸드에 목검.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("평타 : 막대기 평타가 1타 → 2타 → 강화타 순으로 순환한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("궤적 범위 내 모든 적을 타격하고, 중심에 가까울수록 강하다. (1.1 ~ 4.4)").color(NamedTextColor.GREEN));
            lore.add(Component.text("콤보 : 궤적 중심에 적을 맞히면 이어지고, 빗나가면 1타로 초기화된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("강화타 : 회전하며 1.5배 피해를 주고 적을 멀리 밀쳐낸다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("강화타로 준 피해의 25%만큼 체력을 회복한다. (최대 체력의 1/6까지)").color(NamedTextColor.GREEN));
            lore.add(Component.text("마지막 콤보 후 4초가 지나면 1타로 돌아간다.").color(NamedTextColor.GREEN));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 목검을 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
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
                lore.add(Component.text("시스템 : 재사용").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 전방으로 최대 6칸 순간이동한 후, 1초간 은신한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("은신 중에는 경직 및 무적 상태가 된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 은신을 해제하고 전방 부채꼴에 검풍을 일으켜 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("검풍에 적중한 대상은 2초간 기절한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("은신 중 Q 또는 F를 사용하면 은신이 해제되고 재사용할 수 없다.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 연계").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 전방으로 짧게 돌진하며 막대기를 크게 휘두른다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("바라보는 대상에게 6, 궤적에 닿은 적에게 2의 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("연계 : 사용 후 4초 안의 다음 평타가 강화타가 된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("원래 강화타 차례였다면 Q 강화타가 된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("Q 강화타 : 3배 피해를 주고 강하게 밀쳐내며, 벽에 부딪힌 적은 1초간 기절한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("Q 강화타로 준 피해의 20%만큼 12초간 보호막을 얻는다. (최대 체력의 1/5까지)").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격 / 반격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 자세 / 재사용").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("종언 : 목검을 뽑아 전방 부채꼴을 위에서 아래로 지그재그로 4번 벤다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("참격의 중앙과 칼끝에 가깝게 맞을수록 피해가 커진다. (베기당 2.2 ~ 8)").color(NamedTextColor.GREEN));
                lore.add(Component.text("베는 동안 전방에서 날아오는 투사체를 베어 없앤다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("패리 : 웅크린 채 사용하면 패리 자세를 취하고, 웅크리기를 풀면 종언이 나간다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("패리 성공 : 공격을 한 번 무시하고, 공격한 적을 3초간 발광시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("반격 : 3초 안에 재사용하면 대상에게 돌진해 12의 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("돌진은 앞을 막는 블럭을 부수며, 베기에 휘말린 적도 피해를 입는다. (2.2 ~ 12)").color(NamedTextColor.GREEN));
                lore.add(Component.text("목검을 쥔 동안 R, Q를 사용할 수 없으며, 돌진 중에는 무적이다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3초 안에 재사용하지 않으면 F가 바로 준비된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("종언 또는 반격을 사용하면 재사용 대기시간 44초.").color(NamedTextColor.GREEN));
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

    public List<Long> requireExpOfR = List.of(22L, 66L, 180L, 240L, 360L, 646L);
    public List<Long> requireExpOfQ = List.of(22L, 66L, 180L, 240L, 360L, 646L);
    public List<Long> requireExpOfF = List.of(22L, 66L, 180L, 240L, 360L, 646L);

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