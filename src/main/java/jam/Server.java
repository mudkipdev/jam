package jam;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

public final class Server implements Config {
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
        });

        if (FORWARDING_SECRET != null) {
            VelocityProxy.enable(FORWARDING_SECRET);
        } else {
            MojangAuth.init();
        }

        server.start(ADDRESS, PORT);
    }
}
