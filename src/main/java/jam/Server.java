package jam;

import jam.listener.PlayerListeners;
import jam.utility.Components;
import jam.utility.JamConditions;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.network.packet.server.common.ServerLinksPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public final class Server implements Config {
    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(builder -> builder.tag("prefix", new Inserting() {
                @Override
                public @NotNull Component value() {
                    return Components.PREFIX;
                }

                @Override
                public boolean allowsChildren() {
                    return false;
                }
            })).build();

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

        var server = MinecraftServer.init();
        lobby = new Lobby();
        registerEventListeners();

        MinecraftServer.getCommandManager().register(new Command("start") {{
            this.setCondition(Conditions.all(
                    Conditions::playerOnly,
                    Conditions.any(JamConditions.DEVELOPER, (sender, commandString) ->
                            "notmattw".equals(((Player) sender).getUsername())),
                    JamConditions.LOBBY));

            this.addSyntax((sender, context) -> {
                var player = (Player) sender;
                lobby.sendMessage(Server.MM.deserialize("<prefix><white>" + player.getUsername() + " <gray>has force started the game."));
                lobby.getQueue().start();
            });
        }});

        MinecraftServer.getCommandManager().register(new Command("find") {{
            this.setCondition(Conditions.all(
                    Conditions::playerOnly,
                    JamConditions.LOBBY));

            this.addSyntax((sender, context) -> {
                var player = (Player) sender;

                if (lobby.getColorblind().addViewer(player)) {
                    player.sendMessage(Component.text("You find yourself very lost...", NamedTextColor.GRAY, TextDecoration.BOLD));
                } else {
                    lobby.getColorblind().removeViewer(player);
                    player.sendMessage(Component.text("You're back! That was weird.", NamedTextColor.GRAY, TextDecoration.BOLD));
                }
            });
        }});

        // Red herring easter eggs
        MinecraftServer.getCommandManager().register(new Command("shallow") {{
            this.addSyntax((sender, context) -> {});
        }});

        MinecraftServer.getCommandManager().register(new Command("vignette") {{
            this.addSyntax((sender, context) -> {}, ArgumentType.Integer("count"));
        }});

        MinecraftServer.getCommandManager().register(new Command("custom") {{
            this.addSyntax((sender, context) -> {}, ArgumentType.StringArray("count"));
        }});

        MinecraftServer.getCommandManager().register(new Command("secret") {{
            this.addSyntax((sender, context) -> {});
        }});



        // TODO: re-enable bungeecord forwarding (you can check git version history)
        if (!Config.OFFLINE_MODE) {
            MojangAuth.init();
            LOGGER.info("Enabled Mojang authentication.");
        }

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
        var eventHandler = MinecraftServer.getGlobalEventHandler();

        byte[] resource;

        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("pack.png")) {
            if (stream == null) throw new IOException("Could not find pack.png!");
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
                    MM.deserialize("<rainbow>Color Chase"),
                    Component.newline(),
                    Component.text("by mudkip, Cody, GoldenStack")));

            event.setResponseData(data);
        });

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            LOGGER.info("{} connected", event.getPlayer().getUsername());
            event.setSpawningInstance(lobby.getInstance());
            event.getPlayer().setRespawnPoint(Lobby.SPAWN);
            if (Config.ENABLE_RESOURCE_PACK) {
                event.getPlayer().sendResourcePacks(RESOURCE_PACK_REQUEST);
            }

            event.getPlayer().sendPacket(new ServerLinksPacket(
                    new ServerLinksPacket.Entry(Component.text("mudkip's website"), "https://mudkip.dev"),
                    new ServerLinksPacket.Entry(Component.text("golden's website"), "https://goldenstack.net"),
                    new ServerLinksPacket.Entry(Component.text("cody's website"), "https://codyq.dev")));
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
