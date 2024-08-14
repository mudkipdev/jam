package jam.utility.constants;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jam.utility.WorldBlock;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author emortal
 */
public interface Sphere {
    static List<WorldBlock> getNearbyBlocks(
            Point point,
            Set<Point> blocksInSphere,
            Instance instance,
            Predicate<Block> predicate) {
        var filteredBlocks = new ArrayList<WorldBlock>();

        for (var block : blocksInSphere) {
            var blockPos = block.add(point);
            Block currentBlock;

            try {
                currentBlock = instance.getBlock(blockPos, Block.Getter.Condition.TYPE);
            } catch (Exception ignored) {
                continue;
            }

            if (!predicate.test(currentBlock)) {
                continue;
            }

            filteredBlocks.add(new WorldBlock(
                    new BlockVec(
                            point.blockX(),
                            point.blockY(),
                            point.blockZ()),
                    currentBlock));
        }

        Collections.shuffle(filteredBlocks);
        return filteredBlocks;
    }

    static Set<Point> getBlocksInSphere(double radius) {
        Set<Point> points = new HashSet<>();

        for (double x = -radius; x <= radius; x++) {
            for (double y = -radius; y <= radius; y++) {
                for (double z = -radius; z <= radius; z++) {
                    if ((x * x) + (y * y) + (z * z) > radius * radius) continue;
                    points.add(new Vec(x, y, z));
                }
            }
        }

        return points;
    }

    static byte[] getSphereExplosionOffsets(double radius) {
        var points = new ByteArrayList();

        for (var x = (byte) -radius; x <= radius; x++) {
            for (var y = (byte) -radius; y <= radius; y++) {
                for (var z = (byte) -radius; z <= radius; z++) {
                    if ((x * x) + (y * y) + (z * z) > radius * radius) {
                        continue;
                    }

                    points.add(x);
                    points.add(y);
                    points.add(z);
                }
            }
        }

        return points.toArray(new byte[0]);
    }

    static @NotNull Set<Point> fibonacciSpherePoints(int points) {
        var positions = new HashSet<Point>();
        var phi = Math.PI * (Math.sqrt(5.0) - 1.0);

        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (points - 1.0)) * 2;
            double yRad = Math.sqrt(1 - y * y);
            double theta = phi * i;

            double x = Math.cos(theta) * yRad;
            double z = Math.sin(theta) * yRad;

            positions.add(new Vec(x, y, z));
        }

        return positions;
    }
}