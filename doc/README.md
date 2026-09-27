# Mob category catalog

`mob_categories.xlsx` is the editable source for the vanilla mob catalogue. The `Mobs` sheet contains each registry ID, the descriptive fields from the original table, and the category flags. Namespaced IDs also support entities added by mods.

## Export to JSON

From the project root, run:

```powershell
python doc/excel_to_json.py
```

The converter needs Python 3 and `openpyxl`. If that package is not installed, run `python -m pip install openpyxl` once.

The converter writes `doc/MobCategory.json`. You can also provide input and output paths:

```powershell
python doc/excel_to_json.py path\to\catalog.xlsx path\to\MobCategory.json
```

The converter checks required columns, duplicate entity IDs, and boolean values before writing. Edit the workbook, then run the converter again whenever you change categories.

## Load the exported file in a world

Copy `doc/MobCategory.json` to the active save's `config/enchlib/MobCategory.json` path. In game, run `/plusec-debug reload` to load the new data. When this JSON file exists, it takes precedence over the legacy `MobCategory.config` file. The schema retains mob names and behavior flags for documentation, while EnchLib reads the `id` and `categories` fields.

For quick changes in a single-player world, pause the game and open EnchLib's configuration screen from Mod Menu. The screen starts with the mob under your crosshair when available. Enter an entity ID and comma-separated categories, then save. On a server, the edit is sent through `/plusec-debug mob set` and requires the permission level for debug commands.

## Categories

The category labels are user-defined tags, not Minecraft's built-in spawn groups. A mob can have any number of them. The original workbook labels are preserved as lowercase JSON tags: `animals`, `magik`, `undead`, `rebel`, `fungi`, `hell`, `void`, `water`, `arthropods`, `cubic`, and `flying`.

Mob variants that share a registry type use one row. The workbook combines the Ghastling with Happy Ghast and the tamed dog with Wolf. Giant is included because it is a registered vanilla entity type, although it does not normally spawn.
