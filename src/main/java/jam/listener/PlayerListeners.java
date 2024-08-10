package jam.listener;

import jam.Server;
import jam.game.Game;
import jam.game.Queue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;

public interface PlayerListeners {
    static void onPlayerSpawn(PlayerSpawnEvent event) {
        Instance lobbyInstance = Server.getLobby().getInstance();
        Queue queue = Server.getLobby().getQueue();

        // if they're waiting in queue
        if (lobbyInstance.equals(event.getInstance())) {
            Player player = event.getPlayer();
            player.setGameMode(GameMode.SPECTATOR);
            player.updateViewableRule(viewer -> !player.getInstance().equals(lobbyInstance));

            player.sendMessage(Component.textOfChildren(
                    Component.text("The game will be starting soon! ", NamedTextColor.YELLOW),
                    Component.text("Please stay patient. :)", NamedTextColor.GRAY),
                    Component.newline()));

            queue.addPlayer(player);
        }
    }

    static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        Queue queue = Server.getLobby().getQueue();
        queue.removePlayer(event.getPlayer());
    }

    static void onEntityAttack(EntityAttackEvent event) {
        if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target) {
            Game game = attacker.getTag(Game.TAG);
            if (game != null && game == target.getTag(Game.TAG)) {
                game.handlePlayerAttack(attacker, target);
            }
        }
    }
}
