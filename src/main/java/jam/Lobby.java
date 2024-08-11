package jam;

import jam.game.Arena;
import jam.game.Queue;
import jam.utility.SignHandler;
import jam.utility.Sounds;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;

import java.util.Set;

public final class Lobby {
    public static final Pos SPAWN = new Pos(0.5D, 2.0D, 0.5D);
    public static final Set<BlockVec> SIGNS = Set.of(
            new BlockVec(0, 0, -2),
            new BlockVec(-1, 0, -2));

    public static DynamicRegistry.Key<DimensionType> dimension;
    private final InstanceContainer instance;
    private final Queue queue;

    public Lobby() {
        dimension = MinecraftServer.getDimensionTypeRegistry()
                .register("jam:jam", DimensionType.builder()
                        .ambientLight(1.0F)
                        .build());

        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        this.instance.setChunkLoader(Arena.createLoader("lobby"));
        this.instance.setTimeRate(0);
        this.instance.setTime(6000);
        this.queue = new Queue();

        // SCUFFED
        this.instance.eventNode().addListener(InstanceChunkLoadEvent.class, event -> {
            try {
                for (var position : SIGNS) {
                    var block = this.instance.getBlock(position);
                    if (!block.name().contains("sign")) continue;
                    this.instance.setBlock(position, block.withHandler(new SignHandler()));
                }
            } catch (Exception e) {
                // chunk is not loaded yet
            }
        });

        this.instance.eventNode().addListener(PlayerMoveEvent.class, event -> {
            if (event.getNewPosition().y() < -10.0D) {
                event.getPlayer().teleport(SPAWN);
                event.getPlayer().playSound(Sounds.TELEPORT);
            }
        });

        this.instance.eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (!event.getBlock().name().contains("trapdoor")) {
                return;
            }

            var open = Boolean.parseBoolean(event.getBlock().getProperty("open"));

            event.getInstance().setBlock(event.getBlockPosition(), event.getBlock()
                    .withProperty("open", !open ? "true" : "false"));
        });
    }

    public InstanceContainer getInstance() {
        return this.instance;
    }

    public Queue getQueue() {
        return this.queue;
    }
}
