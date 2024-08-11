package jam.listener;

import jam.Config;
import jam.Server;
import jam.game.Game;
import jam.utility.Tags;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.entity.EntityAttackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public interface PlayerListeners {
    Logger LOGGER = LoggerFactory.getLogger(PlayerListeners.class);

    Set<UUID> DEVELOPERS = Set.of(
            UUID.fromString("0541ed27-7595-4e6a-9101-6c07f879b7b5"),  // mudkip
            UUID.fromString("7beddb4f-8574-4e21-ad45-9b6b88957725"),  // golden
            UUID.fromString("45bae2bd-1889-44f1-91f6-4f1730b4665b"),  // cody
            UUID.fromString("7bd5b459-1e6b-4753-8274-1fbd2fe9a4d5")); // emortal

    Function<PlayerChatEvent, Component> CHAT_FORMAT = event -> Server.MINI_MESSAGE.deserialize(
            (DEVELOPERS.contains(event.getPlayer().getUuid()) ? "<gradient:gold:#FF76B6>" : "<gray>")
                    + event.getPlayer().getUsername() + " <gray>» <white>" + event.getMessage());

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
                            .append(Server.MINI_MESSAGE.deserialize("<gradient:#FF6C32:#FF76B6>Made for the Minestom Game Jam"))
                            .appendNewline()
                            .append(Component.text("by mudkip, Cody, GoldenStack ❤", NamedTextColor.GRAY))
                            .appendNewline());

            player.openBook(Book.book(
                    Component.text("How to Play"),
                    Component.text("mudkip, Cody, GoldenStack"),
                    Server.MINI_MESSAGE.deserialize(Config.INSTRUCTIONS)));
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

    static void onEntityAttack(EntityAttackEvent event) {
        if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target) {
            var game = attacker.getTag(Tags.GAME);

            if (game != null && game == target.getTag(Tags.GAME)) {
                game.handlePlayerAttack(attacker, target);
            }
        }
    }
}
