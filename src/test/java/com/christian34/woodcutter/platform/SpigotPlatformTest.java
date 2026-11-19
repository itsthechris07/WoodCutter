package com.christian34.woodcutter.platform;

import com.christian34.woodcutter.PluginTestBase;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the platform of Spigot directly, also where the mocked Paper server can't tell it apart (the action bar and
 * clickable lines of the chat api of BungeeCord).
 *
 * @author Christian34
 */
class SpigotPlatformTest extends PluginTestBase {
    private final SpigotPlatform spigot = new SpigotPlatform();

    private record Sent(ChatMessageType type, BaseComponent component) {
    }

    private PlayerMock player(List<Sent> sent) {
        PlayerMock player = new PlayerMock(server, "Player") {
            @Override
            public Player.Spigot spigot() {
                return new Player.Spigot() {
                    @Override
                    public void sendMessage(BaseComponent component) {
                        sent.add(new Sent(ChatMessageType.CHAT, component));
                    }

                    @Override
                    public void sendMessage(ChatMessageType type, BaseComponent component) {
                        sent.add(new Sent(type, component));
                    }

                    @Override
                    public void sendMessage(ChatMessageType type, BaseComponent... components) {
                        for (BaseComponent component : components) sent.add(new Sent(type, component));
                    }
                };
            }
        };
        server.addPlayer(player);
        return player;
    }

    @Test
    void platformOfTheServer() {
        assertEquals(System.getProperty(Platform.PROPERTY, "paper"), Platform.get().name());
    }

    @Test
    void actionBarAndClickableLines() {
        List<Sent> sent = new ArrayList<>();
        PlayerMock player = player(sent);
        spigot.sendActionBar(player, "§cToo hungry");
        assertEquals(ChatMessageType.ACTION_BAR, sent.getFirst().type());
        assertEquals("Too hungry", sent.getFirst().component().toPlainText());

        spigot.sendCommand(player, "§7/§9WoodCutter toggle", "/woodcutter toggle", true);
        spigot.sendCommand(player, "§7/§9WoodCutter give", "/woodcutter give ", false);
        Sent toggle = sent.get(1);
        assertEquals(ChatMessageType.CHAT, toggle.type());
        assertEquals("/WoodCutter toggle", toggle.component().toPlainText());
        assertEquals(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/woodcutter toggle"), toggle.component().getClickEvent());
        assertEquals(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/woodcutter give "), sent.get(2).component().getClickEvent());
        assertNotNull(toggle.component().getHoverEvent());

        // the console gets the text
        spigot.sendCommand(server.getConsoleSender(), "§9help", "/woodcutter", true);
        assertEquals(3, sent.size());
    }

    @Test
    void titles() {
        PlayerMock player = server.addPlayer();
        spigot.showTitle(player, "", "§6Timber!", 2, 24, 10);
        assertEquals("§6Timber!", player.nextSubTitle());
    }

    @Test
    void loreLinesOfOtherPluginsStay() {
        ItemMeta meta = new ItemStack(Material.DIAMOND_AXE).getItemMeta();
        meta.setLore(List.of("§5Sharp", "§7WoodCutter"));
        assertTrue(spigot.hasLoreLine(meta, line -> line.startsWith("WoodCutter")));
        spigot.replaceLoreLines(meta, line -> line.startsWith("WoodCutter"), "WoodCutter II");
        assertEquals(List.of("§5Sharp", "§7WoodCutter II"), meta.getLore());
        spigot.replaceLoreLines(meta, line -> line.startsWith("WoodCutter"), null);
        assertEquals(List.of("§5Sharp"), meta.getLore());
        assertFalse(spigot.hasLoreLine(meta, line -> line.startsWith("WoodCutter")));
        spigot.replaceLoreLines(meta, line -> line.startsWith("Sharp"), null);
        assertFalse(meta.hasLore());

        // names and lore of the menu are not in italics
        spigot.setDisplayName(meta, "§9Settings");
        assertTrue(meta.getDisplayName().endsWith("§9Settings"));
        spigot.setLore(meta, List.of("§7Click"));
        assertTrue(meta.getLore().getFirst().endsWith("§7Click"));
    }

    @Test
    void tasksOfPlayersWhoLeftAreRetired() {
        PlayerMock player = server.addPlayer();
        AtomicBoolean ran = new AtomicBoolean();
        AtomicBoolean retired = new AtomicBoolean();
        spigot.runLater(plugin, player, () -> ran.set(true), () -> retired.set(true), 2);
        server.getScheduler().performTicks(3);
        assertTrue(ran.get());
        assertFalse(retired.get());

        ran.set(false);
        spigot.runLater(plugin, player, () -> ran.set(true), () -> retired.set(true), 2);
        player.disconnect();
        server.getScheduler().performTicks(3);
        assertFalse(ran.get());
        assertTrue(retired.get());

        AtomicBoolean global = new AtomicBoolean();
        spigot.runGlobal(plugin, () -> global.set(true));
        server.getScheduler().performTicks(1);
        assertTrue(global.get());
        assertTrue(spigot.owns(player.getLocation()));
    }

}
