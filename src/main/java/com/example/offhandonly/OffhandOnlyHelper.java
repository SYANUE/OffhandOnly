package com.example.offhandonly;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * “副手挖掘 / 副手攻击 / 生效范围”的公共判断逻辑。
 *
 * <p>这里刻意不写任何物品白名单：只问原版两个问题——
 * “这东西挖这些方块会加速吗？”、“这东西有攻击力加成吗？”。
 * 只要模组物品正确声明了 {@code getMiningSpeedMultiplier} 或属性 modifier，就自动兼容。
 */
public final class OffhandOnlyHelper {

    /** 用来探测“是不是工具”的方块：镐 / 斧 / 铲 / 剪刀 各一个代表。 */
    private static final BlockState[] TOOL_PROBES = {
            Blocks.STONE.getDefaultState(),
            Blocks.OAK_LOG.getDefaultState(),
            Blocks.DIRT.getDefaultState(),
            Blocks.COBWEB.getDefaultState(),
            Blocks.OAK_LEAVES.getDefaultState()
    };

    private OffhandOnlyHelper() {
    }

    // ------------------------------------------------------------------
    // 生效范围：生存模式开关 + 玩家白名单
    // ------------------------------------------------------------------

    /**
     * 这个玩家是否受本模组影响。
     *
     * <p>任何“改行为”的地方（挖掘 / 攻击 / 右键 / 第一人称渲染）都要先过这一关。
     * 不满足时一律按原版走，不做任何特殊处理。
     */
    public static boolean isActive(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        OffhandOnlyConfig config = OffhandOnlyConfig.get();
        if (config.survivalOnly && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return isWhitelisted(player, config);
    }

    /** 白名单为空视为“所有人生效”；否则按玩家名或 UUID 匹配（忽略大小写）。 */
    public static boolean isWhitelisted(PlayerEntity player, OffhandOnlyConfig config) {
        if (config.playerWhitelist == null || config.playerWhitelist.isEmpty()) {
            return true;
        }
        String name = player.getGameProfile().getName();
        String uuid = player.getUuidAsString();
        for (String raw : config.playerWhitelist) {
            if (raw == null) {
                continue;
            }
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.equalsIgnoreCase(name) || entry.equalsIgnoreCase(uuid)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 物品判定
    // ------------------------------------------------------------------

    public static boolean isTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (BlockState probe : TOOL_PROBES) {
            if (stack.getMiningSpeedMultiplier(probe) > 1.0F) {
                return true;
            }
        }
        return isWeapon(stack);
    }

    /** 有攻击力加成就算武器（剑 / 斧 / 镐 / 三叉戟……，模组物品同理）。 */
    public static boolean isWeapon(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                        .get(EntityAttributes.GENERIC_ATTACK_DAMAGE).isEmpty();
    }

    // ------------------------------------------------------------------
    // 模式判定
    // ------------------------------------------------------------------

    /**
     * 当前是否处于“副手挖掘”模式。
     *
     * <p>四个条件缺一不可：玩家生效 + 主手挖掘被禁用 + 允许副手挖掘 + 副手拿着工具。
     */
    public static boolean isOffhandMining(PlayerEntity player) {
        OffhandOnlyConfig config = OffhandOnlyConfig.get();
        return config.disableBlockBreaking
                && config.offhandMining
                && isActive(player)
                && isTool(player.getStackInHand(Hand.OFF_HAND));
    }

    /**
     * 当前是否处于“副手攻击”模式。
     *
     * <p>副手拿着武器，或者开了“空手攻击”且副手为空。
     */
    public static boolean isOffhandCombat(PlayerEntity player) {
        OffhandOnlyConfig config = OffhandOnlyConfig.get();
        if (!config.disableAttack || !config.offhandAttack || !isActive(player)) {
            return false;
        }
        ItemStack offhand = player.getStackInHand(Hand.OFF_HAND);
        return isWeapon(offhand) || (config.offhandEmptyHandAttack && offhand.isEmpty());
    }

    /** 挖掘 / 收获判定该用哪个物品栈：副手模式用副手，否则保持原版主手。 */
    public static ItemStack effectiveMiningStack(PlayerEntity player) {
        return isOffhandMining(player) ? player.getStackInHand(Hand.OFF_HAND) : player.getMainHandStack();
    }

    /** 攻击（附魔伤害、耐久消耗）该用哪个物品栈。 */
    public static ItemStack effectiveCombatStack(PlayerEntity player) {
        return isOffhandCombat(player) ? player.getStackInHand(Hand.OFF_HAND) : player.getMainHandStack();
    }
}
