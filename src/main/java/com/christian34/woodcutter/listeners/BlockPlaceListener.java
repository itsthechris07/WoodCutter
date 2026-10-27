package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.woodcutting.Cutter;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * Remembers logs that were placed by players, so buildings are not cut like trees.
 *
 * @author Christian34
 */
public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        // also if 'destroy-fake-trees' is enabled, it may be disabled later
        if (Cutter.isWood(e.getBlockPlaced().getType())) PlacedLogs.mark(e.getBlockPlaced());
    }

    /**
     * a tree that grows where a placed log has been is a real tree
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent e) {
        for (BlockState state : e.getBlocks()) {
            if (Cutter.isWood(state.getType())) PlacedLogs.unmark(state.getBlock());
        }
    }

    /**
     * placed logs that a piston moves stay placed logs, else buildings could be pushed into trees
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        move(e.getBlocks(), e.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        move(e.getBlocks(), e.getDirection());
    }

    /**
     * @param direction the direction the blocks are moved to
     */
    static void move(List<Block> blocks, BlockFace direction) {
        // all at first, as a moved log can take the place of another one
        List<Block> placed = new ArrayList<>();
        for (Block block : blocks) {
            if (Cutter.isWood(block.getType()) && PlacedLogs.isPlaced(block)) placed.add(block);
        }
        placed.forEach(PlacedLogs::unmark);
        for (Block block : placed) PlacedLogs.mark(block.getRelative(direction));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        forget(List.of(e.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        forget(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        forget(e.blockList());
    }

    /**
     * the logs are gone, so their positions don't have to be saved any longer
     */
    static void forget(List<Block> blocks) {
        for (Block block : blocks) {
            if (Cutter.isWood(block.getType())) PlacedLogs.unmark(block);
        }
    }

}
