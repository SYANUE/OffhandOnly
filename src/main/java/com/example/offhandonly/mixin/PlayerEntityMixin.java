package com.example.offhandonly.mixin;

import com.example.offhandonly.OffhandCombatAttributes;
import com.example.offhandonly.OffhandOnlyConfig;
import com.example.offhandonly.OffhandOnlyHelper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让“挖方块”和“打人”这两件事在原版的计算链里改用副手物品。
 *
 * <p>注意：这个类是<b>双端</b>的。挖掘进度、耐久消耗、攻击伤害全都是服务端权威的，
 * 所以单机由集成服务端直接生效，多人服务器需要服务端也装本模组。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    // ------------------------------------------------------------------
    // 挖掘
    // ------------------------------------------------------------------

    /** 1. 工具速度 + 是否适用：直接问副手物品本身。 */
    @Redirect(
            method = "getBlockBreakingSpeed",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/entity/player/PlayerInventory;getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F")
    )
    private float offhandonly$miningSpeed(PlayerInventory inventory, BlockState state) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (OffhandOnlyHelper.isOffhandMining(self)) {
            return self.getStackInHand(Hand.OFF_HAND).getMiningSpeedMultiplier(state);
        }
        return inventory.getBlockBreakingSpeed(state);
    }

    /** 2. 效率附魔：改读副手物品的等级。 */
    @Redirect(
            method = "getBlockBreakingSpeed",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/enchantment/EnchantmentHelper;getEfficiency(Lnet/minecraft/entity/LivingEntity;)I")
    )
    private int offhandonly$efficiency(LivingEntity entity) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (OffhandOnlyHelper.isOffhandMining(self)) {
            return EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, self.getStackInHand(Hand.OFF_HAND));
        }
        return EnchantmentHelper.getEfficiency(entity);
    }

    /** 3. 效率附魔分支里的 {@code !itemStack.isEmpty()} 判定，也指向同一只手的物品。 */
    @Redirect(
            method = "getBlockBreakingSpeed",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$efficiencyStack(PlayerEntity self) {
        return OffhandOnlyHelper.effectiveMiningStack(self);
    }

    /** 4. 能不能“收获”（决定 30 还是 100 的除数）。 */
    @Redirect(
            method = "canHarvest",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$canHarvestStack(PlayerInventory inventory) {
        return OffhandOnlyHelper.effectiveMiningStack((PlayerEntity) (Object) this);
    }

    /** 5. 效率打折：整体倍率（含急速、水下速掘等加成之后）统一乘一次。 */
    @ModifyReturnValue(method = "getBlockBreakingSpeed", at = @At("RETURN"))
    private float offhandonly$applySpeedMultiplier(float original) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (OffhandOnlyHelper.isOffhandMining(self)) {
            return original * (float) OffhandOnlyConfig.get().offhandMiningSpeed;
        }
        return original;
    }

    // ------------------------------------------------------------------
    // 攻击
    // ------------------------------------------------------------------

    /**
     * 每 tick 维护临时属性 modifier（攻击力差额 / 伤害折扣 / 攻速差额）。
     * 只在服务端做，客户端通过属性同步拿到同样的值。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void offhandonly$tickCombatAttributes(CallbackInfo ci) {
        OffhandCombatAttributes.tick((PlayerEntity) (Object) this);
    }

    /**
     * {@code attack()} 里有 3 处 {@code getMainHandStack()}：
     * <ol>
     *     <li>ordinal 0 —— 给 {@code EnchantmentHelper.getAttackDamage} 算锋利/亡灵杀手；</li>
     *     <li>ordinal 1 —— 同上，另一条分支；</li>
     *     <li>ordinal 2 —— 末尾 {@code ItemStack#postHit} 的接收者，也就是扣武器耐久的地方。</li>
     * </ol>
     * 三处统一改成“副手模式下的战斗用栈”。
     */
    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE",
                     ordinal = 0,
                     target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$attackStack0(PlayerEntity self) {
        return OffhandOnlyHelper.effectiveCombatStack(self);
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE",
                     ordinal = 1,
                     target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$attackStack1(PlayerEntity self) {
        return OffhandOnlyHelper.effectiveCombatStack(self);
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE",
                     ordinal = 2,
                     target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack offhandonly$attackStack2(PlayerEntity self) {
        return OffhandOnlyHelper.effectiveCombatStack(self);
    }
}
