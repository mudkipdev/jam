package jam.game;

import jam.Lobby;
import jam.utility.Titleable;
import jam.utility.Zone;
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
import net.minestom.server.instance.anvil.AnvilLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum Arena implements Titleable {
    LONDON(
            new Vec(0, 0, 17),
            new Zone(
                    new BlockVec(-18, 2, -1),
                    new BlockVec(19, 2, 36)
            ),
            List.of(new Zone(
                            new BlockVec(10, 1, -1),
                            new BlockVec(13, 1, 1)
                    ), new Zone(
                            new BlockVec(-1, 4, 12),
                            new BlockVec(2, 4, 13)
                    ), new Zone(
                            new BlockVec(-12, 1, 0),
                            new BlockVec(-9, 1, 4)
                    ), new Zone(
                            new BlockVec(7, 1, 26),
                            new BlockVec(11, 1, 30)
                    ), new Zone(
                            new BlockVec(9, 1, 11),
                            new BlockVec(11, 1, 14)
                    ), new Zone(
                            new BlockVec(-13, 1, 11),
                            new BlockVec(-9, 1, 15)
                    )
            ),
            List.of(new Zone(
                    new BlockVec(-11, 2, 28),
                    new BlockVec(-11, 2, 28)
            ))
    );

    private final Vec center;
    private final Zone effectSpawnZone;
    private final List<Zone> runnerSpawns, hunterSpawns;

    Arena(Vec center, Zone effectSpawnZone, List<Zone> runnerSpawns, List<Zone> hunterSpawns) {
        this.center = center;
        this.effectSpawnZone = effectSpawnZone;
        this.runnerSpawns = runnerSpawns;
        this.hunterSpawns = hunterSpawns;
    }

    private @NotNull Pos randomPoint(@NotNull List<Zone> zones) {
        Zone zone = zones.get(ThreadLocalRandom.current().nextInt(zones.size()));
        Point point = zone.randomBlock().add(0.5, 0, 0.5);

        Point diff = point.sub(center);
        return new Pos(point).withView(90 + (float) Math.toDegrees(Math.atan2(diff.z(), diff.x())), 0);
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

    public static IChunkLoader createLoader(String world) {
        try {
            return new AnvilLoader(Path.of(world));
        } catch (Exception e) { // bad practice, but whatever
            LOGGER.error("Failed to load Anvil world.", e);
            System.exit(1);
            return null; // W code
        }
    }

    public Instance createArenaInstance() {
        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer(Lobby.dimension);
        var arena = Arena.random();

        instance.setChunkLoader(Arena.createLoader(arena.name().toLowerCase()));
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

    public BlockVec pickEffectSpawn() {
        return this.effectSpawnZone.randomBlock();
    }
}
