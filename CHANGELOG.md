# Changelog

## 1.7.0

- Added per-enchantment villager trade settings, with separate controls for enchanted books and enchanted items in both Cloth Config and YACL.
- Enchantments tagged `minecraft:curse` are excluded from new villager offers by default, including vanilla, modded, and datapack enchantments. Individual overrides can allow them again.
- Trade settings are saved per world and use the existing server-authoritative configuration synchronization. Globally disabled enchantments remain excluded.
- Existing configurations automatically use the tag-based defaults; resetting trade settings restores those defaults.
- Newly generated offers containing an excluded enchantment are skipped as a whole. Existing offers and purchased items are unchanged; enabling an enchantment does not add it to trade generation pools.
- Added builds for Minecraft 26.3 and 26.4 Snapshot 7. The snapshot build uses Fabric API 0.165.4+26.4 and Fabric Loader 0.20.0.

## 1.6.1

- Follow-up release for Minecraft 26.3 with the current command parsing, enchantment application, and inventory synchronization fixes.

## 1.6.0

- Enchantment commands now resolve unqualified IDs against the live registry, including unique modded enchantments.
- Namespaced autocomplete suggestions are quoted so command parsing accepts them reliably.
- Applying and removing enchantments now synchronizes inventory changes to the client.
- Failures while applying or removing enchantments are recorded in the log for easier diagnosis.
