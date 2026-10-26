package com.example.offhandonly.mixin;

import com.example.offhandonly.OffhandOnlyConfig;
import com.example.offhandonly.OffhandOnlyHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 负责：
 * <ol>
 *     <li>左键单击：主手攻击禁用；副手拿着武器时放行（伤害由 {@link PlayerEntityMixin} 处理）。</li>
 *     <li>左键长按：只在“副手拿着工具”时放行 {@code handleBlockBreaking(boolean)}。</li>
 *     <li>挖掘/攻击动画：把原版挥主手的 {@code swingHand(MAIN_HAND)} 换成挥副手。</li>
 *     <li>右键：把 {@code doItemUse()} 里的 {@code Hand.values()} 限制为只有 {@code OFF_HAND}。</li>
 * </ol>
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /**
     * 左键单击：攻击实体 / 玩家，以及创造模式单点瞬破，都从这里进入。
     *
     * <p>三种情况：
     * <ul>
     *     <li>主手攻击没被禁用 → 放行，纯原版；</li>
     *     <li>副手拿着武器且允许副手攻击 → 放行；</li>
     *     <li>其余 → 返回 false，等价于“这次没有发生攻击”，也不会播放挥手动画。</li>
     * </ul>
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void offhandonly$gateAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!OffhandOnlyConfig.get().disableAttack) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!OffhandOnlyHelper.isActive(player)) {
            return;
        }
        if (OffhandOnlyHelper.isOffhandCombat(player)) {
            return;
        }
        cir.setReturnValue(false);
    }

    /** 攻击动画：原版固定挥主手，副手模式下改成挥副手。 */
    @Redirect(
            method = "doAttack",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V")
    )
    private void offhandonly$swingAttackOffhand(ClientPlayerEntity player, Hand hand) {
        player.swingHand(OffhandOnlyHelper.isOffhandCombat(player) ? Hand.OFF_HAND : hand);
    }

    /**
     * 持续挖掘的闸门。
     *
     * <p>原版这里做了三件事：更新破坏进度、喷粒子、挥主手。我们只在“该不该挖”这一层做判断：
     * <ul>
     *     <li>{@code breaking == false}：永远放行，让原版执行 {@code cancelBlockBreaking()} 清理状态。</li>
     *     <li>主手挖掘没被禁用：放行，保持原版。</li>
     *     <li>副手拿着工具且开启了副手挖掘：放行，交给 {@link PlayerEntityMixin} 换成副手计算。</li>
     *     <li>其余情况：取消。</li>
     * </ul>
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void offhandonly$gateBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (!breaking) {
            return;
        }
        if (!OffhandOnlyConfig.get().disableBlockBreaking) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!OffhandOnlyHelper.isActive(player)) {
            return;
        }
        if (OffhandOnlyHelper.isOffhandMining(player)) {
            return;
        }
        ci.cancel();
    }

    /**
     * 挖掘动画。
     *
     * <p>1.20.1 的 {@code handleBlockBreaking} 里固定写的是 {@code player.swingHand(Hand.MAIN_HAND)}。
     * 而 {@code LivingEntity#swingHand} 会记录 {@code preferredHand}，
     * 第一人称 {@code HeldItemRenderer} 与第三人称 {@code BipedEntityModel#animateArms}
     * 都是按 {@code preferredHand} 决定哪只手做动画的 —— 所以这里换成副手，动画自然就对上了。
     */
    @Redirect(
            method = "handleBlockBreaking",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V")
    )
    private void offhandonly$swingOffhand(ClientPlayerEntity player, Hand hand) {
        player.swingHand(OffhandOnlyHelper.isOffhandMining(player) ? Hand.OFF_HAND : hand);
    }

    /**
     * 右键总入口。
     *
     * <p>1.20.1 的 {@code doItemUse()} 结构是：
     * <pre>
     * for (Hand hand : Hand.values()) {      // MAIN_HAND -> OFF_HAND
     *     ... 实体交互 / 方块交互 / 物品使用 ...
     * }
     * </pre>
     * 只要让这次遍历只返回 {@code OFF_HAND}，主手就完全不会参与，
     * 而盾牌、食物、弓、珍珠、船、拴绳、水桶…… 全部仍由原版逻辑执行。
     */
    @Redirect(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;")
    )
    private Hand[] offhandonly$onlyOffhand() {
        if (OffhandOnlyConfig.get().forceOffhandOnly
                && OffhandOnlyHelper.isActive(MinecraftClient.getInstance().player)) {
            return new Hand[]{Hand.OFF_HAND};
        }
        return Hand.values();
    }
}
