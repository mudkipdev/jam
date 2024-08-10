package jam;

import jam.listener.PlayerListeners;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Server implements Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static Lobby lobby;

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        lobby = new Lobby();

        registerEventListeners();

        // TODO: re-enable bungeecord forwarding (you can check git version history)
        MojangAuth.init();
        LOGGER.info("Enabled Mojang authentication.");

        if (Config.DEBUG) {
            OpenToLAN.open();
        }

        LOGGER.info("Using {} mode.", Config.DEBUG ? "DEBUG" : "PRODUCTION");
        LOGGER.info("Starting server on {}:{}.", ADDRESS, PORT);
        server.start(ADDRESS, PORT);
    }

    public static Lobby getLobby() {
        return lobby;
    }

    private static void registerEventListeners() {
        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(lobby.getInstance());
            LOGGER.info("{} connected", event.getPlayer().getUsername());
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event ->
                LOGGER.info("{} disconnected", event.getPlayer().getUsername()));

        eventHandler.addListener(PlayerChatEvent.class, event ->
                LOGGER.info("<{}> {}", event.getPlayer().getUsername(), event.getMessage()));

        eventHandler.addListener(PlayerSpawnEvent.class, PlayerListeners::onPlayerSpawn);
        eventHandler.addListener(PlayerDisconnectEvent.class, PlayerListeners::onPlayerDisconnect);
    }
}
