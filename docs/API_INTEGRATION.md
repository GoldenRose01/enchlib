# EnchLib API integration guide

This guide is for Fabric mod developers who want their enchantments, enchanting tables, and mob categories to work with EnchLib. EnchLib remains a separate mod and can be installed on its own.

## What works without changing another mod

EnchLib reads enchantments from Minecraft's active registry, including entries added by other mods, and exposes them in its configuration UI and config files. Modded entity types can be assigned categories in the per-world mob catalog by registry ID. This lets players manage third-party content without a hard dependency or a patch in every target mod.

An EnchLib setting affects gameplay only where EnchLib owns that operation or the feature reads the EnchLib API. For example, a modded enchanting screen must consult EnchLib to enforce its per-table availability and chance settings. Without a bridge, EnchLib can still display and store the settings, but cannot safely replace another mod's private enchanting rules. The optional bridge below is the supported way to make those settings effective.

## Optional dependency setup

Compile against EnchLib's API, but keep it optional at runtime. The exact artifact coordinates depend on how the API JAR is distributed; in a local Loom workspace, a file dependency can be used:

```groovy
dependencies {
    modCompileOnly files("libs/enchlib-mc26.3-1.4.1.jar")
}
```

Do not shade EnchLib into your JAR. In `fabric.mod.json`, use `suggests` when the integration is optional:

```json
"suggests": {
  "enchlib": "*"
}
```

If your mod cannot compile against EnchLib, call it through a small reflection bridge after checking `FabricLoader.getInstance().isModLoaded("enchlib")`. Keep the rest of your mod functional when EnchLib is absent. Eldritch Surge uses this pattern in `EnchLibIntegration`.

## Register enchantment defaults

Call registration after your enchantments have been registered, such as from your mod initializer. Use complete registry IDs. EnchLib adds missing records and preserves a player's current choices on later launches.

```java
Map<String, Integer> maxLevels = Map.of(
    "examplemod:arcane_edge", 3
);
Map<String, List<String>> itemRules = Map.of(
    "examplemod:arcane_edge", List.of("swords", "!wooden")
);
Map<String, List<String>> targetMobCategories = Map.of(
    "examplemod:arcane_edge", List.of("undead", "magical")
);

EnchantLibAPI.registerEnchantments(maxLevels, itemRules, targetMobCategories);
```

The two-map overload remains available if the enchantment does not target mob categories. Item rules are the same selectors used by the EnchLib Categories editor: vanilla item groups/tag selectors (for example `#minecraft:enchantable/weapon`), broad groups such as `swords`, `weapons`, or `armor`, and namespaced item IDs such as `minecraft:netherite_sword`. Positive selectors are combined; `!selector` excludes matching items, and `!all` restricts the enchantment to enchanted books. Keep defaults in your mod so users can reset an edited value to the mod's preset.

## Read and apply settings at runtime

The API exposes availability, maximum level, item-category rules, rarity, incompatibilities, and table rules. Read the setting at the point where your mod chooses or applies the enchantment:

```java
String id = "examplemod:arcane_edge";
if (!EnchantLibAPI.isEnabled(id)) return;
int cap = EnchantLibAPI.getMaxLevel(id) == null
        ? 3
        : EnchantLibAPI.getMaxLevel(id);
int appliedLevel = Math.min(requestedLevel, cap);
```

`setEnabled(id, enabled)` and `setMaxLevel(id, level)` update the global EnchLib configuration immediately. `getMaxLevel` returns `null` when no override exists, so retain your own default in that case. A registered default is a reset preset; it must not silently overwrite a value the player has already changed.

## Integrate an enchanting table

Register a stable namespaced table ID. Table IDs are the keys saved in EnchLib's settings, so do not change them between releases. Supply defaults for your own enchantments and any vanilla enchantments your table supports:

```java
String tableId = "examplemod:arcane_enchanting_table";
Map<String, Boolean> enabledDefaults = Map.of("examplemod:arcane_edge", true);
Map<String, Integer> chanceDefaults = Map.of("examplemod:arcane_edge", 75);
EnchantLibAPI.registerEnchantingTableDefaults(
        tableId, "Arcane Enchanting Table", enabledDefaults, chanceDefaults);
```

When generating offers, consult `getTableEnabled(enchantmentId, tableId)` and `getTableChance(enchantmentId, tableId)`. Both return `null` if no user override or preset exists; use your table's own default in that case. Chance is an integer from 0 through 100. For configurable rarity, `getRarity` returns `common`, `uncommon`, `rare`, or `very_rare`; the EnchLib editor combines the per-table chance with weights 100, 60, 30, and 5 respectively. To honor the editor, apply the selected rarity weight to the table chance when rolling offers. `setTableEnabled`, `setTableChance`, `setRarity`, and `resetTableRule` are available for integrations and reset actions.

Example compatibility check:

```java
Boolean enabled = EnchantLibAPI.getTableEnabled(id, tableId);
if (enabled != null && !enabled) continue;
Integer chance = EnchantLibAPI.getTableChance(id, tableId);
int effectiveChance = chance == null ? yourDefaultChance : chance;
// Apply EnchLib rarity weighting when using EnchLib's configured rarity.
```

Avoid registering the same table under aliases: users should see one setting per actual table.

## Share mob categories

Mob categories are world-specific and keyed by the entity registry ID. Register defaults during mod initialization; user-defined assignments in `MobCategory.json` or the legacy `MobCategory.config` take precedence.

```java
Map<String, List<String>> mobDefaults = Map.of(
    "examplemod:crystal_beast", List.of("magical", "beast", "hostile")
);
EnchantLibAPI.registerMobCategories(mobDefaults);
```

At runtime, use `hasMobCategory(server, entity, "magical")` or `getMobCategories(server, entity)`. To update a world assignment, call `setMobCategories(server, "examplemod:crystal_beast", List.of("magical", "boss"))`. The editor and debug commands also accept IDs from other mods; no changes to the entity's owning mod are required to categorize it.

`registerEnchantments(..., targetMobCategories)` describes which mob categories an enchantment targets; `registerMobCategories(...)` describes which categories belong to an entity type. These are separate mappings.

## Eldritch Surge reference integration

Eldritch Surge is an optional integration example. When EnchLib is installed, its bridge registers custom enchantment caps and item selectors, its mob-target categories, vanilla and advanced enchanting table presets, and reads player settings when generating offers. When EnchLib is absent, Eldritch Surge falls back to its own configuration. This keeps both JARs independent while making the EnchLib settings effective in the systems that explicitly consume them.

## API summary

| Purpose | API |
| --- | --- |
| Register enchantments and reset presets | `registerEnchantments(maxLevels, itemCategories[, targetMobCategories])` |
| Read/write availability and cap | `isEnabled`, `setEnabled`, `getMaxLevel`, `setMaxLevel` |
| Register/read/write table settings | `registerEnchantingTableDefaults`, `getTableEnabled`, `setTableEnabled`, `getTableChance`, `setTableChance`, `resetTableRule` |
| Read/write rarity and conflicts | `getRarity`, `setRarity`, `getIncompatibleEnchantments`, `setIncompatibleEnchantments` |
| Read/write item rules | `getItemCategories`, `setItemCategories` |
| Register/query/update mob categories | `registerMobCategories`, `getMobCategories`, `hasMobCategory`, `setMobCategories` |

All API IDs should be stable namespaced IDs (`namespace:path`). Registration methods are safe to repeat; defaults fill missing records and preserve player choices.
