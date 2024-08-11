package jam.game;

import jam.Server;
import jam.utility.Zone;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum Arena {
    CITY(
            new Vec(0, 0, 32),
            List.of(new Zone(
                    new BlockVec(-2, 2, 47),
                    new BlockVec(3, 2, 49)
            )),
            List.of(new Zone(
                    new BlockVec(-2, 2, -19),
                    new BlockVec(2, 2, -17)
            ))
    );

    private final Vec center;
    private final List<Zone> runnerSpawns, hunterSpawns;

    Arena(Vec center, List<Zone> runnerSpawns, List<Zone> hunterSpawns) {
        this.center = center;
        this.runnerSpawns = runnerSpawns;
        this.hunterSpawns = hunterSpawns;
    }

    private @NotNull Pos randomPoint(@NotNull List<Zone> zones) {
        Zone zone = zones.get(ThreadLocalRandom.current().nextInt(zones.size()));
        Point point = zone.randomBlock().add(0.5, 0, 0.5);
        // TODO: Facing towards center
        return new Pos(point).withView(0, 0);
    }

    public @NotNull Pos runnerSpawn() {
        return randomPoint(runnerSpawns);
    }

    public @NotNull Pos hunterSpawn() {
        return randomPoint(hunterSpawns);
    }

    public Vec getCenter() {
        return center;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Arena.class);

    public static Arena random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    public IChunkLoader createLoader() {
        try (InputStream stream = Server.class.getResourceAsStream(
                "/" + this.name().toLowerCase() + ".polar")) {
            if (stream == null) {
                throw new IOException("Could not find Polar world.");
            }

            return new PolarLoader(stream);
        } catch (IOException e) {
            LOGGER.error("Failed to load Polar world.", e);
            System.exit(1);
            return null; // W code
        }
    }

    public Instance createArenaInstance() {
        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        var arena = Arena.random();

        instance.setChunkSupplier(LightingChunk::new);
        instance.setChunkLoader(arena.createLoader());
        instance.setTimeRate(0);
        instance.setTime(18000);

        var easterEgg = new Entity(EntityType.TEXT_DISPLAY);
        easterEgg.setNoGravity(true);

        easterEgg.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setText(Component.text("You just lost the game"));
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setScale(new Vec(10));
            meta.setBackgroundColor(0);
        });

        easterEgg.setInstance(instance, getCenter().add(0, 50, 0));
        return instance;
    }
}
