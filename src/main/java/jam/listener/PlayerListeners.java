package jam.listener;

import jam.Config;
import jam.Server;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public interface PlayerListeners {
    Logger LOGGER = LoggerFactory.getLogger(PlayerListeners.class);

    Function<PlayerChatEvent, Component> CHAT_FORMAT = event -> {
        var team = event.getPlayer().getTag(Tags.TEAM);

        return Component.textOfChildren(
                Component.text(
                        event.getPlayer().getUsername(),
                        team == null
                                ? NamedTextColor.GRAY
                                : team.color().getTextColor()),

                Component.text(" » ", NamedTextColor.GRAY),

                Component.text(event.getMessage(), NamedTextColor.WHITE));

    };

    static void onPlayerSpawn(PlayerSpawnEvent event) {
        var lobbyInstance = Server.getLobby().getInstance();
        var queue = Server.getLobby().getQueue();
        var player = event.getPlayer();

        if (event.isFirstSpawn()) {
            player.setReducedDebugScreenInformation(!Config.DEBUG);
            player.sendPlayerListHeaderAndFooter(
                    Component.newline()
                            .append(Server.MINI_MESSAGE.deserialize("<rainbow><b>Color Chase"))
                            .appendNewline(),
                    Component.text(" ".repeat(50))
                            .appendNewline()
                            .append(Server.MINI_MESSAGE.deserialize("<gradient:#FF76B6:#FF6C32>Made for the Minestom Game Jam"))
                            .appendNewline()
                            .append(Component.text("by mudkip, Cody, GoldenStack", NamedTextColor.GRAY))
                            .appendNewline());
        }

        // if they're waiting in queue
        if (lobbyInstance.equals(event.getInstance())) {
            player.setGameMode(GameMode.SPECTATOR);
            player.updateViewableRule(viewer -> !player.getInstance().equals(lobbyInstance));

            player.sendMessage(Component.textOfChildren(
                    Component.newline(),
                    Component.text("The game will be starting soon! ", NamedTextColor.YELLOW),
                    Component.text("Please stay patient. :)", NamedTextColor.GRAY),
                    Component.newline()));

            queue.addPlayer(player);
        }
    }

    static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var player = event.getPlayer();

        if (player.hasTag(Tags.TEAM)) {
            var game = player.getTag(Tags.TEAM).game();
            game.despawnPlayer(player);
        } else {
            var queue = Server.getLobby().getQueue();
            queue.removePlayer(event.getPlayer());
        }
    }

    static void onPlayerChat(PlayerChatEvent event) {
        event.setChatFormat(CHAT_FORMAT);
        LOGGER.info("<{}> {}", event.getPlayer().getUsername(), event.getMessage());
    }

    static void onPlayerUseItem(PlayerUseItemEvent event) {
        var player = event.getPlayer();

        if (!player.hasTag(Tags.TEAM)) {
            return;
        }

        // TODO: item usage
    }
}
