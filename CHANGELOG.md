# Changelog

## 1.6.1

- Follow-up release for Minecraft 26.3 with the current command parsing, enchantment application, and inventory synchronization fixes.

## 1.6.0

- Enchantment commands now resolve unqualified IDs against the live registry, including unique modded enchantments.
- Namespaced autocomplete suggestions are quoted so command parsing accepts them reliably.
- Applying and removing enchantments now synchronizes inventory changes to the client.
- Failures while applying or removing enchantments are recorded in the log for easier diagnosis.
