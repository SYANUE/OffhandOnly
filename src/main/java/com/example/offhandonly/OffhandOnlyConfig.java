package com.example.offhandonly;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置：{@code config/offhandonly.json}，可在 ModMenu 里直接改。
 */
public final class OffhandOnlyConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static OffhandOnlyConfig instance = new OffhandOnlyConfig();

    /** 副手挖掘效率倍率的下限 / 上限。 */
    public static final double MIN_SPEED = 0.1D;
    public static final double MAX_SPEED = 1.0D;

    /** 副手攻击伤害倍率的下限 / 上限。 */
    public static final double MIN_DAMAGE = 0.1D;
    public static final double MAX_DAMAGE = 1.0D;

    // ---------------- 生效范围 ----------------

    /** 只在生存（非创造、非旁观）模式生效。 */
    public boolean survivalOnly = false;

    /**
     * 玩家白名单：填玩家名或 UUID，逗号分隔。留空 = 所有人生效。
     * 不在名单里的玩家，本模组的挖掘 / 攻击 / 右键 / 渲染全部按原版走。
     */
    public List<String> playerWhitelist = new ArrayList<>();

    // ---------------- 主手 ----------------

    /** 主手左键：禁止攻击实体 / 玩家（关掉就是原版行为）。 */
    public boolean disableAttack = true;

    /** 主手左键：禁止破坏方块与持续挖掘（关掉就是原版行为）。 */
    public boolean disableBlockBreaking = true;

    // ---------------- 副手 ----------------

    /** 副手拿工具时允许左键挖掘，效率按 {@link #offhandMiningSpeed} 打折。 */
    public boolean offhandMining = true;

    /** 副手挖掘效率倍率：1.0 = 和主手一样，0.5 = 减半。 */
    public double offhandMiningSpeed = 0.5D;

    /** 副手拿武器时允许左键攻击，伤害按 {@link #offhandAttackDamage} 打折。 */
    public boolean offhandAttack = true;

    /** 副手攻击伤害倍率：1.0 = 和主手一样，0.5 = 减半。 */
    public double offhandAttackDamage = 0.5D;

    /** 副手空手时也允许攻击（空手伤害同样打折）。 */
    public boolean offhandEmptyHandAttack = false;

    /** 右键：强制所有交互使用 OFF_HAND，完全不进入 MAIN_HAND 流程。 */
    public boolean forceOffhandOnly = true;

    // ---------------- 第一人称渲染 ----------------

    /** 第一人称是否渲染主手。 */
    public boolean renderMainHand = true;

    /** 第一人称是否渲染副手；打开时副手空手也会画出手臂。 */
    public boolean renderOffhand = true;

    /** 打印调试日志。 */
    public boolean debug = false;

    public static OffhandOnlyConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("offhandonly.json");
    }

    public static void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                OffhandOnlyConfig loaded = GSON.fromJson(json, OffhandOnlyConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            }
            instance.sanitize();
            save();
        } catch (IOException | RuntimeException e) {
            OffhandOnly.LOGGER.warn("[OffhandOnly] 读取配置失败，使用默认值", e);
            instance = new OffhandOnlyConfig();
        }
    }

    public static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            OffhandOnly.LOGGER.warn("[OffhandOnly] 保存配置失败", e);
        }
    }

    /** 防止手改 JSON 写出越界值。 */
    public void sanitize() {
        offhandMiningSpeed = clamp(offhandMiningSpeed, MIN_SPEED, MAX_SPEED);
        offhandAttackDamage = clamp(offhandAttackDamage, MIN_DAMAGE, MAX_DAMAGE);
        if (playerWhitelist == null) {
            playerWhitelist = new ArrayList<>();
        }
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
