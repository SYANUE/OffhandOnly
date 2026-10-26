# 发布到 Modrinth 完整指南

> 这份文档面向本项目的发布流程（OffhandOnly 1.20.1 / Fabric）。
> Modrinth 的后台界面全是英文，所以字段名保留英文原文，说明用中文。
> 第 3 节提供**中英双语的项目简介**，可以直接复制粘贴到 Modrinth。

---

## 0. 发布前需要准备的东西

| 材料 | 从哪里来 | 备注 |
|---|---|---|
| `offhandonly-1.3.0.jar` | `./gradlew build` → `build/libs/` | **不要**上传 `-sources.jar` |
| 图标 | `src/main/resources/assets/offhandonly/icon.png` | Modrinth 要求正方形；当前是 128×128，能用但会略糊，建议 512×512 |
| 项目简介（中/英） | 本文第 3 节 | 直接复制 |
| 许可证 | 仓库根目录 `LICENSE`（MIT） | Modrinth 必须选一个 |
| 源码链接 | `https://github.com/SYANUE/OffhandOnly` | 填在 Links → Source |

---

## 1. 创建项目（Create a project）

1. 登录 [modrinth.com](https://modrinth.com) → 右上角头像 → **Dashboard**
2. 点 **Create a project** → 选 **Mod**（不是 Modpack / Resource Pack / Shader）
3. 填下面这些字段：

| 字段 | 填什么 | 说明 |
|---|---|---|
| **Name** | `Offhand Only` | 展示名，可以改 |
| **Slug** | `offhand-only` | URL 的一部分，**建好后改动会导致链接变化**，想清楚再填 |
| **Summary** | 见下方 | 一句话简介，**上限 256 字符** |
| **Description** | 第 3 节的中文或英文正文 | 支持 Markdown |
| **Categories** | `Utility`、`Game Mechanics` | 最多选几个，别乱选 |
| **Environment** | **Client and server** | ⚠️ 见第 4 节，选错会误导服务器用户 |
| **License** | `MIT License` | 和仓库里的 LICENSE 一致 |
| **Links → Source** | `https://github.com/SYANUE/OffhandOnly` | |
| **Links → Issues** | `https://github.com/SYANUE/OffhandOnly/issues` | |

**Summary 建议**（英文，158 字符）：

```
Disables main-hand attack and use entirely — every interaction goes to your offhand, while item behaviour stays 100% vanilla.
```

**Summary 建议**（中文）：

```
主手左键/右键完全禁用，所有交互强制交给副手，物品行为 100% 沿用原版。
```

> Modrinth 的 Summary 只显示一行，中文版更适合中文用户，但英文版能被更多人看懂。
> 建议：**Summary 用英文**，正文里两种语言都放（见第 3 节）。

---

## 2. 上传版本（Upload a version）

项目建好后，进项目页 → **Versions** → **Create version**。

| 字段 | 填什么 |
|---|---|
| **Name** | `1.3.0` |
| **Version number** | `1.3.0`（必须和 `gradle.properties` 里的 `mod_version` 一致） |
| **Release channel** | 首次发布选 **Release**；功能没定型就选 Beta |
| **Loaders** | **Fabric**（不要勾 Forge / Quilt） |
| **Game versions** | **1.20.1** |
| **Dependencies** | 见下 |
| **File** | 拖入 `offhandonly-1.3.0.jar` |

### 依赖怎么填

| 依赖 | 类型 | 理由 |
|---|---|---|
| **Fabric API** | **Required** | 配置界面走 ModMenu，而 ModMenu 依赖 Fabric API |
| **ModMenu** | **Optional** | 只用来提供设置界面；没有它模组照常工作，改 JSON 即可 |

> 说实话：本模组自己的 `fabric.mod.json` 里**没有** `fabric-api` 依赖，只有 `modmenu` 是 `suggests`。
> 但玩家装 ModMenu 时必然需要 Fabric API，所以把它标成 Required 是符合实际的。
> **千万不要**把 ModMenu 标成 Required —— 专用服务器不会装它。

上传后 Modrinth 会自动读 `fabric.mod.json`，版本号、loader、游戏版本通常能自动填对，但**务必自己核对一遍**。

---

## 3. 项目简介（中英双语，可直接粘贴）

Modrinth 的 Description 支持 Markdown。建议**英文在前、中文在后**，用分隔线隔开。

### 3.1 English

````markdown
# Offhand Only

**Left click and right click are completely disabled for your main hand — every interaction is forced onto your offhand.**
Item behaviour is not reimplemented: the vanilla code still runs, it just uses the other hand.

## Features

- **Main hand**: cannot attack, cannot mine, cannot use items, cannot place blocks, cannot interact with entities.
- **Offhand**: shields, food, blocks, bows, ender pearls, boats, leads, buckets, flint and steel, fishing rods, tridents, fireworks, potions — all behave exactly like vanilla.
- **Offhand mining**: configurable efficiency (default 50%), durability is consumed from the offhand tool, Fortune / Silk Touch resolve with the offhand tool too.
- **Offhand attack**: configurable damage (default 50%), attack speed mirrors the offhand weapon, enchantments and durability follow the offhand.
- **Scope**: optionally survival-only, or restricted to a player whitelist.
- **First-person rendering**: main hand and offhand can be toggled independently, and the empty offhand arm is drawn too.
- Totem of Undying in the offhand keeps vanilla's death protection untouched.
- Inventory, `E`, `F`, dragging, shift-clicking and dropping all stay vanilla.

## How it works

Instead of simulating shield / bow / food / boat logic, the mod only changes **which hand** the vanilla code looks at:

- Right click: the `Hand.values()` loop inside `MinecraftClient#doItemUse()` is narrowed to `OFF_HAND`.
- Left click: `doAttack()` and `handleBlockBreaking()` are gated on whether the offhand holds a tool / weapon.
- Mining: block-breaking speed, Efficiency, `canHarvest` and `postMine` durability all read the offhand stack.
- Attack: temporary attribute modifiers mirror the offhand weapon's damage and attack speed onto `GENERIC_ATTACK_DAMAGE` / `GENERIC_ATTACK_SPEED`.

Because the vanilla pipeline is preserved, vanilla items and most modded items that follow the vanilla interaction system work without any per-item code.

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Mod loader | Fabric Loader |
| Fabric API | required (used by ModMenu) |
| ModMenu | optional — only needed for the in-game config screen |

## ⚠️ Multiplayer

This mod is **client + server**. Mining speed, durability and attack damage are server-authoritative, so **install it on the server as well**, otherwise the client and the server will disagree about mining progress.

The player whitelist and the survival-only switch must also be configured in the **server**'s `config/offhandonly.json`.

## Configuration

`config/offhandonly.json`, or the config screen in ModMenu:

| Key | Description |
|---|---|
| `survivalOnly` | only active in survival mode |
| `playerWhitelist` | player names / UUIDs, comma separated; empty = everyone |
| `disableAttack` / `disableBlockBreaking` | master switches for the main hand |
| `offhandMining` / `offhandMiningSpeed` | offhand mining toggle / efficiency (0.10–1.00) |
| `offhandAttack` / `offhandAttackDamage` | offhand attack toggle / damage (0.10–1.00) |
| `offhandEmptyHandAttack` | allow attacking with an empty offhand |
| `forceOffhandOnly` | force right-click onto the offhand |
| `renderMainHand` / `renderOffhand` | first-person rendering toggles |

## Known limitations

- A few vanilla entity interactions are gated behind `if (hand == Hand.MAIN_HAND)` (villager trading, some mounting logic). Since only `OFF_HAND` is ever passed, they behave like vanilla offhand right-click.
- Third-party mods that implement their own damage calculation (bypassing `PlayerEntity#attack`) need separate handling.

## License

MIT — source on [GitHub](https://github.com/SYANUE/OffhandOnly).
````

### 3.2 中文

````markdown
---

# Offhand Only（仅副手）

**主手左键 / 右键完全禁用，所有交互强制交给副手。**
物品行为没有重新实现一遍——跑的还是原版代码，只是换了只手。

## 功能

- **主手**：不能攻击、不能挖方块、不能使用物品、不能放方块、不能与实体交互。
- **副手**：盾牌、食物、方块、弓、末影珍珠、船、拴绳、水桶 / 岩浆桶、打火石、钓鱼竿、三叉戟、烟花、药水……全部按原版行为工作。
- **副手挖掘**：效率可配（默认 50%），耐久从副手工具扣，时运 / 精准采集也按副手工具判定。
- **副手攻击**：伤害可配（默认 50%），攻速镜像副手武器，附魔与耐久都走副手。
- **生效范围**：可限制为仅生存模式，也可按玩家白名单只对指定玩家生效。
- **第一人称渲染**：主手 / 副手可分别开关，副手空手时也会画出手臂。
- 副手的不死图腾保持原版致死保护，完全没动。
- 物品栏、`E`、`F` 换副手、拖拽、Shift 快速移动、丢弃，全部保持原版。

## 实现思路

不为盾牌 / 弓 / 食物 / 船单独写模拟逻辑，只改变**原版代码看哪只手**：

- 右键：把 `MinecraftClient#doItemUse()` 里的 `Hand.values()` 遍历收窄成 `OFF_HAND`。
- 左键：`doAttack()` 与 `handleBlockBreaking()` 按"副手是否拿着工具 / 武器"决定放行。
- 挖掘：破坏速度、效率附魔、`canHarvest`、`postMine` 扣耐久全部改读副手栈。
- 攻击：用临时属性 modifier 把副手武器的伤害与攻速镜像到 `GENERIC_ATTACK_DAMAGE` / `GENERIC_ATTACK_SPEED` 上。

因为原版管线完整保留，原版物品以及大多数遵循原版交互系统的模组物品都能直接工作，不需要任何逐物品适配。

## 前置

| | |
|---|---|
| Minecraft | 1.20.1 |
| 模组加载器 | Fabric Loader |
| Fabric API | 必需（ModMenu 需要） |
| ModMenu | 可选，只用来提供游戏内配置界面 |

## ⚠️ 多人游戏

本模组是**双端**的。挖掘速度、耐久、攻击伤害全部由服务端计算，所以**服务器也必须安装**，否则客户端和服务端对挖掘进度会各算各的。

玩家白名单和"仅生存模式"开关，也要在**服务端**的 `config/offhandonly.json` 里配一遍。

## 配置

`config/offhandonly.json`，或 ModMenu 里的设置界面：

| 键 | 说明 |
|---|---|
| `survivalOnly` | 只在生存模式生效 |
| `playerWhitelist` | 玩家名 / UUID，逗号分隔；留空 = 全部生效 |
| `disableAttack` / `disableBlockBreaking` | 主手总开关 |
| `offhandMining` / `offhandMiningSpeed` | 副手挖掘开关 / 效率（0.10–1.00） |
| `offhandAttack` / `offhandAttackDamage` | 副手攻击开关 / 伤害（0.10–1.00） |
| `offhandEmptyHandAttack` | 副手空手也能攻击 |
| `forceOffhandOnly` | 右键强制副手 |
| `renderMainHand` / `renderOffhand` | 第一人称渲染开关 |

## 已知边界

- 少数原版实体交互写了 `if (hand == Hand.MAIN_HAND)` 才生效（村民交易、部分骑乘逻辑）。因为只传 `OFF_HAND`，它们会按"原版副手右键"的结果走。
- 第三方模组若自己另写伤害计算（绕过 `PlayerEntity#attack`），需要单独适配。

## 许可证

MIT，源码在 [GitHub](https://github.com/SYANUE/OffhandOnly)。
````

---

## 4. 本项目特有的坑（务必看）

| 坑 | 后果 | 正确做法 |
|---|---|---|
| **Environment 选了 "Client only"** | 服务器管理员以为只装客户端就行，结果挖掘进度回跳、耐久不对 | 必须选 **Client and server** |
| **把 ModMenu 标成 Required** | 专用服务器因为缺 ModMenu 直接拒绝启动 | ModMenu 标 **Optional** |
| **上传了 `-sources.jar`** | 玩家装了没反应（里面没有 class） | 只传 `offhandonly-1.3.0.jar` |
| **版本号和 `gradle.properties` 不一致** | 游戏内显示的版本和下载页对不上 | 两边都改成同一个数 |
| **Loader 勾了 Forge/Quilt** | 装不上的用户疯狂报错 | 只勾 **Fabric** |
| **没写"服务器也要装"** | 多人玩家体验异常还找不到原因 | 简介里加醒目提示（第 3 节已包含） |

---

## 5. 图标与画廊（Gallery）

- **图标**：Modrinth 项目图标是正方形，建议 **512×512**。当前 `icon.png` 是 128×128，能显示但会略糊。
- **画廊**：强烈建议放 2–4 张图，能显著提高点击率：
  1. 第一人称视角：主手空、副手举盾（展示"只剩副手"）
  2. 挖掘对比：副手镐挖石头，右下角耐久变化
  3. 配置界面截图（ModMenu 里打开的那个）
  4. 攻击测试：副手剑打怪的血量变化

---

## 6. 发布后

1. **更新版本**：改 `gradle.properties` 的 `mod_version` → `./gradlew build` → Modrinth 项目页 **Create version** 传新 jar。
   （旧版本不要删，玩家需要按版本回退）
2. **写 Changelog**：每个版本填一句人话，比如"修复副手挖掘扣主手耐久的 bug"。
3. **Featured version**：把最新 Release 设为 Featured，下载页默认给这个。
4. **Issue 反馈**：Modrinth 的 Issues 建议关掉，统一引导到 GitHub Issues，避免两头看。

---

## 7. 发布前检查清单

```
[ ] jar 能在干净的 1.20.1 + Fabric 环境里启动，无 mixin 报错
[ ] 只上传 offhandonly-1.3.0.jar（不是 -sources.jar）
[ ] Version number == gradle.properties 的 mod_version == fabric.mod.json 里渲染出来的版本
[ ] Loaders = Fabric，Game version = 1.20.1
[ ] Environment = Client and server
[ ] Fabric API = Required，ModMenu = Optional
[ ] 简介里写明了"服务器也要装"
[ ] License = MIT
[ ] Links 填了 Source 和 Issues
[ ] 图标是正方形
```
