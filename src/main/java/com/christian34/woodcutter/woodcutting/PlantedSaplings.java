package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * WoodCutter 2026.
 * <p>
 * Remembers the saplings WoodCutter has planted, so players can't break them until they grow (setting
 * 'replant.protect'). Like the placed logs, the positions are saved sorted in the persistent data of their chunk.
 *
 * @author Christian34
 */
public final class PlantedSaplings {

    private PlantedSaplings() {
    }

    private static NamespacedKey key() {
        return new NamespacedKey(WoodCutter.getInstance(), "saplings");
    }

    private static int[] positions(PersistentDataContainer data) {
        int[] saved = data.get(key(), PersistentDataType.INTEGER_ARRAY);
        return saved == null ? new int[0] : saved;
    }

    public static boolean isPlanted(Block block) {
        return Arrays.binarySearch(positions(block.getChunk().getPersistentDataContainer()), PlacedLogs.pack(block)) >= 0;
    }

    static void mark(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = positions(data);
        int index = Arrays.binarySearch(saved, PlacedLogs.pack(block));
        if (index >= 0) return;
        int insert = -index - 1;
        int[] positions = new int[saved.length + 1];
        System.arraycopy(saved, 0, positions, 0, insert);
        positions[insert] = PlacedLogs.pack(block);
        System.arraycopy(saved, insert, positions, insert + 1, saved.length - insert);
        data.set(key(), PersistentDataType.INTEGER_ARRAY, positions);
    }

    /**
     * forgets the sapling, e.g. when it grows or is broken
     */
    public static void unmark(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = positions(data);
        int index = Arrays.binarySearch(saved, PlacedLogs.pack(block));
        if (index < 0) return;
        int[] positions = new int[saved.length - 1];
        System.arraycopy(saved, 0, positions, 0, index);
        System.arraycopy(saved, index + 1, positions, index, saved.length - index - 1);
        if (positions.length == 0) data.remove(key());
        else data.set(key(), PersistentDataType.INTEGER_ARRAY, positions);
    }

}
