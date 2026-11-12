package goldenrose01.enchlib.tree

import goldenrose01.enchlib.compat.MCCompat
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

object TreeCutHandler {

    class TreeStructure(
        val origin: BlockPos,
        val logs: Set<BlockPos>,
        val leaves: Set<BlockPos>
    )

    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, _ ->
            // Tool and block quick check
            val tool = player.mainHandItem
            if (!player.isShiftKeyDown || !state.`is`(BlockTags.LOGS) || !isProperAxe(tool)) {
                return@register true
            }

            if (level.isClientSide) {
                // Cancel client prediction break if sneaking with valid axe on a log
                return@register false
            }

            val serverLevel = level as? ServerLevel ?: return@register true
            val serverPlayer = player as? ServerPlayer ?: return@register true

            // Resolve tree cut mode
            val mode: TreeCutMode = if (player.isCreative) {
                TreeCutMode.CHOP
            } else if (FabricLoader.getInstance().isModLoaded("eldrich-surge")) {
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
        return MCCompat.readEnchantments(stack).any { (id, lvl) ->
            (id == "eldrich-surge:lumberjack" || id == "eldritch-surge:lumberjack") && lvl > 0
        }
    }

    fun scanTree(level: ServerLevel, origin: BlockPos): TreeStructure? {
        val visited = mutableSetOf<BlockPos>()
        val logs = mutableSetOf<BlockPos>()
        val leaves = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()

        queue.add(origin)
        visited.add(origin)
        logs.add(origin)

        val maxLogs = 512
        val maxLeaves = 1024

        while (queue.isNotEmpty() && logs.size < maxLogs) {
            val current = queue.removeFirst()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val neighbor = current.offset(dx, dy, dz)
                        if (neighbor.y < origin.y - 1) continue
                        if (visited.add(neighbor)) {
                            val nState = level.getBlockState(neighbor)
                            if (nState.`is`(BlockTags.LOGS)) {
                                logs.add(neighbor)
                                queue.add(neighbor)
                            } else if (nState.`is`(BlockTags.LEAVES) && leaves.size < maxLeaves) {
                                leaves.add(neighbor)
                                queueLeaves(level, neighbor, visited, leaves, maxLeaves)
                            }
                        }
                    }
                }
            }
        }

        // Tree safety check: must have attached leaves to protect player-built structures
        if (leaves.isEmpty()) {
            return null
        }

        return TreeStructure(origin, logs, leaves)
    }

    private fun queueLeaves(
        level: ServerLevel,
        start: BlockPos,
        visited: MutableSet<BlockPos>,
        leaves: MutableSet<BlockPos>,
        maxLeaves: Int
    ) {
        val leafQueue = ArrayDeque<BlockPos>()
        leafQueue.add(start)
        var count = 0
        while (leafQueue.isNotEmpty() && leaves.size < maxLeaves && count < 128) {
            count++
            val curr = leafQueue.removeFirst()
            for (dir in Direction.values()) {
                val next = curr.relative(dir)
                if (visited.add(next)) {
                    val state = level.getBlockState(next)
                    if (state.`is`(BlockTags.LEAVES)) {
                        leaves.add(next)
                        leafQueue.add(next)
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
                        targetState.`is`(BlockTags.LEAVES)

                if (canShift) {
                    level.setBlock(target, originalState, 3)
                    shiftedPositions.add(posToShift)
                } else {
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

        // Break leaves
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
