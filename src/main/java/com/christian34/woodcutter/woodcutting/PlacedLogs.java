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
 * the persistent data of their chunk, which survives restarts and needs no file or database.
 *
 * @author Christian34
 */
public final class PlacedLogs {

    private PlacedLogs() {
    }

    private static NamespacedKey key() {
        return new NamespacedKey(WoodCutter.getInstance(), "placed_logs");
    }

    /**
     * @return the position of the block within its chunk as one number
     */
    static int pack(Block block) {
        return ((block.getY() + 2048) << 8) | ((block.getX() & 15) << 4) | (block.getZ() & 15);
    }

    /**
     * @return the positions (see {@link #pack(Block)}) of all placed logs of the chunk
     */
    public static Set<Integer> read(Chunk chunk) {
        Set<Integer> positions = new HashSet<>();
        int[] saved = chunk.getPersistentDataContainer().get(key(), PersistentDataType.INTEGER_ARRAY);
        if (saved != null) {
            for (int position : saved) positions.add(position);
        }
        return positions;
    }

    public static boolean isPlaced(Block block) {
        int[] saved = block.getChunk().getPersistentDataContainer().get(key(), PersistentDataType.INTEGER_ARRAY);
        if (saved == null) return false;
        int position = pack(block);
        for (int other : saved) {
            if (other == position) return true;
        }
        return false;
    }

    public static void mark(Block block) {
        if (isPlaced(block)) return;
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = data.get(key(), PersistentDataType.INTEGER_ARRAY);
        int[] positions = saved == null ? new int[1] : Arrays.copyOf(saved, saved.length + 1);
        positions[positions.length - 1] = pack(block);
        data.set(key(), PersistentDataType.INTEGER_ARRAY, positions);
    }

    /**
     * forgets the log, e.g. when it is broken or a tree grows at its position
     */
    public static void unmark(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] saved = data.get(key(), PersistentDataType.INTEGER_ARRAY);
        if (saved == null) return;
        int position = pack(block);
        int[] positions = Arrays.stream(saved).filter(other -> other != position).toArray();
        if (positions.length == saved.length) return;
        if (positions.length == 0) data.remove(key());
        else data.set(key(), PersistentDataType.INTEGER_ARRAY, positions);
    }

}
