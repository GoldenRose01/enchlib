package goldenrose01.enchlib.modmenu;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.PlaceholderCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import goldenrose01.enchlib.api.EnchantLibAPI;
import goldenrose01.enchlib.config.GlobalConfigIO;
import goldenrose01.enchlib.config.MobCategoryManager;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** In-game editor for enchantment and mob metadata. */
public final class MobCategoryScreen {
    private static final String[] RARITIES = {"common", "uncommon", "rare", "very_rare"};
    private static final String[] ITEM_RULES = {"all", "books", "tools", "pickaxes", "axes", "shovels", "hoes", "swords", "weapons", "armor", "head_armor", "chest_armor", "leg_armor", "foot_armor", "bows", "crossbows", "tridents", "maces", "fishing_rods", "wooded", "wooden", "stone", "iron", "gold", "diamond", "netherite", "turtle_shell"};

    private MobCategoryScreen() { }

    public static Screen create(Screen parent) {
        var root = YetAnotherConfigLib.createBuilder().title(Component.literal("EnchLib"));
        root.category(PlaceholderCategory.createBuilder().name(Component.literal("Available Enchantments"))
                .screen((client, screen) -> availableScreen(screen)).build());
        root.category(PlaceholderCategory.createBuilder().name(Component.literal("Enchant Detail"))
                .screen((client, screen) -> detailMenu(screen)).build());
        root.category(PlaceholderCategory.createBuilder().name(Component.literal("Mob"))
                .screen((client, screen) -> mobScreen(screen)).build());
        return root.build().generateScreen(parent);
    }

    private static Screen detailMenu(Screen parent) {
        var root = YetAnotherConfigLib.createBuilder().title(Component.literal("Enchant Detail"));
        root.category(placeholder("Uncompatibility", parent, Kind.INCOMPATIBILITY));
        root.category(placeholder("Rarity", parent, Kind.RARITY));
        root.category(placeholder("Max Level", parent, Kind.MAX_LEVEL));
        root.category(placeholder("Categories", parent, Kind.CATEGORIES));
        return root.build().generateScreen(parent);
    }

    private enum Kind { INCOMPATIBILITY, RARITY, MAX_LEVEL, CATEGORIES }
    private static PlaceholderCategory placeholder(String title, Screen parent, Kind kind) {
        return PlaceholderCategory.createBuilder().name(Component.literal(title))
                .screen((client, screen) -> enchantScreen(screen, kind)).build();
    }

    private static Screen availableScreen(Screen parent) {
        var available = GlobalConfigIO.INSTANCE.readAvailable();
        var known = enchantmentIds();
        Map<String, GlobalConfigIO.AvailableEnch> configured = available.stream().collect(Collectors.toMap(
                GlobalConfigIO.AvailableEnch::getId, e -> e, (a, b) -> b, TreeMap::new));
        for (String id : known) configured.putIfAbsent(id, new GlobalConfigIO.AvailableEnch(id, true));
        ConfigCategory.Builder category = ConfigCategory.createBuilder().name(Component.literal("Available Enchantments"));
        Map<String, Boolean> state = new TreeMap<>();
        category.option(ButtonOption.createBuilder().name(Component.literal("Reset all to presets"))
                .text(Component.literal("🗑 Reset all"))
                .action(screen -> {
                    state.replaceAll((id, enabled) -> true);
                    GlobalConfigIO.INSTANCE.writeAvailable(state.entrySet().stream().map(e -> new GlobalConfigIO.AvailableEnch(e.getKey(), true)).toList());
                    screen.onClose();
                }).build());
        configured.forEach((id, entry) -> {
            state.put(id, entry.getEnabled());
            var group = OptionGroup.createBuilder().name(Component.literal(id)).collapsed(true);
            group.option(boolOption("Available", "Enable or disable this enchantment.", entry.getEnabled(), value -> state.put(id, value)));
            group.option(ButtonOption.createBuilder().name(Component.literal("Reset to preset"))
                    .text(Component.literal("🗑")).action(screen -> { state.put(id, true); EnchantLibAPI.setEnabled(id, true); screen.onClose(); }).build());
            category.group(group.build());
        });
        return YetAnotherConfigLib.createBuilder().title(Component.literal("Available Enchantments")).category(category.build())
                .save(() -> {
                    List<GlobalConfigIO.AvailableEnch> result = state.entrySet().stream()
                            .map(e -> new GlobalConfigIO.AvailableEnch(e.getKey(), e.getValue())).toList();
                    GlobalConfigIO.INSTANCE.writeAvailable(result);
                }).build().generateScreen(parent);
    }

    private static Screen enchantScreen(Screen parent, Kind kind) {
        List<String> ids = enchantmentIds();
        var details = GlobalConfigIO.INSTANCE.readDetails().getEnchantments().stream()
                .collect(Collectors.toMap(GlobalConfigIO.EnchantmentDetails::getId, e -> e, (a, b) -> b));
        var available = GlobalConfigIO.INSTANCE.readAvailable().stream().collect(Collectors.toMap(
                GlobalConfigIO.AvailableEnch::getId, GlobalConfigIO.AvailableEnch::getEnabled, (a, b) -> b));
        Properties presets = loadPresets();
        ConfigCategory.Builder category = ConfigCategory.createBuilder().name(Component.literal(title(kind)));
        List<Runnable> commits = new ArrayList<>();
        category.option(ButtonOption.createBuilder().name(Component.literal("Reset this menu to presets"))
                .text(Component.literal("🗑 Reset all"))
                .action(screen -> { resetSection(kind, ids, presets); screen.onClose(); }).build());
        for (String id : ids) {
            GlobalConfigIO.EnchantmentDetails detail = details.getOrDefault(id, new GlobalConfigIO.EnchantmentDetails(id, 1, new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), new HashMap<>(), null, null, null));
            OptionGroupBuilder group = new OptionGroupBuilder(category, display(id), commits);
            switch (kind) {
                case MAX_LEVEL -> {
                    int fallback = parseInt(presets.getProperty("level:" + id), detail.getMax_level());
                    fallback = Optional.ofNullable(EnchantLibAPI.getPresetMaxLevel(id)).orElse(fallback);
                    final int presetLevel = fallback;
                    int[] value = {detail.getMax_level() > 1 || details.containsKey(id) ? detail.getMax_level() : fallback};
                    boolean[] dirty = {false};
                    group.add(intOption("Maximum level", value[0], 1, 255, v -> { value[0] = v; dirty[0] = true; }));
                    group.reset(() -> EnchantLibAPI.setMaxLevel(id, presetLevel));
                    group.commit(() -> { if (dirty[0]) EnchantLibAPI.setMaxLevel(id, value[0]); });
                }
                case RARITY -> {
                    String fallback = Optional.ofNullable(EnchantLibAPI.getPresetRarity(id)).orElse(presets.getProperty("rarity:" + id, "common"));
                    String[] rarity = {detail.getRarity() == null ? fallback : detail.getRarity()};
                    boolean[] rarityDirty = {false};
                    group.add(Option.<String>createBuilder().name(Component.literal("Rarity class"))
                            .description(OptionDescription.of(Component.literal("Relative table weight: Common 100, Uncommon 60, Rare 30, Very Rare 5. The selected table percentage is multiplied by this weight.")))
                            .binding(rarity[0], () -> rarity[0], v -> { rarity[0] = v; rarityDirty[0] = true; })
                            .controller(option -> DropdownStringControllerBuilder.create(option).values(RARITIES)).build());
                    for (Map.Entry<String, String> table : EnchantLibAPI.getEnchantingTables().entrySet()) {
                        String tableId = table.getKey();
                        GlobalConfigIO.EnchantingTableRule rule = detail.getTable_rules().getOrDefault(tableId, new GlobalConfigIO.EnchantingTableRule(true, 100));
                        boolean[] enabled = {rule.getEnabled()}, tableDirty = {false}; int[] chance = {rule.getChance()}; boolean[] chanceDirty = {false};
                        group.add(boolOption(table.getValue() + " — enabled", "Include this enchantment in " + table.getValue() + ".", enabled[0], v -> { enabled[0] = v; tableDirty[0] = true; }));
                        group.add(intOption(table.getValue() + " — chance %", chance[0], 0, 100, v -> { chance[0] = v; chanceDirty[0] = true; }));
                        group.commit(() -> { if (tableDirty[0]) EnchantLibAPI.setTableEnabled(id, tableId, enabled[0]); if (chanceDirty[0]) EnchantLibAPI.setTableChance(id, tableId, chance[0]); });
                    }
                    group.reset(() -> {
                        EnchantLibAPI.setRarity(id, fallback);
                        EnchantLibAPI.getEnchantingTables().keySet().forEach(tableId -> EnchantLibAPI.resetTableRule(id, tableId));
                    });
                    group.commit(() -> { if (rarityDirty[0]) EnchantLibAPI.setRarity(id, rarity[0]); });
                }
                case CATEGORIES -> {
                    Set<String> selected = new TreeSet<>(detail.getEnc_category());
                    List<String> registeredPreset = EnchantLibAPI.getPresetItemCategories(id);
                    List<String> presetRules = !registeredPreset.isEmpty() ? registeredPreset : csv(presets.getProperty("compat:" + id, String.join(",", selected)));
                    if (!details.containsKey(id)) selected.addAll(presetRules);
                    Set<String> rules = selected;
                    String[] individualItems = {selected.stream().filter(v -> v.startsWith("item:") || v.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$") && !Arrays.asList(ITEM_RULES).contains(v)).map(v -> v.startsWith("item:") ? v.substring(5) : v).collect(Collectors.joining(", "))};
                    selected.removeIf(v -> v.startsWith("item:") || v.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$") && !Arrays.asList(ITEM_RULES).contains(v));
                    boolean[] categoryDirty = {false};
                    for (String token : itemRulesFor(id, presetRules, selected)) {
                        if (token.startsWith("item:") || token.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$") && !Arrays.asList(ITEM_RULES).contains(token)) continue;
                        boolean isNegative = token.startsWith("!"); String raw = isNegative ? token.substring(1) : token;
                        boolean checked = isNegative ? selected.contains(token) : selected.contains(token);
                        group.add(boolOption(raw + (isNegative ? " (exclude)" : ""), "Include this rule; positive rules are combined and exclusions are applied after them.", checked, value -> {
                            categoryDirty[0] = true; if (value) rules.add(token); else rules.remove(token);
                        }));
                    }
                    group.add(Option.<String>createBuilder().name(Component.literal("Specific item IDs"))
                            .description(OptionDescription.of(Component.literal("Comma-separated IDs, for example minecraft:netherite_sword or othermod:crystal_blade.")))
                            .binding(individualItems[0], () -> individualItems[0], value -> { individualItems[0] = value.trim(); categoryDirty[0] = true; })
                            .controller(StringControllerBuilder::create).build());
                    group.reset(() -> EnchantLibAPI.setItemCategories(id, presetRules));
                    group.commit(() -> { if (!categoryDirty[0]) return;
                        List<String> saved = new ArrayList<>(rules);
                        for (String item : csv(individualItems[0])) {
                            String normalized = item.contains(":") ? item : "minecraft:" + item;
                            saved.add("item:" + normalized);
                        }
                        EnchantLibAPI.setItemCategories(id, saved);
                    });
                    break;
                }
                case INCOMPATIBILITY -> {
                    Set<String> eligible = new TreeSet<>(ids); eligible.remove(id);
                    Set<String> thisItems = compatibleItems(id, detail.getEnc_category(), presets);
                    List<String> chosen = new ArrayList<>();
                    for (String other : eligible) {
                        Set<String> otherItems = compatibleItems(other, details.containsKey(other) ? details.get(other).getEnc_category() : List.of(), presets);
                        if (!Collections.disjoint(thisItems, otherItems)) chosen.add(other);
                    }
                    chosen.retainAll(detail.getIncompatible_enchantments());
                    boolean[] dirty = {false};
                    group.add(ListOption.<String>createBuilder().name(Component.literal("Incompatible enchantments"))
                            .description(OptionDescription.of(Component.literal("Choose one or more enchantments that share compatible items with this one.")))
                            .binding(chosen, () -> chosen, values -> { chosen.clear(); chosen.addAll(values); dirty[0] = true; })
                            .controller(option -> DropdownStringControllerBuilder.create(option).values(eligibleFor(thisItems, ids, details, presets).toArray(String[]::new)).allowEmptyValue(false).allowAnyValue(false))
                            .minimumNumberOfEntries(0).build());
                    group.reset(() -> EnchantLibAPI.setIncompatibleEnchantments(id, csv(presets.getProperty("incompat:" + id, ""))));
                    group.commit(() -> { if (dirty[0]) EnchantLibAPI.setIncompatibleEnchantments(id, chosen); });
                }
            }
            group.install();
        }
        return YetAnotherConfigLib.createBuilder().title(Component.literal(title(kind))).category(category.build())
                .save(() -> commits.forEach(Runnable::run)).build().generateScreen(parent);
    }

    /** YACL groups do not expose a mutation callback after their child options are built, so each row saves on screen close. */
    private static final class OptionGroupBuilder {
        private final ConfigCategory.Builder category; private final String title; private final List<Runnable> allCommits;
        private final List<Option<?>> options = new ArrayList<>(); private final List<Runnable> commits = new ArrayList<>(); private Runnable reset;
        OptionGroupBuilder(ConfigCategory.Builder category, String title, List<Runnable> allCommits) { this.category = category; this.title = title; this.allCommits = allCommits; }
        void add(Option<?> option) { options.add(option); }
        void commit(Runnable task) { commits.add(task); }
        void reset(Runnable task) { reset = task; }
        void install() {
            var group = dev.isxander.yacl3.api.OptionGroup.createBuilder().name(Component.literal(title)).collapsed(true);
            options.forEach(group::option);
            if (reset != null) group.option(ButtonOption.createBuilder().name(Component.literal("Reset to preset"))
                    .text(Component.literal("🗑")).action(screen -> { reset.run(); screen.onClose(); }).build());
            category.group(group.build());
            // Saving is attached to each option so edits persist immediately, including when navigating to another page.
            // The callbacks below are wired by the option bindings through the on-save hook in enchantScreen.
            allCommits.addAll(commits);
        }
    }

    private static Screen mobScreen(Screen parent) {
        var mobs = new ArrayList<String>(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString).toList());
        mobs.sort(String::compareTo);
        var root = YetAnotherConfigLib.createBuilder().title(Component.literal("Mob categories"));
        ConfigCategory.Builder reset = ConfigCategory.createBuilder().name(Component.literal("Mob category actions"));
        reset.option(ButtonOption.createBuilder().name(Component.literal("Reset all mob categories to presets"))
                .text(Component.literal("🗑 Reset all"))
                .action(screen -> { mobs.forEach(id -> applyMob(Minecraft.getInstance(), id, defaultMobCategories(id))); screen.onClose(); }).build());
        root.category(reset.build());
        for (String mob : mobs) root.category(PlaceholderCategory.createBuilder().name(Component.literal(mob))
                .screen((client, screen) -> mobDetailScreen(screen, mob)).build());
        return root.build().generateScreen(parent);
    }

    private static Screen mobDetailScreen(Screen parent, String mob) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder().name(Component.literal("Categories"));
        Minecraft client = Minecraft.getInstance();
        List<String> categories = new ArrayList<>(EnchantLibAPI.getMobCategories());
        for (String extra : List.of("animals", "arthropods", "cubic", "flying", "fungi", "hell", "magik", "rebel", "undead", "void", "water")) if (!categories.contains(extra)) categories.add(extra);
        categories.sort(String::compareTo);
        Set<String> initial = new TreeSet<>(MobCategoryManager.INSTANCE.getCategories(mob));
        for (String cat : categories) category.option(boolOption(cat, "Assign the " + cat + " category to this mob.", initial.contains(cat), v -> { if (v) initial.add(cat); else initial.remove(cat); }));
        category.option(ButtonOption.createBuilder().name(Component.literal("Reset categories to preset")).text(Component.literal("🗑"))
                .action(screen -> { applyMob(client, mob, defaultMobCategories(mob)); screen.onClose(); }).build());
        return YetAnotherConfigLib.createBuilder().title(Component.literal("Mob categories")).category(category.build())
                .save(() -> applyMob(client, mob, initial)).build().generateScreen(parent);
    }

    private static void resetSection(Kind kind, List<String> ids, Properties presets) {
        switch (kind) {
            case MAX_LEVEL -> ids.forEach(id -> EnchantLibAPI.setMaxLevel(id, parseInt(presets.getProperty("level:" + id), 1)));
            case RARITY -> ids.forEach(id -> {
                EnchantLibAPI.setRarity(id, Optional.ofNullable(EnchantLibAPI.getPresetRarity(id)).orElse(presets.getProperty("rarity:" + id, "common")));
                EnchantLibAPI.getEnchantingTables().keySet().forEach(table -> EnchantLibAPI.resetTableRule(id, table));
            });
            case CATEGORIES -> ids.forEach(id -> EnchantLibAPI.setItemCategories(id, csv(presets.getProperty("compat:" + id, ""))));
            case INCOMPATIBILITY -> ids.forEach(id -> EnchantLibAPI.setIncompatibleEnchantments(id, csv(presets.getProperty("incompat:" + id, ""))));
        }
    }

    private static List<String> defaultMobCategories(String id) {
        Properties defaults = new Properties();
        try {
            for (String line : GlobalConfigIO.INSTANCE.readPresetFile("MobCategory.defaults.config").split("\\R")) {
                String trimmed = line.trim(); int sep = trimmed.indexOf('=');
                if (sep > 0 && !trimmed.startsWith("#")) defaults.setProperty(trimmed.substring(0, sep).trim(), trimmed.substring(sep + 1).trim());
            }
        } catch (Exception ignored) { return List.of(); }
        return csv(defaults.getProperty(id, ""));
    }

    private static void applyMob(Minecraft client, String id, Collection<String> cats) {
        if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(() -> MobCategoryManager.INSTANCE.setCategories(client.getSingleplayerServer(), id, new ArrayList<>(cats)));
        else if (client.player != null) client.player.connection.sendCommand("plusec-debug mob set " + id + " " + (cats.isEmpty() ? "none" : String.join(",", cats)));
    }

    private static Option<Boolean> boolOption(String name, String tooltip, boolean initial, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder().name(Component.literal(name)).description(OptionDescription.of(Component.literal(tooltip)))
                .binding(initial, () -> initial, setter).controller(BooleanControllerBuilder::create).build();
    }
    private static Option<Integer> intOption(String name, int initial, int min, int max, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder().name(Component.literal(name)).binding(initial, () -> initial, value -> setter.accept(Math.max(min, Math.min(max, value))))
                .controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max)).build();
    }
    private static Option<String> stringOption(String name, String initial, List<String> values, Consumer<String> setter) {
        return Option.<String>createBuilder().name(Component.literal(name)).binding(initial, () -> initial, setter)
                .controller(option -> DropdownStringControllerBuilder.create(option).values(values)).build();
    }
    private static List<String> enchantmentIds() {
        TreeSet<String> ids = new TreeSet<>();
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) ids.addAll(client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).keySet().stream().map(Identifier::toString).toList());
        GlobalConfigIO.INSTANCE.readAvailable().forEach(e -> ids.add(e.getId()));
        GlobalConfigIO.INSTANCE.readDetails().getEnchantments().forEach(e -> ids.add(e.getId()));
        loadPresets().stringPropertyNames().forEach(key -> ids.add(key.substring(key.indexOf(':') + 1)));
        return new ArrayList<>(ids);
    }
    private static String title(Kind kind) { return switch (kind) { case INCOMPATIBILITY -> "Uncompatibility"; case RARITY -> "Rarity"; case MAX_LEVEL -> "Max Level"; case CATEGORIES -> "Categories"; }; }
    private static String display(String id) { return id; }
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
    private static List<String> itemRulesFor(String id, List<String> defaults, Set<String> selected) {
        LinkedHashSet<String> rules = new LinkedHashSet<>(Arrays.asList(ITEM_RULES));
        defaults.stream().filter(v -> !v.startsWith("item:")).forEach(rules::add);
        selected.stream().filter(v -> !v.startsWith("item:")).forEach(rules::add); rules.add("!all");
        return rules.stream().sorted().toList();
    }
    private static Set<String> compatibleItems(String id, List<String> current, Properties presets) {
        List<String> rules = current.isEmpty() ? csv(presets.getProperty("compat:" + id, "all")) : current;
        Set<String> expanded = new HashSet<>();
        for (String rule : rules) {
            String token = rule.startsWith("!") ? rule.substring(1) : rule;
            if (token.equals("all") || token.equals("all_items")) expanded.addAll(List.of("weapons", "armor", "tools", "books"));
            expanded.add(token);
            String itemPath = token.startsWith("item:") ? token.substring(5).substring(token.substring(5).indexOf(':') + 1) : token.contains(":") ? token.substring(token.indexOf(':') + 1) : token;
            if (itemPath.endsWith("_sword") || itemPath.endsWith("_bow") || itemPath.endsWith("_crossbow") || itemPath.endsWith("_trident") || itemPath.endsWith("_mace")) expanded.add("weapons");
            if (itemPath.endsWith("_axe") || itemPath.endsWith("_pickaxe") || itemPath.endsWith("_shovel") || itemPath.endsWith("_hoe")) expanded.add("tools");
            if (itemPath.endsWith("_helmet") || itemPath.endsWith("_chestplate") || itemPath.endsWith("_leggings") || itemPath.endsWith("_boots") || itemPath.equals("turtle_helmet")) expanded.add("armor");
            if (token.equals("sword") || token.equals("swords")) expanded.add("weapons");
            if (token.equals("axe") || token.equals("axes")) expanded.add("tools");
            if (token.equals("armor") || token.endsWith("_armor")) expanded.add("armor");
            if (token.equals("tools") || token.equals("pickaxes") || token.equals("shovels") || token.equals("hoes")) expanded.add("tools");
        }
        return expanded;
    }

    private static List<String> eligibleFor(Set<String> itemTypes, List<String> ids,
                                             Map<String, GlobalConfigIO.EnchantmentDetails> details, Properties presets) {
        return ids.stream().filter(other -> !Collections.disjoint(itemTypes,
                        compatibleItems(other, details.containsKey(other) ? details.get(other).getEnc_category() : List.of(), presets)))
                .sorted().toList();
    }
}
