package goldenrose01.enchlib.config

import goldenrose01.enchlib.config.data.*
import kotlinx.serialization.json.Json

object DefaultDataLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun getEnchantmentData(enchantmentId: String): DetailedEnchantmentEntry? {
        return when (enchantmentId) {
            "minecraft:sharpness" -> DetailedEnchantmentEntry(
                id = "minecraft:sharpness",
                name = "Sharpness",
                max_level = 5,
                applicable_to = listOf("sword", "axe"),
                description = "Increases melee damage dealt to all mobs.",
                enc_category = listOf("Damage", "Combat"),
                mob_category = listOf("all"),
                rarity = "common",
                levels = listOf(
                    EnchantmentLevelData(level = 1, extra_damage = 0.5),
                    EnchantmentLevelData(level = 2, extra_damage = 1.0),
                    EnchantmentLevelData(level = 3, extra_damage = 1.5),
                    EnchantmentLevelData(level = 4, extra_damage = 2.0),
                    EnchantmentLevelData(level = 5, extra_damage = 2.5)
                )
            )
            "minecraft:efficiency" -> DetailedEnchantmentEntry(
                id = "minecraft:efficiency",
                name = "Efficiency",
                max_level = 5,
                applicable_to = listOf("pickaxe", "axe", "shovel", "hoe", "shears"),
                description = "Increases mining speed.",
                enc_category = listOf("mining"),
                mob_category = listOf("none"),
                rarity = "common",
                levels = listOf(
                    EnchantmentLevelData(level = 1, speed_multiplier = 1.0),
                    EnchantmentLevelData(level = 2, speed_multiplier = 1.3),
                    EnchantmentLevelData(level = 3, speed_multiplier = 1.6),
                    EnchantmentLevelData(level = 4, speed_multiplier = 1.9),
                    EnchantmentLevelData(level = 5, speed_multiplier = 2.2)
                )
            )
            "minecraft:smite" -> DetailedEnchantmentEntry(
                id = "minecraft:smite",
                name = "Smite",
                max_level = 5,
                applicable_to = listOf("sword", "axe"),
                description = "Increases melee damage dealt to undead mobs.",
                enc_category = listOf("Damage", "Combat"),
                mob_category = listOf("undead"),
                rarity = "common",
                levels = listOf(
                    EnchantmentLevelData(level = 1, extra_damage_vs_category = 2.5),
                    EnchantmentLevelData(level = 2, extra_damage_vs_category = 5.0),
                    EnchantmentLevelData(level = 3, extra_damage_vs_category = 7.5),
                    EnchantmentLevelData(level = 4, extra_damage_vs_category = 10.0),
                    EnchantmentLevelData(level = 5, extra_damage_vs_category = 12.5)
                )
            )
            else -> null
        }
    }

    fun getDefaultMobCategories(): MobCategoriesConfig {
        return MobCategoriesConfig(
            mobs = mutableListOf(
                MobEntry("Allay", "Passive", listOf("magik", "flying")),
                MobEntry("Armadillo", "Passive", listOf("animals", "cubic")),
                MobEntry("Axolotl", "Pet", listOf("animals", "water", "arthropods")),
                MobEntry("Bat", "Passive", listOf("animals", "flying")),
                MobEntry("Bee", "Neutral", listOf("flying")),
                MobEntry("Blaze", "Hostile", listOf("magik", "hell", "flying")),
                MobEntry("Bogged", "Hostile", listOf("undead", "fungi")),
                MobEntry("Breeze", "Hostile", listOf("magik", "flying")),
                MobEntry("Camel", "Passive", listOf("animals")),
                MobEntry("Cat", "Pet", listOf("animals")),
                MobEntry("Cave spider", "H-Neutral", listOf("arthropods")),
                MobEntry("Chicken", "Passive", listOf("animals")),
                MobEntry("Cod", "Passive", listOf("water")),
                MobEntry("Copper Golem", "Golem", listOf("magik", "cubic")),
                MobEntry("Cow", "Passive", listOf("animals")),
                MobEntry("Creeper", "Hostile", listOf("fungi", "arthropods")),
                MobEntry("Dog", "Pet", listOf("animals")),
                MobEntry("Dolphin", "Neutral", listOf("animals", "water")),
                MobEntry("Donkey", "Passive", listOf("animals")),
                MobEntry("Drowned", "H-Neutral", listOf("undead", "water")),
                MobEntry("Elder Guardian", "Hostile", listOf("magik", "water")),
                MobEntry("Ender Dragon", "Hostile", listOf("magik", "void", "arthropods", "flying")),
                MobEntry("Enderman", "Neutral", listOf("void")),
                MobEntry("Endermite", "Hostile", listOf("void")),
                MobEntry("Evoker", "Hostile", listOf("magik", "rebel")),
                MobEntry("Fox", "Passive", listOf("animals")),
                MobEntry("Frog", "Passive", listOf("animals")),
                MobEntry("Ghastling", "Pet", listOf("magik", "hell", "arthropods", "cubic", "flying")),
                MobEntry("Ghast", "Hostile", listOf("undead", "hell", "arthropods", "cubic", "flying")),
                MobEntry("Glow Squid", "Passive", listOf("animals", "magik", "water", "arthropods")),
                MobEntry("Goat", "R-Neutral", listOf("animals")),
                MobEntry("Guardian", "Hostile", listOf("water")),
                MobEntry("Hoglin", "Hostile", listOf("animals")),
                MobEntry("Horse", "Passive", listOf("animals")),
                MobEntry("Husk", "Hostile", listOf("undead")),
                MobEntry("Illusioner", "Hostile", listOf("magik", "rebel", "arthropods")),
                MobEntry("Iron Golem", "Golem", listOf("cubic")),
                MobEntry("Llama", "Neutral", listOf("animals")),
                MobEntry("Magma Cube", "Hostile", listOf("hell", "cubic")),
                MobEntry("Mooshroom", "Passive", listOf("animals")),
                MobEntry("Mule", "Passive", listOf("animals")),
                MobEntry("Ocelot", "Passive", listOf("animals")),
                MobEntry("Panda", "Neutral", listOf("animals")),
                MobEntry("Parrot", "Passive", listOf("animals", "flying")),
                MobEntry("Phantom", "Hostile", listOf("undead", "flying")),
                MobEntry("Pig", "Passive", listOf("animals")),
                MobEntry("Piglin", "H-Neutral", listOf("hell")),
                MobEntry("Piglin Brute", "Hostile", listOf("rebel", "hell")),
                MobEntry("Pillager", "Hostile", listOf("rebel")),
                MobEntry("Polar Bear", "Neutral", listOf("animals", "water")),
                MobEntry("Pufferfish", "Passive", listOf("animals", "water")),
                MobEntry("Rabbit", "Passive", listOf("animals")),
                MobEntry("Ravanger", "Hostile", listOf("rebel")),
                MobEntry("Salmon", "Passive", listOf("animals", "water")),
                MobEntry("Sheep", "Passive", listOf("animals")),
                MobEntry("Shulker", "Hostile", listOf("magik", "cubic", "flying")),
                MobEntry("Silverfish", "Hostile", listOf("arthropods")),
                MobEntry("Skeleton", "Hostile", listOf("undead")),
                MobEntry("Skeleton Horse", "Passive", listOf("magik", "undead")),
                MobEntry("Slime", "Hostile", listOf("water", "cubic")),
                MobEntry("Sniffer", "Passive", listOf("animals", "arthropods", "cubic")),
                MobEntry("Snow Golem", "Golem", listOf("cubic")),
                MobEntry("Spider", "H-Neutral", listOf("arthropods")),
                MobEntry("Squid", "Passive", listOf("animals", "water", "arthropods")),
                MobEntry("Stray", "Hostile", listOf("undead")),
                MobEntry("Strider", "Passive", listOf("undead", "hell")),
                MobEntry("Tadpole", "Passive", listOf("animals", "water")),
                MobEntry("Turtle", "Passive", listOf("animals", "water", "flying")),
                MobEntry("Vex", "Hostile", listOf("magik", "undead", "rebel", "flying")),
                MobEntry("Villager", "NPC", listOf()),
                MobEntry("Vindicator", "Hostile", listOf("rebel")),
                MobEntry("Wandering Trader", "NPC", listOf("magik")),
                MobEntry("Warden", "Hostile", listOf("magik", "fungi")),
                MobEntry("Witch", "Hostile", listOf("magik")),
                MobEntry("Wither Boss", "Hostile", listOf("magik", "hell", "void")),
                MobEntry("Wither Skeleton", "Hostile", listOf("undead", "hell")),
                MobEntry("Wolf", "Neutral", listOf("animals")),
                MobEntry("Zoglin", "Hostile", listOf("undead", "hell")),
                MobEntry("Zombie", "Hostile", listOf("undead")),
                MobEntry("Zombie Horse", "Hostile", listOf("undead")),
                MobEntry("Zombified Piglin", "Neutral", listOf("undead", "hell")),
                MobEntry("Zombified villager", "Hostile", listOf("undead"))
            )
        )
    }

    fun getDefaultIncompatibilityRules(): IncompatibilityRules {
        return IncompatibilityRules(
            incompatible_pairs = mutableListOf(
                IncompatiblePair("minecraft:fortune", "minecraft:silk_touch"),
                IncompatiblePair("minecraft:depth_strider", "minecraft:frost_walker"),
                IncompatiblePair("minecraft:loyalty", "minecraft:riptide"),
                IncompatiblePair("minecraft:channeling", "minecraft:riptide"),
                IncompatiblePair("minecraft:multishot", "minecraft:piercing")
            ),
            category_limits = mutableMapOf(
                "Damage" to 1,
                "Protection" to 4,
                "Combat" to 3,
                "Mining" to 3
            )
        )
    }
}
