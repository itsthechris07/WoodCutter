package com.christian34.woodcutter.utils;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Locale;
import java.util.stream.IntStream;

/**
 * WoodCutter 2026.
 * <p>
 * Permissions that end with a number, e.g. woodcutter.maxblocks.300 for ranks.
 *
 * @author Christian34
 */
public final class Permissions {

    private Permissions() {
    }

    /**
     * @param prefix the permission without the number, e.g. "woodcutter.maxblocks."
     * @return the numbers of all permissions of the player that start with the prefix
     */
    public static IntStream numbers(Player player, String prefix) {
        IntStream.Builder numbers = IntStream.builder();
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission().toLowerCase(Locale.ROOT);
            if (!info.getValue() || !permission.startsWith(prefix)) continue;
            try {
                numbers.add(Integer.parseInt(permission.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // not a number, e.g. woodcutter.maxblocks.*
            }
        }
        return numbers.build();
    }

}
