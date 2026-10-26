package com.example.offhandonly.mixin;

import com.example.offhandonly.OffhandOnlyHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修“耐久扣到主手”的 bug。
 *
 * <p>1.20.1 的 {@code ServerPlayerInteractionManager#tryBreakBlock} 长这样：
 * <pre>
 * ItemStack tool = this.player.getMainHandStack();   // ← 主手
 * ItemStack snapshot = tool.copy();
 * boolean canHarvest = this.player.canHarvest(state);
 * tool.postMine(world, state, pos, this.player);     // ← 扣耐久在这里
 * if (removed &amp;&amp; canHarvest) block.afterBreak(..., snapshot);   // ← 时运/精准采集也用这个栈
 * </pre>
 * 挖掘速度虽然已经按副手算了，但扣耐久和时运判定仍然认主手，所以会“用副手的镐挖、扣主手工具的耐久”。
 *
 * <p>把这一处 {@code getMainHandStack()} 换掉，耐久、时运、精准采集一起对上。
 * 客户端没有 postMine（耐久只在服务端扣），所以只需要这一个注入点。
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Redirect(
            method = "tryBreakBlock",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/server/network/ServerPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$toolStack(ServerPlayerEntity player) {
        return OffhandOnlyHelper.effectiveMiningStack(player);
    }
}
