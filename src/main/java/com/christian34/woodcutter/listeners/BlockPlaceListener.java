package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.woodcutting.Cutter;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;

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
     * a tree that grows where a placed log has been (it burned, exploded, ...) is a real tree
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent e) {
        for (BlockState state : e.getBlocks()) {
            if (Cutter.isWood(state.getType())) PlacedLogs.unmark(state.getBlock());
        }
    }

}
