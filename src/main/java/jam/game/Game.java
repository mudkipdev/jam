package jam.game;

import jam.Server;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Game {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private final Instance instance;

    public Game() {
        this.instance = createArenaInstance();
    }

    private static Instance createArenaInstance() {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();
        Arena arena = Arena.random();

        instance.setChunkSupplier(LightingChunk::new);
        instance.setChunkLoader(arena.createLoader());
        instance.setTimeRate(0);
        instance.setTime(6000);

        return instance;
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void spawnPlayer(Player player) {
        player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
        player.setGameMode(GameMode.ADVENTURE);
    }
}
