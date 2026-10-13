package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.utils.Debug;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * WoodCutter 2026.
 * <p>
 * The region flag 'woodcutter' of WorldGuard: '/rg flag &lt;region&gt; woodcutter deny' disables WoodCutter in a
 * region (players break single logs there, if they may build), it is allowed by default. This class must only be
 * loaded if WorldGuard is installed.
 *
 * @author Christian34
 */
public class WorldGuardHook {
    public static final String FLAG = "woodcutter";
    private final StateFlag flag;

    private WorldGuardHook(StateFlag flag) {
        this.flag = flag;
    }

    /**
     * registers the flag - WorldGuard only accepts flags while the plugins are loaded (onLoad), before it is enabled
     *
     * @return null if the flag could not be registered
     */
    @Nullable
    public static WorldGuardHook register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag(FLAG, true);
            registry.register(flag);
            return new WorldGuardHook(flag);
        } catch (FlagConflictException | IllegalStateException ex) {
            // registered already, e.g. by this plugin before a reload of the server
            Flag<?> existing = registry.get(FLAG);
            if (existing instanceof StateFlag flag) return new WorldGuardHook(flag);
            Debug.warn("Couldn't register the WorldGuard flag '" + FLAG + "': " + ex);
            return null;
        }
    }

    /**
     * @return false if the flag denies WoodCutter for the player at the block
     */
    public boolean isAllowed(Player player, Block block) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .testState(BukkitAdapter.adapt(block.getLocation()), WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

}
