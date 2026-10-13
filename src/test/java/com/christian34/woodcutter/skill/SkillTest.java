package com.christian34.woodcutter.skill;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.woodcutting.Cutter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the woodcutting skill: experience, levels, perks, unlocked features and its commands.
 *
 * @author Christian34
 */
class SkillTest extends PluginTestBase {
    private static final int X = 0, Y = 70, Z = 0;
    private Skill skill;

    /**
     * ten levels with ten experience each, so the perks grow by a tenth with every level
     */
    @BeforeEach
    void setUpSkill() {
        this.skill = plugin.getSkill();
        setConfig(ConfigData.Keys.SKILL_MAX_LEVEL, 10);
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 10);
        setConfig(ConfigData.Keys.SKILL_XP_INCREASE, 0);
        setConfig(ConfigData.Keys.DELAY, false);
    }

    private Block tree(int x, int height) {
        for (int y = 0; y < height; y++) {
            world.getBlockAt(x, Y + y, Z).setType(Material.OAK_LOG);
        }
        return world.getBlockAt(x, Y, Z);
    }

    private void breakBlock(PlayerMock player, Block block) {
        server.getPluginManager().callEvent(new BlockBreakEvent(block, player));
    }

    @Test
    void levelsNeedMoreAndMoreExperience() {
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 50);
        setConfig(ConfigData.Keys.SKILL_XP_INCREASE, 25);
        assertEquals(0, skill.xpForLevel(0));
        assertEquals(50, skill.xpForLevel(1));
        assertEquals(50 + 75, skill.xpForLevel(2));
        assertEquals(50 + 75 + 100, skill.xpForLevel(3));
        assertEquals(0, skill.levelOf(49));
        assertEquals(1, skill.levelOf(50));
        assertEquals(2, skill.levelOf(224));
        assertEquals(10, skill.levelOf(Long.MAX_VALUE / 2));
    }

    @Test
    void treesGiveExperience() {
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(6, skill.xp(player.getUniqueId()));
        assertEquals(0, skill.level(player.getUniqueId()));
        assertTrue(messages(player).stream().noneMatch(message -> message.contains("woodcutting level")));

        breakBlock(player, tree(X + 10, 6));
        assertEquals(1, skill.level(player.getUniqueId()));
        assertContains(messages(player), "woodcutting level 1");

        // a single log is no tree
        breakBlock(player, tree(X + 20, 1));
        assertEquals(12, skill.xp(player.getUniqueId()));
    }

    @Test
    void noExperienceIfDisabled() {
        setConfig(ConfigData.Keys.SKILL, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(0, skill.xp(player.getUniqueId()));
        skill.reward(player, 100);
        assertEquals(0, skill.perk(player, Skill.PERK_MAX_BLOCKS));
        player.performCommand("woodcutter level");
        assertContains(messages(player), "skill is disabled");
    }

    @Test
    void perksGrowWithTheLevel() {
        setConfig(ConfigData.Keys.MAX_BLOCKS, 100);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_MAX_BLOCKS, 120);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_DELAY, 20);
        PlayerMock player = addCutter("Cutter");
        assertEquals(100, Cutter.maxBlocks(player, plugin.getConfigData(), 1));

        skill.reward(player, 50);
        assertEquals(5, skill.level(player.getUniqueId()));
        assertEquals(60, skill.perk(player, Skill.PERK_MAX_BLOCKS));
        assertEquals(160, Cutter.maxBlocks(player, plugin.getConfigData(), 1));

        setConfig(ConfigData.Keys.DELAY, true);
        setConfig(ConfigData.Keys.DELAY_TIME, 30);
        plugin.getDelays().put(player.getUniqueId(), System.currentTimeMillis());
        assertEquals(20, plugin.remainingDelay(player));

        // more experience than the highest level needs
        skill.reward(player, 500);
        assertEquals(10, skill.level(player.getUniqueId()));
        assertEquals(120, skill.perk(player, Skill.PERK_MAX_BLOCKS));
    }

    @Test
    void levelUpShowsWhatThePlayerGot() {
        setConfig(ConfigData.Keys.SKILL_MAX_LEVEL, 10);
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 10);
        setConfig(ConfigData.Keys.SKILL_XP_INCREASE, 0);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_MAX_BLOCKS, 100);
        // only grows every second level
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_SPEED, 5);
        setConfig(ConfigData.Keys.SKILL_UNLOCKS + Skill.UNLOCK_REPLANT, 2);
        PlayerMock player = addCutter("Cutter");
        plugin.getSkill().reward(player, 10);
        List<String> messages = messages(player);
        assertContains(messages, "level 1");
        assertContains(messages, "Bigger trees: +10 logs");
        assertContains(messages, "Faster cutting: +1 logs per tick");
        assertTrue(messages.stream().noneMatch(message -> message.contains("sapling")));

        plugin.getSkill().reward(player, 10);
        messages = messages(player);
        assertContains(messages, "Bigger trees: +20 logs");
        assertContains(messages, "A new sapling is planted for free");
        // perks that stay the same are left out
        assertTrue(messages.stream().noneMatch(message -> message.contains("Faster cutting")));
    }

    @Test
    void costsGetLower() {
        setConfig(ConfigData.Keys.COSTS_HUNGER, 4);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_COSTS, 50);
        PlayerMock player = addCutter("Cutter");
        skill.reward(player, 100);
        player.setFoodLevel(10);
        breakBlock(player, tree(X, 6));
        assertEquals(8, player.getFoodLevel());
    }

    @Test
    void speedsUpTheAnimation() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 1);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_SPEED, 4);
        PlayerMock player = addCutter("Cutter");
        skill.reward(player, 50);
        // two more logs per tick at half of the levels
        breakBlock(player, tree(X, 8));
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 3, Z).getType());
        assertEquals(Material.OAK_LOG, world.getBlockAt(X, Y + 4, Z).getType());
    }

    @Test
    void featuresAreUnlocked() {
        setConfig(ConfigData.Keys.SKILL_UNLOCKS + Skill.UNLOCK_REPLANT, 2);
        setConfig(ConfigData.Keys.SKILL_UNLOCKS + Skill.UNLOCK_DROPS_TO_INVENTORY, 0);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        PlayerMock player = addCutter("Cutter");
        skill.reward(player, 10);
        assertFalse(skill.isUnlocked(player, Skill.UNLOCK_REPLANT));

        Block root = tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.AIR, root.getType());

        skill.reward(player, 10);
        assertTrue(skill.isUnlocked(player, Skill.UNLOCK_REPLANT));
        tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.OAK_SAPLING, root.getType());

        skill.reward(player, 1000);
        assertFalse(skill.isUnlocked(player, Skill.UNLOCK_DROPS_TO_INVENTORY));
        assertFalse(Cutter.dropsToInventory(plugin, player));
    }

    @Test
    void savesTheAxe() {
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_AXE_SAVER, 100);
        PlayerMock player = addCutter("Cutter");
        PlayerItemDamageEvent other = new PlayerItemDamageEvent(player, player.getInventory().getItemInMainHand(), 1);
        skill.reward(player, 100);
        // the axe is only saved while a tree is cut
        server.getPluginManager().callEvent(other);
        assertFalse(other.isCancelled());

        breakBlock(player, tree(X, 6));
        PlayerItemDamageEvent event = new PlayerItemDamageEvent(player, player.getInventory().getItemInMainHand(), 1);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void experienceIsSaved() {
        PlayerMock player = addCutter("Cutter");
        PlayerMock better = addCutter("Better");
        skill.reward(player, 15);
        skill.reward(better, 40);
        skill.save();

        Skill loaded = new Skill(plugin);
        assertEquals(15, loaded.xp(player.getUniqueId()));
        assertEquals(List.of("Better", "Cutter"), loaded.top(10).stream().map(Skill.Entry::name).toList());
        assertEquals(List.of("Better"), loaded.top(1).stream().map(Skill.Entry::name).toList());
        Skill.Entry entry = loaded.find("better");
        assertNotNull(entry);
        assertEquals(40, entry.xp());
        assertNull(loaded.find("Nobody"));
    }

    @Test
    void commandsShowTheLevelAndTheBestPlayers() {
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_MAX_BLOCKS, 120);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_FORESTER_LUCK, 3);
        PlayerMock player = addCutter("Cutter");
        PlayerMock guest = addPlayer("Guest");
        skill.reward(player, 55);
        messages(player);

        player.performCommand("woodcutter level");
        List<String> messages = messages(player);
        assertContains(messages, "Cutter | Level 5 | 5/10 XP");
        assertContains(messages, "Bigger trees: +60 logs");
        assertContains(messages, "Forester's luck: 1.5 %");

        guest.performCommand("woodcutter level cutter");
        messages = messages(guest);
        assertContains(messages, "Cutter | Level 5");
        assertTrue(messages.stream().noneMatch(message -> message.contains("Bigger trees")));
        guest.performCommand("woodcutter level Nobody");
        assertContains(messages(guest), "has not cut a tree yet");

        guest.performCommand("woodcutter top");
        assertContains(messages(guest), "1. Cutter | Level 5 | 55 XP");
        guest.performCommand("woodcutter");
        assertContains(messages(guest), "/WoodCutter level");
    }

}
