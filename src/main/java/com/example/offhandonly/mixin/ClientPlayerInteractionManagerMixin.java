package com.example.offhandonly.mixin;

import com.example.offhandonly.OffhandOnlyConfig;
import com.example.offhandonly.OffhandOnlyHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 兜底：即使有其它代码（原版其它路径或第三方模组）直接以 {@code MAIN_HAND}
 * 调用交互入口，也在这里统一改写为 {@code OFF_HAND}。
 *
 * <p>主流程其实已经被 {@link MinecraftClientMixin} 的 {@code Hand.values()} 重定向覆盖，
 * 这一层只是保证“任何情况下都不会自动改回 MAIN_HAND”。
 *
 * <p>四个入口缺一不可：
 * <ul>
 *     <li>{@code interactBlock} —— 放方块 / 打火石 / 水桶 / 骨粉 / 门……</li>
 *     <li>{@code interactItem} —— 食物 / 弓 / 珍珠 / 钓鱼竿 / 望远镜……</li>
 *     <li>{@code interactEntity} —— 拴绳 / 船 / 矿车 / 村民……</li>
 *     <li>{@code interactEntityAtLocation} —— 带命中点的实体交互（盔甲架、展示框、马具等）</li>
 * </ul>
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    private static Hand offhandonly$force(Hand hand) {
        if (hand == Hand.MAIN_HAND
                && OffhandOnlyConfig.get().forceOffhandOnly
                && OffhandOnlyHelper.isActive(MinecraftClient.getInstance().player)) {
            return Hand.OFF_HAND;
        }
        return hand;
    }

    @ModifyVariable(method = "interactBlock", at = @At("HEAD"), argsOnly = true)
    private Hand offhandonly$forceOffhandBlock(Hand hand) {
        return offhandonly$force(hand);
    }

    @ModifyVariable(method = "interactItem", at = @At("HEAD"), argsOnly = true)
    private Hand offhandonly$forceOffhandItem(Hand hand) {
        return offhandonly$force(hand);
    }

    @ModifyVariable(method = "interactEntity", at = @At("HEAD"), argsOnly = true)
    private Hand offhandonly$forceOffhandEntity(Hand hand) {
        return offhandonly$force(hand);
    }

    @ModifyVariable(method = "interactEntityAtLocation", at = @At("HEAD"), argsOnly = true)
    private Hand offhandonly$forceOffhandEntityAt(Hand hand) {
        return offhandonly$force(hand);
    }
}
