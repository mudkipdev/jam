package jam.utility;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;

/**
 * @author emortal
 */
public record WorldBlock(BlockVec position, Block block) {

}
