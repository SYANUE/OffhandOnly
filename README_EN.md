# Offhand Only

[中文](README.md) | **English**

A Minecraft **1.20.1 + Fabric** mod: **left click and right click are completely disabled for your main hand — every interaction is forced onto your offhand.** Item behaviour is not reimplemented; the vanilla code still runs, it just uses the other hand.

- **Main hand**: cannot attack, cannot mine, cannot use items, cannot place blocks, cannot interact with entities.
- **Offhand**: shields, food, blocks, bows, ender pearls, boats, leads, buckets of water/lava, flint and steel, fishing rods, tridents, fireworks, potions… all behave exactly like vanilla.
- **Offhand mining**: configurable efficiency (default 50%), durability is consumed from the offhand tool, Fortune / Silk Touch are resolved with the offhand tool too.
- **Offhand attack**: configurable damage (default 50%), attack speed mirrors the offhand weapon, enchantments and durability follow the offhand.
- **Scope**: optionally survival-only, or restricted to a player whitelist.
- **First-person rendering**: main hand and offhand can be toggled independently, and the empty offhand is drawn too.
- Totem of Undying in the offhand keeps vanilla's death protection untouched.
- Inventory, `E`, `F` (swap offhand), dragging, shift-clicking and dropping all stay vanilla.

> The mod is **client + server** (`environment: "*"`) because mining speed, durability and attack damage are all server-authoritative.

---

## 1. Right click: only the hand changes

The 1.20.1 `MinecraftClient#doItemUse()` looks like this:

```java
for (Hand hand : Hand.values()) {          // the only hand iteration
    ... entity interaction / block interaction / item use ...
}
```

Restricting that `Hand.values()` call to `{OFF_HAND}` makes all three branches fall through to the offhand automatically. A `ClientPlayerInteractionManagerMixin` acts as a safety net: if other code calls one of the four interaction entry points with `MAIN_HAND`, the hand is rewritten as well.

## 2. Left click: gates

| Entry point | Attack | Mining |
|---|---|---|
| `MinecraftClient#doAttack()` | main hand disabled + offhand holds a weapon → allow; otherwise `setReturnValue(false)` | creative instant-break goes through the same path |
| `MinecraftClient#handleBlockBreaking(boolean)` | — | offhand holds a tool → allow; `breaking == false` is always allowed |

Both gates check `isActive(player)` first: when "survival only" / the whitelist does not match, everything runs vanilla.

## 3. Mining: speed + durability

The mining chain is `BlockState#calcBlockBreakingDelta` → `Block#calcBlockBreakingDelta` → `player.canHarvest(state)` + `player.getBlockBreakingSpeed(state)`.

**Speed** (`PlayerEntityMixin`):

| Injection point | Replaced with |
|---|---|
| `inventory.getBlockBreakingSpeed(state)` | `getMiningSpeedMultiplier` of the offhand stack |
| `EnchantmentHelper.getEfficiency(this)` | Efficiency level of the offhand stack |
| `this.getMainHandStack()` | offhand stack |
| `inventory.getMainHandStack()` inside `canHarvest` | offhand stack |
| `@ModifyReturnValue getBlockBreakingSpeed` | × `offhandMiningSpeed` |

**Durability** (`ServerPlayerInteractionManagerMixin`):

```java
// ServerPlayerInteractionManager#tryBreakBlock
ItemStack tool = this.player.getMainHandStack();   // main hand — this is what gets damaged
ItemStack snapshot = tool.copy();
tool.postMine(world, state, pos, this.player);     // durability is consumed here
if (removed && canHarvest) block.afterBreak(..., snapshot);   // Fortune / Silk Touch use it too
```

That method contains **two** `getMainHandStack()` calls (the other one feeds `Item.canMine`). Both are `@Redirect`ed to `effectiveMiningStack(player)`, so durability, Fortune and Silk Touch all line up. The client never calls `postMine` — durability is server-side only — so this single class is enough.

## 4. Attack: temporary attribute modifiers

`PlayerEntity#attack` reads `getAttributeValue(GENERIC_ATTACK_DAMAGE)`, and the equipment bonus on that attribute is applied by vanilla from the `MAINHAND` slot, so the offhand item's modifiers never get in. Instead of re-implementing the attribute maths, the mod **adds temporary modifiers** to the attribute (`OffhandCombatAttributes`, maintained in `PlayerEntity#tick`, server side only):

| Modifier | Operation | Value |
|---|---|---|
| `offhandonly.weapon_damage` | `ADDITION` | offhand weapon bonus − main hand weapon bonus |
| `offhandonly.damage_scale` | `MULTIPLY_TOTAL` | `−(1 − offhandAttackDamage)` |
| `offhandonly.weapon_speed` | `ADDITION` | offhand attack speed bonus − main hand attack speed bonus |

Empty main hand + diamond sword in the offhand → (1+7) × 0.5 = **4 damage**; attack speed 4 − 2.4 = **1.6**.
With "offhand empty-hand attack" enabled, an empty offhand works too and is scaled the same way (0.5 damage).

`attack` also contains 3 `getMainHandStack()` calls (Sharpness ×2, `postHit` durability ×1); each is redirected with `@At(ordinal = 0/1/2)` to `effectiveCombatStack(player)`.

## 5. Animation

`swingHand` records `LivingEntity#preferredHand`, and both the first-person `HeldItemRenderer` and the third-person `BipedEntityModel#animateArms` use it to decide which arm swings. So the two hard-coded `swingHand(MAIN_HAND)` calls (in `handleBlockBreaking` and `doAttack`) are simply switched to `OFF_HAND`.

## 6. First-person rendering

`HeldItemRenderer#renderFirstPersonItem` is the only first-person draw entry point per hand, so everything is handled at its HEAD:

| Config | Behaviour |
|---|---|
| `renderMainHand = false` | main hand is not drawn |
| `renderOffhand = false` | offhand is not drawn |
| `renderOffhand = true` and offhand empty | **the empty offhand arm is drawn** |

The third case matters: vanilla's empty-hand branch only draws the arm for the **main** hand (`if (isMainHand) renderArmHoldingItem(...)`), so an empty offhand renders nothing at all. Hiding the main hand would otherwise leave you with no hands, so the same `renderArmHoldingItem` (a `@Shadow`ed private method) is reused for the offhand.

## 7. Scope

`OffhandOnlyHelper.isActive(player)` is the master gate for every behavioural change:

```java
survivalOnly && (player.isCreative() || player.isSpectator())  → inactive
playerWhitelist non-empty and the player is not listed          → inactive
```

- **Survival only**: when enabled, creative / spectator go back to vanilla completely (including first-person rendering).
- **Player whitelist**: player names or UUIDs, comma separated; **empty = everyone**.
- On a multiplayer server, behaviour (mining / damage) is decided by the **server** config; the client config only affects local input and rendering. Keep both in sync.

---

## 8. Building

Requires **JDK 17+** (this project is built with JDK 21, targeting Java 17 bytecode).

```bash
./gradlew build          # Windows: gradlew.bat build
```

> The repo ships a Gradle Wrapper (8.10.2). That version is pinned because Fabric Loom 1.6.x does not support Gradle 9.

Output: `build/libs/offhandonly-1.3.0.jar` (the `-sources` jar is a source package — do not install it).

**Dependencies**: Fabric Loader + Fabric API (needed by ModMenu) + ModMenu.
ModMenu is declared as `suggests`, not `depends`, in `fabric.mod.json` — the mod has to run on dedicated servers, which will never have ModMenu, and a hard `depends` would make the server refuse to start.

## 9. Configuration

Open the config screen from the ModMenu list, or edit `config/offhandonly.json` directly:

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

| Key | Description |
|---|---|
| `survivalOnly` | only active in survival mode |
| `playerWhitelist` | player names / UUIDs; empty = everyone |
| `disableAttack` / `disableBlockBreaking` | master switches for the main hand; off = pure vanilla |
| `offhandMining` / `offhandMiningSpeed` | offhand mining toggle / efficiency (0.10–1.00, step 0.05) |
| `offhandAttack` / `offhandAttackDamage` | offhand attack toggle / damage (0.10–1.00, step 0.05) |
| `offhandEmptyHandAttack` | allow attacking with an empty offhand |
| `forceOffhandOnly` | force right-click onto the offhand |
| `renderMainHand` / `renderOffhand` | first-person main hand / offhand rendering |

## 10. Test matrix

| Scenario | Expected |
|---|---|
| Full-durability pickaxe in both hands, mine stone | only the offhand pickaxe loses durability |
| Empty main hand + Fortune III pickaxe in the offhand | drops are rolled with Fortune III |
| Empty main hand + diamond sword in the offhand, hit a zombie | 4 damage, the offhand sword loses durability |
| Enable "offhand empty-hand attack", punch with an empty offhand | 0.5 damage |
| Disable "render main hand", empty offhand | the empty offhand arm appears on the left, the main hand disappears |
| Enable "survival only", then switch to creative | main hand works again and is rendered again |
| Whitelist your own name | works normally; whitelist someone else → pure vanilla |

## 11. Known limitations

1. **Multiplayer servers must install the mod on the server too**, and the whitelist / survival switch has to be set in the server config as well.
2. Creative instant-break goes through `doAttack`, which is allowed when the offhand holds a tool; creative mode itself does not consume durability.
3. Enchantment damage on attack only mirrors main-hand-slot modifiers. Third-party mods that implement their own damage calculation (bypassing `PlayerEntity#attack`) need separate handling.
4. A few vanilla entity interactions are gated behind `if (hand == Hand.MAIN_HAND)` (villager trading, some mounting logic). Since only `OFF_HAND` is ever passed, they behave the way vanilla behaves when you right-click with the offhand.
5. The config screen is laid out for 240 logical pixels of height (the default 854×480 window at GUI scale 2). Smaller windows get cramped, but the widgets stay clickable.

---

## Related

- [中文说明](README.md)
- [Publishing to Modrinth](docs/PUBLISHING-MODRINTH.md) — a Chinese-language walkthrough that includes a **ready-to-paste bilingual project description**
