package jam.game;

import jam.utility.Zone;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public enum Team {
    BASED(new Zone(
            new BlockVec(-2, 1, -19), //start
            new BlockVec(2, 1, -17))), // end

    CRINGE(new Zone(
            new BlockVec(3, 1, 49), // start
            new BlockVec(-2, 1, 47))); // end

    private final Zone spawn;

    Team(Zone spawn) {
        this.spawn = spawn;
    }

    public Pos pickRandomSpawn() {
        return new Pos(this.spawn.randomBlock())
                .add(0.0D, 1.5D, 0.0D) // make them spawn on top of the block
                .withYaw(this == BASED ? 0.0F : 180.0F); // make teams face each other
    }

    // changes the concrete spawn platform's color
    public void changeSpawnColor(Instance instance, Color color) {
        this.spawn.eachBlock(vector ->
                instance.setBlock(vector, color.getInkBlock()));
    }

    public enum Color {
        RED(NamedTextColor.RED),
        ORANGE(NamedTextColor.GOLD),
        YELLOW(NamedTextColor.YELLOW),
        GREEN(NamedTextColor.GREEN),
        BLUE(NamedTextColor.AQUA),
        PURPLE(NamedTextColor.LIGHT_PURPLE);

        private final TextColor textColor;

        Color(TextColor textColor) {
            this.textColor = textColor;
        }

        public TextColor getTextColor() {
            return this.textColor;
        }

        public Block getInkBlock() {
            return switch (this) {
                case RED -> Block.RED_CONCRETE;
                case ORANGE -> Block.ORANGE_CONCRETE;
                case YELLOW -> Block.YELLOW_CONCRETE;
                case GREEN -> Block.GREEN_CONCRETE;
                case BLUE -> Block.LIGHT_BLUE_CONCRETE;
                case PURPLE -> Block.PINK_CONCRETE;
            };
        }

        public Color getComplementaryColor() {
            return switch (this) {
                case RED -> GREEN;
                case ORANGE -> BLUE;
                case YELLOW -> PURPLE;
                case GREEN -> RED;
                case BLUE -> ORANGE;
                case PURPLE -> YELLOW;
            };
        }
    }
}
