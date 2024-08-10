package jam.utility;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;

import java.util.*;
import java.util.function.Consumer;

public record Zone(Point start, Point end) {
    private static final Random RANDOM = new Random();

    public void eachBlock(Consumer<BlockVec> consumer) {
        for (int x = this.start.blockX(); x <= this.end.blockX(); x++) {
            for (int z = this.start.blockZ(); z <= this.end.blockZ(); z++) {
                for (int y = this.start.blockY(); y <= this.end.blockY(); y++) {
                    consumer.accept(new BlockVec(x, y, z));
                }
            }
        }
    }

    public BlockVec randomBlock() {
        List<BlockVec> vectors = new ArrayList<>();
        this.eachBlock(vectors::add);
        return vectors.get(RANDOM.nextInt(vectors.size()));
    }
}
