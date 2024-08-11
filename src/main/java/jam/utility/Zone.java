package jam.utility;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public record Zone(Point start, Point end) {
    private static int randomNumber(int min, int max) {
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max);
    }

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
        return new BlockVec(
                randomNumber(this.start.blockX(), this.end.blockX()),
                randomNumber(this.start.blockY(), this.end.blockY()),
                randomNumber(this.start.blockZ(), this.end.blockZ()));
    }
}
