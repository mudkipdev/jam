package jam.utility;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;

import java.util.concurrent.ThreadLocalRandom;

public record Zone(Point start, Point end) {
    private static int randomNumber(int min, int max) {
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max+1);
    }

    public BlockVec randomBlock() {
        return new BlockVec(
                randomNumber(this.start.blockX(), this.end.blockX()),
                randomNumber(this.start.blockY(), this.end.blockY()),
                randomNumber(this.start.blockZ(), this.end.blockZ()));
    }
}
