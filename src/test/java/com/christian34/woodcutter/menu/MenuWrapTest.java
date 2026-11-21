package com.christian34.woodcutter.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests how the descriptions of the menu are split into lines.
 *
 * @author Christian34
 */
class MenuWrapTest {

    @Test
    void shortTextsStayInOneLine() {
        assertEquals(List.of("§7Mobs are pushed away"), Menu.wrap("§7Mobs are pushed away"));
        assertEquals(List.of(), Menu.wrap(""));
    }

    @Test
    void longTextsBreakAtSpacesAndKeepTheirColor() {
        List<String> lines = Menu.wrap("§7The items drop where the tree hits the ground instead of where it stood");
        assertEquals(List.of("§7The items drop where the tree hits the", "§7ground instead of where it stood"), lines);
    }

    @Test
    void semicolonsStartANewLine() {
        assertEquals(List.of("§x§1§2§3§4§5§6away: away", "§x§1§2§3§4§5§6random: anywhere"),
                Menu.wrap("§x§1§2§3§4§5§6away: away; random: anywhere"));
    }

    @Test
    void chineseBreaksBetweenCharacters() {
        List<String> lines = Menu.wrap("§7向砍树的玩家显示副标题向砍树的玩家显示副标题向砍树的玩家显示副标题");
        assertEquals(2, lines.size());
        // 19 characters as wide as two letters
        assertEquals("§7向砍树的玩家显示副标题向砍树的玩家显示", lines.get(0));
        // fullwidth semicolons too
        assertEquals(List.of("§7simple：匀速倾倒", "§7rigid：越倒越快"), Menu.wrap("§7simple：匀速倾倒；rigid：越倒越快"));
    }

}
