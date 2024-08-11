package jam;

import jam.game.Team;
import jam.listener.PlayerListeners;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Server implements Config {
    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static Lobby lobby;

    public static void main(String[] args) {
        System.setProperty(
                "org.slf4j.simpleLogger.defaultLogLevel",
                Config.DEBUG ? "trace" : "info");

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

        try {
            server.start(ADDRESS, PORT);
        } catch (RuntimeException e) {
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                    player.kick(Component.text(
                            "A fatal exception occured, check the logs!",
                            NamedTextColor.RED)));

            LOGGER.error("Fatal exception occured!", e);
            System.exit(1);
        }
    }

    public static Lobby getLobby() {
        return lobby;
    }

    private static void registerEventListeners() {
        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(ServerListPingEvent.class, event -> {
            var data = event.getResponseData();
            data.setMaxPlayer(500);
            data.setDescription(Component.textOfChildren(
                    MINI_MESSAGE.deserialize("<rainbow>Color Chase"),
                    Component.newline(),
                    Component.text("by mudkip, Cody, GoldenStack")));

            event.setResponseData(data);
        });

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(lobby.getInstance());
            LOGGER.info("{} connected", event.getPlayer().getUsername());
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event ->
                LOGGER.info("{} disconnected", event.getPlayer().getUsername()));

        eventHandler
                .addListener(PlayerChatEvent.class, PlayerListeners::onPlayerChat)
                .addListener(PlayerSpawnEvent.class, PlayerListeners::onPlayerSpawn)
                .addListener(PlayerDisconnectEvent.class, PlayerListeners::onPlayerDisconnect)
                .addListener(PlayerUseItemEvent.class, PlayerListeners::onPlayerUseItem)
                .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
                .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true));
    }
}
