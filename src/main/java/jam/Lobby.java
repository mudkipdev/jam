package jam;

import jam.game.Queue;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;

public final class Lobby {
    private final Instance instance;
    private final Queue queue;

    public Lobby() {
        DynamicRegistry.Key<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry()
                .register("jam:lobby", DimensionType.builder()
                        .ambientLight(1.0F)
                        .effects("minecraft:the_end")
                        .build());

        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        this.queue = new Queue();
    }

    public Instance getInstance() {
        return this.instance;
    }

    public Queue getQueue() {
        return this.queue;
    }
}
