package jam.listener;

import jam.Config;
import jam.Server;
import jam.game.Game;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public interface PlayerListeners {
    Logger LOGGER = LoggerFactory.getLogger(PlayerListeners.class);

    Function<PlayerChatEvent, Component> CHAT_FORMAT = event -> Server.MINI_MESSAGE.deserialize(
            "<gray>" + event.getPlayer().getUsername() + " <gray>» <white>" + event.getMessage());

//    Component.textOfChildren(
//                Component.text(event.getPlayer().getUsername(), NamedTextColor.GRAY),
//                Component.text(" » ", NamedTextColor.GRAY),
//                Component.text(event.getMessage(), NamedTextColor.WHITE));

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
                            .append(Component.text("by mudkip, Cody, GoldenStack ❤", NamedTextColor.GRAY))
                            .appendNewline());
        }

        // if they're waiting in queue
        if (lobbyInstance.equals(event.getInstance())) {
            player.setGameMode(GameMode.SPECTATOR);
            player.updateViewableRule(viewer -> !player.getInstance().equals(lobbyInstance));

            player.sendMessage(Component.textOfChildren(
                    Component.newline(),
                    Component.text("The game will be starting soon! ", NamedTextColor.WHITE),
                    Component.text("Please stay patient. :)", NamedTextColor.GRAY),
                    Component.newline()));

            queue.addPlayer(player);
        }
    }

    static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var player = event.getPlayer();

        if (player.hasTag(Tags.GAME)) {
            player.getTag(Tags.GAME).despawnPlayer(player);
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

        if (!player.hasTag(Tags.GAME)) {
            return;
        }

        // TODO: item usage
    }

    static void onEntityAttack(EntityAttackEvent event) {
        if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target) {
            Game game = attacker.getTag(Tags.GAME);
            if (game != null && game == target.getTag(Tags.GAME)) {
                game.handlePlayerAttack(attacker, target);
            }
        }
    }

    static void onPlayerMove(PlayerMoveEvent event) {
        Game game = event.getPlayer().getTag(Tags.GAME);
        if (game != null) {
            game.handlePlayerMove(event);
        }
    }
}
