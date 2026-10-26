package com.example.offhandonly;

import net.fabricmc.api.ClientModInitializer;

/**
 * Offhand Only —— 只允许使用副手的模组（客户端侧）。
 *
 * <p>设计原则：
 * <ul>
 *     <li>左键：攻击始终禁用；挖掘只在副手拿着工具时放行，效率按配置打折。</li>
 *     <li>右键：绕过 {@code MAIN_HAND}，直接进入 {@code OFF_HAND} 流程。</li>
 *     <li>物品本身的使用逻辑（盾牌、食物、弓、珍珠、船、拴绳、水桶、打火石、钓鱼竿……）
 *         全部交给 Minecraft 原版交互系统，本模组只负责“用哪只手”。</li>
 * </ul>
 *
 * <p>本模组不修改物品栏界面、不修改按键绑定、不交换主副手物品栈，
 * 因此 E / F / 拖拽 / 快速移动 / 丢弃 全部保持原版行为。
 */
public class OffhandOnlyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OffhandOnlyConfig config = OffhandOnlyConfig.get();
        OffhandOnly.LOGGER.info("[OffhandOnly] 已启用：攻击={} 副手攻击={}({}x) 主手挖掘={} 副手挖掘={}({}x) 强制副手={}",
                config.disableAttack,
                config.offhandAttack,
                config.offhandAttackDamage,
                config.disableBlockBreaking,
                config.offhandMining,
                config.offhandMiningSpeed,
                config.forceOffhandOnly);
    }
}
