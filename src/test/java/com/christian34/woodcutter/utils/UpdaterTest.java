package com.christian34.woodcutter.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the version comparison of the update check.
 *
 * @author Christian34
 */
class UpdaterTest {

    @Test
    void newerVersionIsAnUpdate() {
        assertTrue(Updater.isNewer("3.0.1", "3.0.0"));
        assertTrue(Updater.isNewer("3.1", "3.0.9"));
        assertTrue(Updater.isNewer("10.0.0", "9.9.9"));
    }

    @Test
    void olderOrSameVersionIsNoUpdate() {
        assertFalse(Updater.isNewer("2.0.7", "3.0.0"));
        assertFalse(Updater.isNewer("3.0.0", "3.0.0"));
        assertFalse(Updater.isNewer("3.0", "3.0.0"));
    }

    @Test
    void releaseIsNewerThanItsBeta() {
        assertTrue(Updater.isNewer("3.0.0", "3.0.0-beta.2"));
        assertFalse(Updater.isNewer("3.0.0-beta.2", "3.0.0"));
    }

    @Test
    void tagOfTheGitHubReleaseIsRead() {
        assertEquals("v3.0.1", Updater.tagName("{\"url\":\"x\",\"tag_name\": \"v3.0.1\",\"name\":\"WoodCutter 3.0.1\"}"));
        assertNull(Updater.tagName("{\"message\":\"Not Found\"}"));
        assertTrue(Updater.isNewer("v3.0.1", "3.0.0"));
        assertFalse(Updater.isNewer("v3.0.0", "3.0.0"));
    }

}
