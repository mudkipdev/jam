package jam;

import jam.listener.PlayerListeners;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

public final class Server implements Config {
    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static final @NotNull ResourcePackRequest RESOURCE_PACK_REQUEST = ResourcePackRequest.resourcePackRequest()
            .required(true)
            .packs(List.of(Config.RESOURCE_PACK))
            .build();

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

        byte[] resource;
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("pack.png")) {
            if (stream == null) throw new IOException("Could not find pack.png");
            resource = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] base64 = Base64.getEncoder().encode(resource);
        String favicon = "data:image/png;base64," + new String(base64);

        eventHandler.addListener(ServerListPingEvent.class, event -> {
            var data = event.getResponseData();
            data.setMaxPlayer(500);
            data.setFavicon(favicon);
            data.setDescription(Component.textOfChildren(
                    MINI_MESSAGE.deserialize("<rainbow>Color Chase"),
                    Component.newline(),
                    Component.text("by mudkip, Cody, GoldenStack")));

            event.setResponseData(data);
        });

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(lobby.getInstance());
            event.getPlayer().sendResourcePacks(RESOURCE_PACK_REQUEST);
            LOGGER.info("{} connected", event.getPlayer().getUsername());
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event ->
                LOGGER.info("{} disconnected", event.getPlayer().getUsername()));

        eventHandler
                .addListener(PlayerChatEvent.class, PlayerListeners::onPlayerChat)
                .addListener(PlayerSpawnEvent.class, PlayerListeners::onPlayerSpawn)
                .addListener(PlayerDisconnectEvent.class, PlayerListeners::onPlayerDisconnect)
                .addListener(EntityAttackEvent.class, PlayerListeners::onEntityAttack)
                .addListener(PlayerDeathEvent.class, PlayerListeners::onPlayerDeath)
                .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
                .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true));
    }
}
