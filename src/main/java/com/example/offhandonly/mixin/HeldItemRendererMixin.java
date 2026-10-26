package com.example.offhandonly.mixin;

import com.example.offhandonly.OffhandOnlyConfig;
import com.example.offhandonly.OffhandOnlyHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称手部渲染开关。
 *
 * <p>两个开关都在 {@code renderFirstPersonItem} 的入口处理，它是每只手唯一的第一人称绘制入口：
 * <ul>
 *     <li>{@code renderMainHand = false} → 主手直接不画；</li>
 *     <li>{@code renderOffhand = false} → 副手直接不画；</li>
 *     <li>{@code renderOffhand = true} 且副手为空 → 原版在空手分支里只给<b>主手</b>画手臂
 *         （{@code if (isMainHand) renderArmHoldingItem(...)}），副手空手时什么都不画。
 *         这里复用同一个 {@code renderArmHoldingItem} 把空手副手补上，
 *         免得主手一关就出现“两只手都没了”。</li>
 * </ul>
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    private void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      int light, float equipProgress, float swingProgress, Arm arm) {
        throw new AssertionError("shadow");
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void offhandonly$renderGate(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                        float swingProgress, ItemStack item, float equipProgress,
                                        MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                        CallbackInfo ci) {
        if (!OffhandOnlyHelper.isActive(player)) {
            return;
        }
        OffhandOnlyConfig config = OffhandOnlyConfig.get();

        if (hand == Hand.MAIN_HAND) {
            if (!config.renderMainHand) {
                ci.cancel();
            }
            return;
        }

        if (!config.renderOffhand) {
            ci.cancel();
            return;
        }

        if (item.isEmpty() && !player.isInvisible()) {
            matrices.push();
            this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress,
                    player.getMainArm().getOpposite());
            matrices.pop();
            ci.cancel();
        }
    }
}
