package jam.listener;

import jam.Config;
import jam.Server;
import jam.utility.Components;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.entity.EntityAttackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public interface PlayerListeners {
    Logger LOGGER = LoggerFactory.getLogger(PlayerListeners.class);

    Function<PlayerChatEvent, Component> CHAT_FORMAT = event -> Server.MINI_MESSAGE.deserialize(
            (Config.DEVELOPERS.contains(event.getPlayer().getUuid()) ? "<gradient:#FF76B6:gold>" : "<gray>")
                    + event.getPlayer().getUsername() + " <gray>» <white>" + event.getMessage());

    Component STARTING_SOON = Server.MINI_MESSAGE.deserialize(Components.PREFIX_MM + "<gray>The game will start soon. Please stay patient. :)");

    static void onPlayerSpawn(PlayerSpawnEvent event) {
        var lobbyInstance = Server.getLobby().getInstance();
        var queue = Server.getLobby().getQueue();
        var player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);

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
        }

        // if they're waiting in queue
        if (lobbyInstance.equals(event.getInstance())) {
            player.sendMessage(STARTING_SOON);
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

    static void onPlayerDeath(PlayerDeathEvent event) {
        var game = event.getPlayer().getTag(Tags.GAME);
        if (game != null) {
            game.handleExternalPlayerDeath(event);
        }
    }
}
