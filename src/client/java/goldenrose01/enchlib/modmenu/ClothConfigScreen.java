package goldenrose01.enchlib.modmenu;

import goldenrose01.enchlib.api.EnchantLibAPI;
import goldenrose01.enchlib.config.GlobalConfigIO;
import goldenrose01.enchlib.config.MobCategoryManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Optional Cloth Config editor. Changes are submitted only by Save & Done. */
public final class ClothConfigScreen {
    private static final String[] RARITIES = {"common", "uncommon", "rare", "very_rare"};

    private ClothConfigScreen() {}

    public static Screen create(Screen parent) {
        var client = Minecraft.getInstance();
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(text("EnchLib"))
                .setAlwaysShowTabs(true).setShouldListSmoothScroll(true).setShouldTabsSmoothScroll(true);
        var entries = builder.entryBuilder();
        var availability = builder.getOrCreateCategory(text("Available"));
        var levels = builder.getOrCreateCategory(text("Max Levels"));
        var rarity = builder.getOrCreateCategory(text("Rarity"));
        var tables = builder.getOrCreateCategory(text("Tables"));
        var villagers = builder.getOrCreateCategory(text("Villagers"));
        villagers.addEntry(entries.startTextDescription(text("Allow enchantments in newly generated villager books and items. Curses are excluded by default. Existing offers are unchanged.")).build());
        var items = builder.getOrCreateCategory(text("Items"));
        var incompatibility = builder.getOrCreateCategory(text("Incompatibilities"));
        var mobs = builder.getOrCreateCategory(text("Mobs"));
        var gameplay = builder.getOrCreateCategory(text("Gameplay"));

        var treecutGroup = entries.startSubCategory(text("Tree Cutting")).setExpanded(true);
        treecutGroup.add(entries.startTextDescription(text(
                "Tree felling configuration when sneaking with an axe (stone tier or higher). If Eldrich Surge is installed, automatically managed: fell without enchantment, chop with Lumberjack.")).build());
        String[] treeModes = {"off", "fell", "chop"};
        String currentTreeMode = goldenrose01.enchlib.tree.TreeCutConfig.INSTANCE.getMode().getId();
        treecutGroup.add(entries.startSelector(text("Treecut Mode"), treeModes, currentTreeMode)
                .setDefaultValue("fell")
                .setTooltip(text("off: vanilla break\nfell: tree falls log by log\nchop: breaks whole tree"))
                .setSaveConsumer(changed(currentTreeMode, v -> goldenrose01.enchlib.tree.TreeCutConfig.INSTANCE.setMode(goldenrose01.enchlib.tree.TreeCutMode.Companion.fromString(v))))
                .build());
        gameplay.addEntry(treecutGroup.build());

        var weaponsGroup = entries.startSubCategory(text("Axes as Weapons")).setExpanded(true);
        weaponsGroup.add(entries.startTextDescription(text(
                "When enabled, axes lose only 1 durability point when attacking mobs (like swords), and can receive sword enchantments in both the enchanting table and anvil.")).build());
        weaponsGroup.add(entries.startBooleanToggle(text("Axes as Weapons Enabled"), goldenrose01.enchlib.weapons.AxesAreWeaponsConfig.isEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(goldenrose01.enchlib.weapons.AxesAreWeaponsConfig::setEnabled)
                .build());
        gameplay.addEntry(weaponsGroup.build());
        availability.addEntry(entries.startTextDescription(text(
                "World settings. Expand an enchantment to edit it; use Reset to restore its preset. Changes apply with Save & Done. Multiplayer requires operator level 2.")).build());
        // Without a world there is no authoritative configuration to edit.
        builder.setEditable(client.level != null);
        if (client.level == null) availability.addEntry(entries.startTextDescription(text("Open a world to edit its configuration.")).build());

        Properties presets = loadPresets();
        Map<String, GlobalConfigIO.EnchantmentDetails> details = GlobalConfigIO.INSTANCE.readDetails()
                .getEnchantments().stream().collect(Collectors.toMap(GlobalConfigIO.EnchantmentDetails::getId, e -> e, (a, b) -> b));
        Map<String, Boolean> enabled = GlobalConfigIO.INSTANCE.readAvailable().stream().collect(Collectors.toMap(
                GlobalConfigIO.AvailableEnch::getId, GlobalConfigIO.AvailableEnch::getEnabled, (a, b) -> b));
        Set<String> ids = new TreeSet<>(details.keySet());
        ids.addAll(enabled.keySet());
        if (client.level != null) ids.addAll(client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .keySet().stream().map(Identifier::toString).toList());
        presets.stringPropertyNames().forEach(key -> ids.add(key.substring(key.indexOf(':') + 1)));

        Map<String, List<String>> idsByNamespace = new TreeMap<>();
        for (String id : ids) {
            String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
            idsByNamespace.computeIfAbsent(ns, k -> new ArrayList<>()).add(id);
        }

        for (var nsEntry : idsByNamespace.entrySet()) {
            String ns = nsEntry.getKey();
            var availNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var levelNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var rarityNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var tableNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var villagerNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var itemNs = entries.startSubCategory(text(ns)).setExpanded(false);
            var incompatNs = entries.startSubCategory(text(ns)).setExpanded(false);

            for (String id : nsEntry.getValue()) {
                var detail = details.get(id);
                boolean tradeDefault = client.level == null || client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .get(Identifier.parse(id)).map(goldenrose01.enchlib.trading.VillagerEnchantmentRules::defaultEnabled).orElse(true);
                var tradeGroup = entries.startSubCategory(text(id)).setExpanded(false);
                for (boolean books : new boolean[]{true, false}) {
                    Boolean override = detail == null ? null : books ? detail.getVillager_books() : detail.getVillager_items();
                    boolean current = override == null ? tradeDefault : override;
                    tradeGroup.add(entries.startBooleanToggle(text(books ? "Enchanted books" : "Enchanted items"), current)
                            .setDefaultValue(tradeDefault)
                            .setSaveConsumer(changed(current, value -> EnchantLibAPI.setVillagerEnabled(id, books,
                                    value == tradeDefault ? null : value))).build());
                }
                villagerNs.add(tradeGroup.build());
                availNs.add(entries.startBooleanToggle(text(id), enabled.getOrDefault(id, true))
                        .setDefaultValue(true).setSaveConsumer(changed(enabled.getOrDefault(id, true), v -> EnchantLibAPI.setEnabled(id, v))).build());
                int presetLevel = Optional.ofNullable(EnchantLibAPI.getPresetMaxLevel(id))
                        .orElse(parseInt(presets.getProperty("level:" + id), 1));
                int level = detail == null ? presetLevel : detail.getMax_level();
                levelNs.add(entries.startIntField(text(id), level).setMin(1).setMax(255)
                        .setDefaultValue(presetLevel).setSaveConsumer(v -> EnchantLibAPI.setMaxLevel(id, v)).build());

                String presetRarity = Optional.ofNullable(EnchantLibAPI.getPresetRarity(id)).orElse(presets.getProperty("rarity:" + id, "common"));
                String currentRarity = detail == null || detail.getRarity() == null ? presetRarity : detail.getRarity();
                Set<String> choices = new LinkedHashSet<>(Arrays.asList(RARITIES));
                choices.add(currentRarity);
                choices.add(presetRarity);
                rarityNs.add(entries.startSelector(text(id), choices.toArray(String[]::new), currentRarity)
                        .setDefaultValue(presetRarity).setTooltip(text("Table weights: common 100, uncommon 60, rare 30, very rare 5."))
                        .setSaveConsumer(changed(currentRarity, v -> EnchantLibAPI.setRarity(id, v))).build());

                var tableGroup = entries.startSubCategory(text(id)).setExpanded(false);
                EnchantLibAPI.getEnchantingTables().forEach((tableId, label) -> {
                    var rule = detail == null ? null : detail.getTable_rules().get(tableId);
                    boolean tableEnabled = rule == null || rule.getEnabled();
                    int chance = rule == null ? 100 : rule.getChance();
                    tableGroup.add(entries.startBooleanToggle(text(label + " — enabled"), tableEnabled).setDefaultValue(true)
                            .setSaveConsumer(changed(tableEnabled, v -> EnchantLibAPI.setTableEnabled(id, tableId, v))).build());
                    tableGroup.add(entries.startIntSlider(text(label + " — chance %"), chance, 0, 100).setDefaultValue(100)
                            .setSaveConsumer(changed(chance, v -> EnchantLibAPI.setTableChance(id, tableId, v))).build());
                });
                tableNs.add(tableGroup.build());

                List<String> presetItems = EnchantLibAPI.getPresetItemCategories(id);
                if (presetItems.isEmpty()) presetItems = csv(presets.getProperty("compat:" + id, ""));
                List<String> currentItems = detail == null ? presetItems : new ArrayList<>(detail.getEnc_category());
                itemNs.add(entries.startStrList(text(id), new ArrayList<>(currentItems)).setExpanded(false)
                        .setDefaultValue(new ArrayList<>(presetItems))
                        .setTooltip(text("One rule per row: swords, armor, tools, !axes, or item:minecraft:diamond_sword."))
                        .setCellErrorSupplier(value -> value.isBlank() ? Optional.of(text("Enter an item category or item ID.")) : Optional.empty())
                        .setSaveConsumer(changed(currentItems, v -> EnchantLibAPI.setItemCategories(id, new ArrayList<>(v)))).build());

                List<String> currentIncompatible = detail == null ? csv(presets.getProperty("incompat:" + id, "")) : new ArrayList<>(detail.getIncompatible_enchantments());
                incompatNs.add(entries.startStrList(text(id), new ArrayList<>(currentIncompatible)).setExpanded(false)
                        .setDefaultValue(new ArrayList<>(csv(presets.getProperty("incompat:" + id, ""))))
                        .setTooltip(text("One enchantment ID per row, for example minecraft:sharpness."))
                        .setCellErrorSupplier(value -> value.equals(id) ? Optional.of(text("An enchantment cannot be incompatible with itself."))
                                : !ids.contains(value) ? Optional.of(text("Unknown enchantment ID.")) : Optional.empty())
                        .setSaveConsumer(changed(currentIncompatible, v -> EnchantLibAPI.setIncompatibleEnchantments(id, new ArrayList<>(v)))).build());
            }

            availability.addEntry(availNs.build());
            levels.addEntry(levelNs.build());
            rarity.addEntry(rarityNs.build());
            tables.addEntry(tableNs.build());
            villagers.addEntry(villagerNs.build());
            items.addEntry(itemNs.build());
            incompatibility.addEntry(incompatNs.build());
        }

        List<Runnable> mobCommits = new ArrayList<>();
        Set<String> categories = new TreeSet<>(EnchantLibAPI.getMobCategories());
        categories.addAll(List.of("animals", "arthropods", "cubic", "flying", "fungi", "hell", "magik", "rebel", "undead", "void", "water"));
        Properties mobPresets = new Properties();
        loadPreset(mobPresets, "mob", "MobCategory.defaults.config");
        Map<String, List<String>> mobsByNamespace = new TreeMap<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString).sorted().forEach(id -> {
            String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
            mobsByNamespace.computeIfAbsent(ns, k -> new ArrayList<>()).add(id);
        });
        for (var nsEntry : mobsByNamespace.entrySet()) {
            var mobNs = entries.startSubCategory(text(nsEntry.getKey())).setExpanded(false);
            for (String id : nsEntry.getValue()) {
                Set<String> initial = new TreeSet<>(MobCategoryManager.INSTANCE.getCategories(id));
                Set<String> selected = new TreeSet<>(initial);
                Set<String> defaults = new TreeSet<>(csv(mobPresets.getProperty("mob:" + id, "")));
                Set<String> options = new TreeSet<>(categories);
                options.addAll(initial);
                options.addAll(defaults);
                var group = entries.startSubCategory(text(id)).setExpanded(false);
                options.forEach(category -> group.add(entries.startBooleanToggle(text(category), initial.contains(category))
                        .setDefaultValue(defaults.contains(category))
                        .setSaveConsumer(v -> { if (v) selected.add(category); else selected.remove(category); }).build()));
                mobNs.add(group.build());
                // ConfigBuilder invokes this after the individual entry consumers.
                mobCommits.add(() -> { if (!initial.equals(selected)) applyMob(client, id, selected); });
            }
            mobs.addEntry(mobNs.build());
        }
        builder.setSavingRunnable(() -> mobCommits.forEach(Runnable::run));
        return builder.build();
    }

    private static Component text(String value) { return Component.literal(value); }

    private static <T> Consumer<T> changed(T initial, Consumer<T> save) {
        return value -> { if (!Objects.equals(initial, value)) save.accept(value); };
    }
    private static void applyMob(Minecraft client, String id, Collection<String> cats) {
        if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(() -> MobCategoryManager.INSTANCE.setCategories(client.getSingleplayerServer(), id, new ArrayList<>(cats)));
        else if (client.player != null) client.player.connection.sendCommand("plusec-debug mob set " + id + " " + (cats.isEmpty() ? "none" : String.join(",", cats)));
    }

    private static List<String> csv(String value) { return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }
    private static int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; } }
    private static Properties loadPresets() {
        Properties result = new Properties();
        loadPreset(result, "level", "EnchLVLmax.config");
        loadPreset(result, "rarity", "EnchRarity.config");
        loadPreset(result, "compat", "EnchCompatibility.config");
        loadPreset(result, "incompat", "EnchUNcompatibility.config");
        return result;
    }
    private static void loadPreset(Properties target, String prefix, String fileName) {
        try {
            for (String line : GlobalConfigIO.INSTANCE.readPresetFile(fileName).split("\\R")) {
                String trimmed = line.trim(); int separator = trimmed.indexOf('=');
                if (separator <= 0 || trimmed.startsWith("#")) continue;
                target.setProperty(prefix + ":" + trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        } catch (Exception ignored) { }
    }
}
