package com.example.offhandonly;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.UUID;

/**
 * 用<b>临时属性 modifier</b> 把副手武器接进原版的伤害/攻速计算。
 *
 * <p>为什么走属性而不是自己算伤害：
 * {@code PlayerEntity#attack} 读的是 {@code getAttributeValue(GENERIC_ATTACK_DAMAGE)}，
 * 而这个属性上的装备加成是原版按 {@code MAINHAND} 槽自动挂上去的，副手物品的 modifier 根本进不来。
 * 与其在 {@code attack} 里手写一遍属性数学（加法/乘法混合，容易和别的模组打架），
 * 不如直接往属性上补两个临时 modifier，让原版自己去算：
 *
 * <ul>
 *     <li>{@code ADDITION}：(副手武器的加成 − 主手武器的加成)，把“谁在提供伤害”换过来；</li>
 *     <li>{@code MULTIPLY_TOTAL}：−(1 − 配置倍率)，也就是整体乘 0.5 之类的折扣。</li>
 * </ul>
 *
 * <p>攻速同理镜像一份（但<b>不</b>打折），这样副手斧头依旧挥得慢，手感才对。
 *
 * <p>只在服务端挂。属性本身会同步给客户端，客户端不需要也不应该重复挂，
 * 否则会被服务端的同步包来回覆盖。
 */
public final class OffhandCombatAttributes {

    private static final UUID DAMAGE_UUID = UUID.fromString("3f1c9b0e-6a2d-4f8b-9c1e-5d7a2b4c8e01");
    private static final UUID DAMAGE_SCALE_UUID = UUID.fromString("3f1c9b0e-6a2d-4f8b-9c1e-5d7a2b4c8e02");
    private static final UUID SPEED_UUID = UUID.fromString("3f1c9b0e-6a2d-4f8b-9c1e-5d7a2b4c8e03");

    private OffhandCombatAttributes() {
    }

    public static void tick(PlayerEntity player) {
        if (player.getWorld().isClient()) {
            return;
        }
        if (!OffhandOnlyHelper.isOffhandCombat(player)) {
            clear(player);
            return;
        }

        ItemStack offhand = player.getStackInHand(Hand.OFF_HAND);
        ItemStack mainHand = player.getMainHandStack();

        set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_UUID, "offhandonly.weapon_damage",
                addition(offhand, EntityAttributes.GENERIC_ATTACK_DAMAGE)
                        - addition(mainHand, EntityAttributes.GENERIC_ATTACK_DAMAGE),
                EntityAttributeModifier.Operation.ADDITION);

        set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_SCALE_UUID, "offhandonly.damage_scale",
                -(1.0D - OffhandOnlyConfig.get().offhandAttackDamage),
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, EntityAttributes.GENERIC_ATTACK_SPEED, SPEED_UUID, "offhandonly.weapon_speed",
                addition(offhand, EntityAttributes.GENERIC_ATTACK_SPEED)
                        - addition(mainHand, EntityAttributes.GENERIC_ATTACK_SPEED),
                EntityAttributeModifier.Operation.ADDITION);
    }

    private static void clear(PlayerEntity player) {
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_UUID);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_SCALE_UUID);
        remove(player, EntityAttributes.GENERIC_ATTACK_SPEED, SPEED_UUID);
    }

    /** 只取 ADDITION 部分：武器的基础攻击力/攻速都是这一种运算，乘算类留给原版自己处理。 */
    private static double addition(ItemStack stack, EntityAttribute attribute) {
        double sum = 0.0D;
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        for (EntityAttributeModifier modifier : stack.getAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND).get(attribute)) {
            if (modifier.getOperation() == EntityAttributeModifier.Operation.ADDITION) {
                sum += modifier.getValue();
            }
        }
        return sum;
    }

    private static void set(PlayerEntity player, EntityAttribute attribute, UUID id, String name,
                            double value, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        EntityAttributeModifier existing = instance.getModifier(id);
        // 值没变就不要反复 remove/add，否则每 tick 都会把属性标脏、触发同步包
        if (existing != null && existing.getOperation() == operation
                && Double.compare(existing.getValue(), value) == 0) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        instance.addPersistentModifier(new EntityAttributeModifier(id, name, value, operation));
    }

    private static void remove(PlayerEntity player, EntityAttribute attribute, UUID id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }
}
