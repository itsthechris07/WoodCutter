package com.christian34.woodcutter.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * WoodCutter 2026.
 * <p>
 * Called before a player cuts a tree with WoodCutter - other plugins can cancel it, the player then only breaks the
 * single block.
 *
 * @author Christian34
 */
public class WoodCutterEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final BlockBreakEvent blockBreakEvent;
    private boolean cancelled = false;

    public WoodCutterEvent(BlockBreakEvent blockBreakEvent) {
        this.blockBreakEvent = blockBreakEvent;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public BlockBreakEvent getBlockBreakEvent() {
        return blockBreakEvent;
    }

    public Player getPlayer() {
        return blockBreakEvent.getPlayer();
    }

    public ItemStack getAxe() {
        return blockBreakEvent.getPlayer().getInventory().getItemInMainHand();
    }

    public Block getBlock() {
        return blockBreakEvent.getBlock();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
