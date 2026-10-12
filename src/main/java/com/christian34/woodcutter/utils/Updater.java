package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.commands.CommandWoodCutter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * Checks the releases on GitHub for updates and tells admins when they join.
 *
 * @author Christian34
 */
public class Updater implements Listener {
    private static final URI VERSION_URI = URI.create("https://api.github.com/repos/itsthechris07/WoodCutter/releases/latest");
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NUMBERS = Pattern.compile("\\d+");
    private final WoodCutter instance;
    private volatile boolean available;

    public Updater(WoodCutter instance) {
        this.instance = instance;
        this.available = false;
        check();
    }

    /**
     * compares the numbers of both versions (e.g. 3.0.1 &gt; 3.0.0 &gt; 2.0.7), a release is newer than its betas
     */
    static boolean isNewer(@NotNull String latest, @NotNull String current) {
        int[] a = numbers(latest.split("-")[0]), b = numbers(current.split("-")[0]);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return !latest.contains("-") && current.contains("-");
    }

    private static int[] numbers(String version) {
        Matcher matcher = NUMBERS.matcher(version);
        return matcher.results().mapToInt(result -> {
            try {
                return Integer.parseInt(result.group());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }).toArray();
    }

    /**
     * reads the tag of the release (e.g. v3.0.1) from the response of the GitHub api
     */
    static @Nullable String tagName(@NotNull String body) {
        Matcher matcher = TAG_NAME.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    public void check() {
        if (WoodCutter.isOffline()) return;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder(VERSION_URI).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json").GET().build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
            // 404 = no release published yet
            if (error == null && response.statusCode() == 404) return;
            String latest = error == null && response.statusCode() == 200 ? tagName(response.body()) : null;
            if (latest == null) {
                Debug.warn("Update checker failed!");
                return;
            }
            if (isNewer(latest, instance.getPluginMeta().getVersion())) {
                this.available = true;
                Message.UPDATE_AVAILABLE.send(instance.getServer().getConsoleSender());
            }
        });
    }

    public boolean isAvailable() {
        return this.available;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (isAvailable() && e.getPlayer().hasPermission(CommandWoodCutter.PERMISSION_ADMIN)) {
            Message.UPDATE_AVAILABLE.send(e.getPlayer());
        }
    }

}
