package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WoodCutter 2026.
 * <p>
 * The sounds and particles of the animations, from the section 'effects' of the config. A sound is written as
 * 'sound volume pitch', e.g. 'minecraft:block.wood.break 1 0.8', all sounds of a list play together.
 *
 * @author Christian34
 */
final class Effects {
    static final String CHOP = "chop";
    static final String CREAK = "creak";
    static final String IMPACT = "impact";
    static final String SPLASH = "splash";
    static final String LEAVES = "leaves";

    private Effects() {
    }

    /**
     * @param shift added to the pitch of every sound, e.g. a higher sound towards the top of the tree
     */
    static void play(Location location, String name, float shift) {
        for (Sound sound : sounds(name)) {
            location.getWorld().playSound(location, sound.key(), sound.volume(), sound.pitch() + shift);
        }
    }

    /**
     * plays the sounds only to the player, e.g. for blocks they break themselves (the server plays the sounds of
     * blocks a player breaks to all other players)
     */
    static void playTo(Player player, Location location, String name, float shift) {
        for (Sound sound : sounds(name)) {
            player.playSound(location, sound.key(), sound.volume(), sound.pitch() + shift);
        }
    }

    static boolean particles(String key) {
        WoodCutter instance = WoodCutter.getInstance();
        return instance == null || instance.getConfigData() == null || instance.getConfigData().getBoolean(key);
    }

    private static List<Sound> sounds(String name) {
        WoodCutter instance = WoodCutter.getInstance();
        if (instance == null || instance.getConfigData() == null) return List.of();
        List<Sound> sounds = new ArrayList<>();
        for (String entry : instance.getConfigData().getList(ConfigData.Keys.EFFECTS_SOUNDS + name)) {
            Sound sound = parse(entry);
            if (sound != null) sounds.add(sound);
        }
        return sounds;
    }

    /**
     * @return the sound of an entry 'sound volume pitch' (volume and pitch may be left out), null if it is invalid
     */
    @Nullable
    static Sound parse(String entry) {
        String[] parts = entry.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return null;
        try {
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1;
            return new Sound(parts[0].toLowerCase(Locale.ROOT), Math.max(0, volume), Math.max(0, pitch));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    record Sound(String key, float volume, float pitch) {
    }

}
