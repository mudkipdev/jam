package jam;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public final class Server implements Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        InstanceContainer instance = instanceManager.createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setTimeRate(0);
        instance.setTime(6000);
        instance.setGenerator(unit ->
                unit.modifier().fillHeight(-64, 0, Block.STONE));

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            LOGGER.info("{} connected", event.getPlayer().getUsername());
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event ->
                LOGGER.info("{} disconnected", event.getPlayer().getUsername()));

        if (SECRETS.length != 0) {
            BungeeCordProxy.setBungeeGuardTokens(Set.of(SECRETS));
            BungeeCordProxy.enable();
            LOGGER.info("Enabled BungeeCord forwarding.");
        } else {
            MojangAuth.init();
            LOGGER.info("Enabled Mojang authentication.");
        }

        LOGGER.info("Starting server on {}:{}.", ADDRESS, PORT);
        server.start(ADDRESS, PORT);
    }
}
