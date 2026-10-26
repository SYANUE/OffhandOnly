# Offhand Only（仅副手）

Minecraft **1.20.1 + Fabric** 模组：**主手左键/右键完全禁用，所有交互强制交给副手**，物品使用逻辑 100% 沿用原版。

- 主手：不能攻击、不能破坏方块、不能使用物品、不能放方块、不能实体交互。
- 副手：盾牌、食物、方块、弓、末影珍珠、船、拴绳、水桶/岩浆桶、打火石、钓鱼竿、三叉戟、烟花、药水……全部按原版行为工作。
- **副手挖掘**：效率可配（默认 50%），耐久扣副手工具，时运/精准采集也按副手工具判定。
- **副手攻击**：伤害可配（默认 50%），攻速镜像副手武器，附魔与耐久都走副手。
- **生效范围**：可限制为仅生存模式，也可按玩家白名单只对指定玩家生效。
- **第一人称渲染**：主手/副手可分别开关，副手空手时也能画出手臂。
- 副手不死图腾：完全不动原版致死保护逻辑。
- 物品栏、E、F 切换副手、拖拽、快速移动、丢弃：全部保持原版。

> 模组是**双端**的（`environment: "*"`），因为挖掘速度、耐久、攻击伤害全部是服务端权威的。

---

## 一、右键：只改“用哪只手”

1.20.1 的 `MinecraftClient#doItemUse()` 结构是：

```java
for (Hand hand : Hand.values()) {          // ← 唯一的手的遍历
    ... 实体交互 / 方块交互 / 物品使用 ...
}
```

把这次 `Hand.values()` 的返回值限制成 `{OFF_HAND}`，三条分支就全部自动落到副手。
外加 `ClientPlayerInteractionManagerMixin` 兜底：别的代码直接以 `MAIN_HAND` 调用那四个入口时也会被改写。

## 二、左键：闸门

| 入口 | 攻击 | 挖掘 |
|---|---|---|
| `MinecraftClient#doAttack()` | 主手禁用 + 副手有武器 → 放行；否则 `setReturnValue(false)` | 创造模式瞬破同此逻辑 |
| `MinecraftClient#handleBlockBreaking(boolean)` | — | 副手有工具 → 放行；`breaking == false` 恒放行 |

两个闸门都先过 `isActive(player)`：不满足“仅生存模式 / 白名单”时直接按原版走。

## 三、挖掘：速度 + 耐久

挖掘链是 `BlockState#calcBlockBreakingDelta` → `Block#calcBlockBreakingDelta`
→ `player.canHarvest(state)` + `player.getBlockBreakingSpeed(state)`。

**速度**（`PlayerEntityMixin`）：

| 注入点 | 改成 |
|---|---|
| `inventory.getBlockBreakingSpeed(state)` | 副手栈的 `getMiningSpeedMultiplier` |
| `EnchantmentHelper.getEfficiency(this)` | 副手栈的效率等级 |
| `this.getMainHandStack()` | 副手栈 |
| `canHarvest` 里的 `inventory.getMainHandStack()` | 副手栈 |
| `@ModifyReturnValue getBlockBreakingSpeed` | × `offhandMiningSpeed` |

**耐久**（`ServerPlayerInteractionManagerMixin`）：

```java
// ServerPlayerInteractionManager#tryBreakBlock
ItemStack tool = this.player.getMainHandStack();   // ← 主手，扣的就是它
ItemStack snapshot = tool.copy();
tool.postMine(world, state, pos, this.player);     // ← 耐久在这里
if (removed && canHarvest) block.afterBreak(..., snapshot);   // ← 时运/精准采集也用它
```

该方法里有**两处** `getMainHandStack()`（另一处是 `Item.canMine`），全部 `@Redirect` 成
`effectiveMiningStack(player)`，耐久、时运、精准采集一起对上。客户端没有 `postMine`，所以只需这一个类。

## 四、攻击：临时属性 modifier

`PlayerEntity#attack` 读的是 `getAttributeValue(GENERIC_ATTACK_DAMAGE)`，而装备加成是原版按
`MAINHAND` 槽自动挂的，副手物品的 modifier 进不来。所以不手写属性数学，而是**往属性上补临时 modifier**
（`OffhandCombatAttributes`，在 `PlayerEntity#tick` 维护，仅服务端）：

| modifier | 运算 | 值 |
|---|---|---|
| `offhandonly.weapon_damage` | `ADDITION` | 副手武器加成 − 主手武器加成 |
| `offhandonly.damage_scale` | `MULTIPLY_TOTAL` | `−(1 − offhandAttackDamage)` |
| `offhandonly.weapon_speed` | `ADDITION` | 副手攻速加成 − 主手攻速加成 |

主手空手 + 副手钻石剑 → (1+7) × 0.5 = **4 点**；攻速 4−2.4 = **1.6**。
开了「副手空手攻击」后，副手为空也会生效，空手伤害同样打折（0.5 点）。

`attack` 里另有 3 处 `getMainHandStack()`（锋利附魔 ×2、`postHit` 扣耐久 ×1），
用 `@At(ordinal = 0/1/2)` 逐个改成 `effectiveCombatStack(player)`。

## 五、动画

`swingHand` 会记录 `LivingEntity#preferredHand`，第一人称 `HeldItemRenderer` 与第三人称
`BipedEntityModel#animateArms` 都按它决定摆哪只手。所以把原版写死的两处
`swingHand(MAIN_HAND)` 换成 `OFF_HAND`（`handleBlockBreaking` 与 `doAttack`）即可。

## 六、第一人称渲染

`HeldItemRenderer#renderFirstPersonItem` 是每只手唯一的第一人称绘制入口，在它的 HEAD 统一处理：

| 配置 | 行为 |
|---|---|
| `renderMainHand = false` | 主手不画 |
| `renderOffhand = false` | 副手不画 |
| `renderOffhand = true` 且副手为空 | **补画空手副手手臂** |

第三条是必须的：原版的空手分支只给主手画手臂（`if (isMainHand) renderArmHoldingItem(...)`），
副手空手时什么都不画。主手一关就会出现“两只手都没了”，所以这里复用同一个
`renderArmHoldingItem`（`@Shadow` 私有方法）把副手补上。

## 七、生效范围

`OffhandOnlyHelper.isActive(player)` 是所有行为改动的总闸门：

```java
survivalOnly && (player.isCreative() || player.isSpectator())  → 不生效
playerWhitelist 非空且玩家不在名单里                            → 不生效
```

- **仅生存模式**：打开后创造/旁观完全回到原版（包括第一人称渲染）。
- **玩家白名单**：填玩家名或 UUID，逗号分隔；**留空 = 所有人**生效。
- 多人服务器上，行为判定（挖掘/伤害）由**服务端**的配置决定，客户端配置只影响本地输入与渲染，两边最好填一样。

---

## 八、构建

需要 **JDK 17+**（本项目用 JDK 21 编译，产出 Java 17 字节码）。

```bash
./gradlew build          # Windows: gradlew.bat build
```

> 仓库自带 Gradle Wrapper（8.10.2）。之所以锁这个版本：Fabric Loom 1.6.x 不兼容 Gradle 9。

产物：`build/libs/offhandonly-1.3.0.jar`（带 `-sources` 的是源码包，不要装）。

**依赖**：Fabric Loader + Fabric API（ModMenu 需要）+ ModMenu。
ModMenu 在 `fabric.mod.json` 里是 `suggests` 而不是 `depends` ——
因为模组要能在专用服务器上跑，服务器不会有 ModMenu，写死 `depends` 会让服务器拒绝启动。

## 九、配置

ModMenu 模组列表里点齿轮，或直接改 `config/offhandonly.json`：

```json
{
  "survivalOnly": false,
  "playerWhitelist": [],
  "disableAttack": true,
  "disableBlockBreaking": true,
  "offhandMining": true,
  "offhandMiningSpeed": 0.5,
  "offhandAttack": true,
  "offhandAttackDamage": 0.5,
  "offhandEmptyHandAttack": false,
  "forceOffhandOnly": true,
  "renderMainHand": true,
  "renderOffhand": true,
  "debug": false
}
```

| 键 | 说明 |
|---|---|
| `survivalOnly` | 只在生存模式生效 |
| `playerWhitelist` | 玩家名 / UUID 列表，留空 = 全部 |
| `disableAttack` / `disableBlockBreaking` | 主手总开关，关掉即完全回到原版 |
| `offhandMining` / `offhandMiningSpeed` | 副手挖掘开关 / 效率（0.10~1.00，步进 0.05） |
| `offhandAttack` / `offhandAttackDamage` | 副手攻击开关 / 伤害（0.10~1.00，步进 0.05） |
| `offhandEmptyHandAttack` | 副手空手也能攻击 |
| `forceOffhandOnly` | 右键强制副手 |
| `renderMainHand` / `renderOffhand` | 第一人称主手 / 副手渲染 |

## 十、测试矩阵

| 场景 | 期望 |
|---|---|
| 主手满耐久镐 + 副手满耐久镐，挖石头 | 只扣副手镐耐久，主手不变 |
| 主手空手 + 副手时运 III 镐 | 掉落按时运 III 判定 |
| 主手空手 + 副手钻石剑打僵尸 | 4 点伤害，副手剑掉耐久 |
| 开启「副手空手攻击」，副手空手打僵尸 | 0.5 点伤害 |
| 关掉「渲染主手」，副手空手 | 屏幕左下出现空手副手手臂，右侧主手消失 |
| 开「仅生存模式」后切创造 | 主手恢复可用、主手重新渲染 |
| 白名单填自己名字 | 正常生效；填别人名字 → 完全原版 |

## 十一、已知边界

1. **多人服务器必须在服务端也装本模组**，且白名单/生存开关要在服务端配置里也设好。
2. 创造模式单点瞬破走 `doAttack`，副手有工具时会放行；创造模式本身不扣耐久。
3. 攻击的附魔伤害只镜像主手槽的 modifier，第三方模组若自己另写伤害计算（绕过 `PlayerEntity#attack`）需单独适配。
4. 少数实体交互在原版里写了 `if (hand == Hand.MAIN_HAND)` 才生效（村民交易、部分骑乘），
   严格只给 `OFF_HAND` 时按原版“副手右键”的结果走。
5. 配置界面按 240 逻辑像素高度排版（GUI 缩放 2 的 854×480 默认窗口），更小的窗口会挤但控件仍可点。
