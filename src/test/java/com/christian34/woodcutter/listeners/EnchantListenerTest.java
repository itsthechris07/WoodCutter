package com.christian34.woodcutter.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the chance of getting the enchantment.
 *
 * @author Christian34
 */
class EnchantListenerTest {

    @Test
    void fullChanceAlwaysEnchants() {
        for (int i = 0; i < 200; i++) {
            assertTrue(EnchantListener.roll(100));
            assertTrue(EnchantListener.roll(250));
            assertFalse(EnchantListener.roll(0));
        }
    }

    @Test
    void lowChanceEnchantsSometimes() {
        int hits = 0;
        for (int i = 0; i < 10_000; i++) {
            if (EnchantListener.roll(10)) hits++;
        }
        // 1 of 10
        assertTrue(hits > 700 && hits < 1300, "hits: " + hits);
    }

}
