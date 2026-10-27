package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * WoodCutter 2026.
 * <p>
 * Remembers the logs that were placed by players, so buildings are not cut like trees. The positions are saved in
 * the persistent data of their chunk, which survives restarts and needs no file or database. They are kept sorted,
 * so big buildings don't slow down breaking and placing logs.
 *
 * @author Christian34
 */
public final class PlacedLogs {

    private PlacedLogs() {
    }

    private static NamespacedKey key() {
        return new NamespacedKey(WoodCutter.getInstance(), "placed");
    }

    /**
     * the unsorted positions of versions up to 3.1.1, sorted into {@link #key()} when the chunk is read
     */
    private static NamespacedKey legacyKey() {
        return new NamespacedKey(WoodCutter.getInstance(), "placed_logs");
    }

    /**
     * @return the position of the block within its chunk as one number
     */
    static int pack(Block block) {
        return ((block.getY() + 2048) << 8) | ((block.getX() & 15) << 4) | (block.getZ() & 15);
    }

    /**
     * @return the sorted positions of the placed logs of the chunk, null if there are none
     */
    private static int[] positions(PersistentDataContainer data) {
        int[] saved = data.get(key(), PersistentDataType.INTEGER_ARRAY);
        int[] legacy = data.get(legacyKey(), PersistentDataType.INTEGER_ARRAY);
        if (legacy == null) return saved;
        int[] positions = saved == null ? legacy.clone() : merge(saved, legacy);
        Arrays.sort(positions);
        positions = Arrays.stream(positions).distinct().toArray();
        data.remove(legacyKey());
        save(data, positions);
        return positions;
    }

    private static int[] merge(int[] a, int[] b) {
        int[] merged = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        return merged;
    }

    private static void save(PersistentDataContainer data, int[] positions) {
        if (positions.length == 0) data.remove(key());
        else data.set(key(), PersistentDataType.INTEGER_ARRAY, positions);
    }

    /**
     * @return the positions (see {@link #pack(Block)}) of all placed logs of the chunk
     */
    public static Set<Integer> read(Chunk chunk) {
        Set<Integer> positions = new HashSet<>();
        int[] saved = positions(chunk.getPersistentDataContainer());
        if (saved != null) {
            for (int position : saved) positions.add(position);
        }
        return positions;
    }

    public static boolean isPlaced(Block block) {
        int[] saved = positions(block.getChunk().getPersistentDataContainer());
        return saved != null && Arrays.binarySearch(saved, pack(block)) >= 0;
    }

    public static void mark(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = positions(data);
        if (saved == null) saved = new int[0];
        int index = Arrays.binarySearch(saved, pack(block));
        if (index >= 0) return;
        int insert = -index - 1;
        int[] positions = new int[saved.length + 1];
        System.arraycopy(saved, 0, positions, 0, insert);
        positions[insert] = pack(block);
        System.arraycopy(saved, insert, positions, insert + 1, saved.length - insert);
        save(data, positions);
    }

    /**
     * forgets the log, e.g. when it is broken, burns, explodes or a tree grows at its position
     */
    public static void unmark(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = positions(data);
        if (saved == null) return;
        int index = Arrays.binarySearch(saved, pack(block));
        if (index < 0) return;
        int[] positions = new int[saved.length - 1];
        System.arraycopy(saved, 0, positions, 0, index);
        System.arraycopy(saved, index + 1, positions, index, saved.length - index - 1);
        save(data, positions);
    }

}
