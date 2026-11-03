package org.core.coreSystem.cores.VOL3.Knight.coreSystem;

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
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;

import java.util.ArrayList;
import java.util.List;

public class knightInventory extends absInventory {

    private final Core plugin;

    public knightInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Knight.contains(player);
    }

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_SWORD");
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.IRON_SWORD;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("KNIGHT");
            case "R" -> Component.text("TetherBlades");
            case "Q" -> Component.text("FlashSevering");
            case "F" -> Component.text("MACHAIRA");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.DIAMOND;
            case "Q" -> Material.NETHERITE_SCRAP;
            case "F" -> Material.NETHERITE_INGOT;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 중거리 견제형").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 메인핸드와 오프핸드 모두 검 종류 아이템 장착.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("검기 : 좌클릭 시 전방으로 검기를 날려 처음 닿는 적에게 3의 피해를 가한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("꽂힌 검 : 적이나 블럭에 꽂힌 검은 12초간 남으며, 최대 12자루까지 유지된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("12자루를 넘기면 가장 먼저 꽂힌 검을 회수한다. 검을 회수할 때마다 체력을 2.33 회복한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("검이 꽂힌 적이 죽으면 꽂혀 있던 검들이 튕겨 나가 주변 바닥에 각각 꽂힌다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("결합선 : 검기가 꽂힌 검에 닿으면, 그 검에서 12블럭 안의 다른 꽂힌 검을 향해 검기가 뻗어 나간다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("뻗어 나간 검기가 지나는 적은 검기 피해의 60%를 받는다. (검기 한 번에 대상마다 한 번)").color(NamedTextColor.GREEN));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 철검을 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
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
                lore.add(Component.text("시스템 : 연계").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트/블럭").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("투척 : 던진 검이 꽂혀 있지 않으면, 오프핸드 검을 전방으로 던진다. 던진 검은 처음 닿는 적이나 블럭에 꽂힌다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("적에게 꽂히면 오프핸드 검 재질의 공격력만큼 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("어검 : 던진 검(FlashSevering으로 던진 검 포함)이 꽂혀 있으면, 그 검들이 사라지고 각 자리 주위 세 방향에 검 3자루가 나타난다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("검 3자루는 살짝 돌아 자세를 잡은 뒤, 적에게 꽂혀 있었다면 직선으로, 블럭에 꽂혀 있었다면 소용돌이치며 중심을 지나 날아간다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("날아가는 검은 블럭을 통과하며, 처음 닿는 적에게 꽂혀 재질 공격력의 1/3만큼 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 대기시간 0.4초.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 방어 / 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 패링 / 재사용").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 3초간 패링 상태가 된다. 패링 상태에서는 이동속도가 70% 감소한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("패링 성공 : 근접 공격이나 투사체를 막고, 공격한 적에게 반드시 꽂히는 검을 날린다. (재사용 대기시간 3초)").color(NamedTextColor.GREEN));
                lore.add(Component.text("날린 검은 블럭을 통과하며, 메인핸드 검 재질의 공격력만큼 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 패링 상태에서 다시 사용하면 전방 40도 부채꼴로 검 3자루를 던진다. (재사용 대기시간 12초)").color(NamedTextColor.GREEN));
                lore.add(Component.text("던진 검은 처음 닿는 적이나 블럭에 꽂히며, 메인핸드 검 재질의 공격력만큼 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("꽂힌 검이 12자루일 때 사용하면 가장 먼저 꽂힌 4자루를 회수하고 4자루를 던진다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("패링 상태가 그냥 끝나면 재사용 대기시간이 없다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 선딜레이").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트/블럭").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("조건 : 꽂힌 검이 1자루 이상 있어야 사용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("선딜레이 : 0.6초간 경직되며, 양손의 검과 꽂힌 검을 모두 거둬 대검으로 합친다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("거둬지는 검은 손으로 날아오며, 뽑히는 적과 경로 위의 적에게 재질 공격력의 1/6만큼 마법 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("거둔 검마다 체력을 2.33 회복한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("내려치기 : 대검을 내리찍어 전방 직선에 피해를 가하고 1.2초간 기절시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("거둔 검이 많을수록 대검과 범위, 피해가 커진다. (1자루 : 길이 6, 폭 2, 피해 4 ~ 12자루 : 길이 12, 폭 3, 피해 13)").color(NamedTextColor.GREEN));
                lore.add(Component.text("거둔 검마다 (재질 공격력 - 1)의 절반만큼 마법 피해가 추가된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("피격-내려치기 : 피격된 블럭들을 모두 파괴한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 대기시간 : 거둔 검 1자루당 5초 (최대 60초).").color(NamedTextColor.GREEN));
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

    public List<Long> requireExpOfR = List.of(44L, 88L, 108L, 244L, 304L, 444L);
    public List<Long> requireExpOfQ = List.of(44L, 88L, 108L, 244L, 304L, 444L);
    public List<Long> requireExpOfF = List.of(44L, 88L, 108L, 244L, 304L, 444L);

    @Override
    protected void reinforceSkill(Player player, String skill, Long skillLevel, Inventory customInv) {
        if (skillLevel >= 6) return;

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
            case "R": requireExpList = requireExpOfR; hpBonus = 1; break;
            case "Q": requireExpList = requireExpOfQ; hpBonus = 2; break;
            case "F": requireExpList = requireExpOfF; break;
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