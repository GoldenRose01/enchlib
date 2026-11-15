package goldenrose01.enchlib.tree

import goldenrose01.enchlib.compat.MCCompat
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object TreeCutHandler {

    class TreeStructure(
        val origin: BlockPos,
        val logs: Set<BlockPos>,
        val leaves: Set<BlockPos>
    )

    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, _ ->
            // On client-side, always return true so the player's client finishes breaking
            // the block and transmits the stop-destroy packet to the server.
            if (level.isClientSide) {
                return@register true
            }

            // Quick check: must be sneaking, holding a proper axe, and breaking a tree log
            val tool = player.mainHandItem
            if (!player.isShiftKeyDown || !isTreeLog(state) || !isProperAxe(tool)) {
                return@register true
            }

            val serverLevel = level as? ServerLevel ?: return@register true
            val serverPlayer = player as? ServerPlayer ?: return@register true

            val isEldritchLoaded = FabricLoader.getInstance().isModLoaded("eldrich-surge") ||
                    FabricLoader.getInstance().isModLoaded("eldritch-surge")

            // Resolve active tree cut mode
            val mode: TreeCutMode = if (player.isCreative) {
                TreeCutMode.CHOP
            } else if (isEldritchLoaded) {
                if (hasLumberjackEnchantment(tool)) TreeCutMode.CHOP else TreeCutMode.FELL
            } else {
                val configured = TreeCutConfig.getMode()
                if (configured == TreeCutMode.OFF) return@register true
                configured
            }

            // Scan connected tree structure
            val tree = scanTree(serverLevel, pos) ?: return@register true

            when (mode) {
                TreeCutMode.FELL -> {
                    executeFell(serverLevel, serverPlayer, pos, tool, tree)
                    return@register false
                }
                TreeCutMode.CHOP -> {
                    executeChop(serverLevel, serverPlayer, pos, tool, tree)
                    return@register false
                }
                TreeCutMode.OFF -> return@register true
            }
        }
    }

    fun isTreeLog(state: BlockState): Boolean {
        return state.`is`(BlockTags.LOGS) ||
                state.`is`(Blocks.MANGROVE_ROOTS)
    }

    fun isTreeCanopy(state: BlockState): Boolean {
        return state.`is`(BlockTags.LEAVES) ||
                state.`is`(BlockTags.WART_BLOCKS) ||
                state.`is`(Blocks.NETHER_WART_BLOCK) ||
                state.`is`(Blocks.WARPED_WART_BLOCK) ||
                state.`is`(Blocks.SHROOMLIGHT) ||
                state.`is`(Blocks.MANGROVE_LEAVES) ||
                state.`is`(Blocks.AZALEA_LEAVES) ||
                state.`is`(Blocks.FLOWERING_AZALEA_LEAVES) ||
                state.`is`(Blocks.BROWN_MUSHROOM_BLOCK) ||
                state.`is`(Blocks.RED_MUSHROOM_BLOCK) ||
                state.`is`(Blocks.WEEPING_VINES) ||
                state.`is`(Blocks.WEEPING_VINES_PLANT) ||
                state.`is`(Blocks.TWISTING_VINES) ||
                state.`is`(Blocks.TWISTING_VINES_PLANT)
    }

    fun isProperAxe(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val isAxe = stack.`is`(ItemTags.AXES)
        if (!isAxe) return false
        if (stack.`is`(Items.WOODEN_AXE)) return false
        val path = BuiltInRegistries.ITEM.getKey(stack.item).path
        if (path.contains("wooden_axe") || path.contains("wood_axe")) return false
        return true
    }

    fun hasLumberjackEnchantment(stack: ItemStack): Boolean {
        if (MCCompat.readEnchantments(stack).any { (id, lvl) ->
                id.endsWith(":lumberjack") && lvl > 0
            }) return true

        val enchantments = stack.get(DataComponents.ENCHANTMENTS) ?: return false
        for (holder in enchantments.keySet()) {
            val key = holder.unwrapKey().orElse(null) ?: continue
            val id = key.identifier()
            if (id.path.equals("lumberjack", ignoreCase = true) && enchantments.getLevel(holder) > 0) {
                return true
            }
        }
        return false
    }

    fun scanTree(level: ServerLevel, origin: BlockPos): TreeStructure? {
        val originBlock = level.getBlockState(origin).block
        val visited = mutableSetOf<BlockPos>()
        val logs = mutableSetOf<BlockPos>()
        val canopy = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()

        queue.add(origin)
        visited.add(origin)
        logs.add(origin)

        val maxLogs = 512
        val maxCanopy = 1024

        while (queue.isNotEmpty() && logs.size < maxLogs) {
            val current = queue.removeFirst()
            for (dx in -2..2) {
                for (dy in -1..2) {
                    for (dz in -2..2) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val neighbor = current.offset(dx, dy, dz)
                        if (neighbor.y < origin.y - 1) continue
                        val isImmediate = kotlin.math.abs(dx) <= 1 && kotlin.math.abs(dy) <= 1 && kotlin.math.abs(dz) <= 1
                        if (!isImmediate && (neighbor.y <= origin.y + 2 || kotlin.math.abs(neighbor.x - origin.x) > 8 || kotlin.math.abs(neighbor.z - origin.z) > 8)) {
                            continue
                        }
                        if (visited.add(neighbor)) {
                            val nState = level.getBlockState(neighbor)
                            if (isTreeLog(nState) && (nState.block == originBlock || nState.`is`(Blocks.MANGROVE_ROOTS) || originBlock == Blocks.MANGROVE_ROOTS)) {
                                logs.add(neighbor)
                                queue.add(neighbor)
                            } else if (isImmediate && isTreeCanopy(nState) && canopy.size < maxCanopy) {
                                canopy.add(neighbor)
                                queueCanopy(level, neighbor, visited, canopy, maxCanopy)
                            }
                        }
                    }
                }
            }
        }

        // Tree safety check: must have attached leaves/canopy (Overworld leaves or Nether warts)
        // to protect player-built structures
        if (canopy.isEmpty()) {
            return null
        }

        return TreeStructure(origin, logs, canopy)
    }

    private fun queueCanopy(
        level: ServerLevel,
        start: BlockPos,
        visited: MutableSet<BlockPos>,
        canopy: MutableSet<BlockPos>,
        maxCanopy: Int
    ) {
        val queue = ArrayDeque<BlockPos>()
        queue.add(start)
        var count = 0
        while (queue.isNotEmpty() && canopy.size < maxCanopy && count < 256) {
            count++
            val curr = queue.removeFirst()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val next = curr.offset(dx, dy, dz)
                        if (kotlin.math.abs(next.x - start.x) > 5 || kotlin.math.abs(next.y - start.y) > 5 || kotlin.math.abs(next.z - start.z) > 5) {
                            continue
                        }
                        if (visited.add(next)) {
                            val state = level.getBlockState(next)
                            if (isTreeCanopy(state)) {
                                canopy.add(next)
                                queue.add(next)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun executeFell(
        level: ServerLevel,
        player: ServerPlayer,
        origin: BlockPos,
        tool: ItemStack,
        tree: TreeStructure
    ) {
        // Destroy the broken block and drop its resources
        level.destroyBlock(origin, true, player)

        // Find blocks strictly above origin.y to shift downwards
        val blocksToShift = (tree.logs + tree.leaves)
            .filter { it.y > origin.y }
            .sortedBy { it.y }

        if (blocksToShift.isNotEmpty()) {
            val stateMap = blocksToShift.associateWith { level.getBlockState(it) }
            val shiftedPositions = mutableSetOf<BlockPos>()

            for (posToShift in blocksToShift) {
                val target = posToShift.below()
                val originalState = stateMap[posToShift] ?: continue
                val targetState = level.getBlockState(target)
                val canShift = target == origin ||
                        target in stateMap ||
                        targetState.isAir ||
                        isTreeCanopy(targetState)

                if (canShift) {
                    level.setBlock(target, originalState, 3)
                    shiftedPositions.add(posToShift)
                } else if (!isTreeLog(targetState)) {
                    level.destroyBlock(posToShift, true, player)
                }
            }

            // Positions that no longer have a block shifted into them become AIR
            for (posToShift in shiftedPositions) {
                val above = posToShift.above()
                if (above !in shiftedPositions) {
                    level.setBlock(posToShift, Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }

        // Apply tool durability damage
        if (!player.isCreative) {
            tool.hurtAndBreak(1, level, player) { item ->
                player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND)
            }
        }
    }

    private fun executeChop(
        level: ServerLevel,
        player: ServerPlayer,
        origin: BlockPos,
        tool: ItemStack,
        tree: TreeStructure
    ) {
        var logsBroken = 0
        val maxDurability = if (!player.isCreative) {
            (tool.maxDamage - tool.damageValue - 1).coerceAtLeast(0)
        } else {
            Int.MAX_VALUE
        }

        // Break logs starting from the cut origin
        val sortedLogs = tree.logs.sortedBy { it.distSqr(origin) }
        for (logPos in sortedLogs) {
            if (!player.isCreative && logsBroken >= maxDurability && logsBroken > 0) {
                break
            }
            level.destroyBlock(logPos, true, player)
            logsBroken++
        }

        // Break leaves / canopy
        for (leafPos in tree.leaves) {
            level.destroyBlock(leafPos, true, player)
        }

        // Apply durability damage
        if (!player.isCreative && logsBroken > 0) {
            tool.hurtAndBreak(logsBroken, level, player) { item ->
                player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND)
            }
        }
    }
}
