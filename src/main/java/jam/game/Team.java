package jam.game;

import jam.utility.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;

import java.util.Random;
import java.util.Set;

public record Team(
        Game game,
        net.minestom.server.scoreboard.Team scoreboard,
        Set<Player> players,
        Color color,
        Zone spawn
) {
    public static final Zone NORTH_SPAWN = new Zone(
            new BlockVec(-2, 1, -19),
            new BlockVec(2, 1, -17));

    public static final Zone SOUTH_SPAWN = new Zone(
            new BlockVec(3, 1, 49),
            new BlockVec(-2, 1, 47));

    private static final Random RANDOM = new Random();

    public Team(Game game, Set<Player> players, Color color, Zone spawn) {
        this(game, MinecraftServer.getTeamManager().createTeam(
                color.name(),
                Component.text(color.getTitle(), color.getTextColor()),
                Component.text(color.name().charAt(0), color.getTextColor(), TextDecoration.BOLD).appendSpace(),
                color.getTextColor(),
                Component.empty()), players, color, spawn);

    }

    @Override
    public String toString() {
        return this.color.getTitle();
    }

    public Pos pickRandomSpawn() {
        return new Pos(this.spawn.randomBlock())
                .add(0.0D, 1.5D, 0.0D) // make them spawn on top of the block
                .withYaw(this.spawn.equals(NORTH_SPAWN) ? 0.0F : 180.0F); // make teams face each other
    }

    public Component getTitle() {
        return Component.text(this.color.getTitle(), this.color.getTextColor());
    }

    public enum Color {
        RED,
        ORANGE,
        YELLOW,
        GREEN,
        BLUE,
        PINK;

        public static Color random() {
            return values()[RANDOM.nextInt(values().length)];
        }

        public String getTitle() {
            String[] words = this.name().split("_");
            StringBuilder builder = new StringBuilder();

            for (String word : words) {
                builder
                        .append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }

            return builder.toString().trim();
        }

        public NamedTextColor getTextColor() {
            return switch (this) {
                case RED -> NamedTextColor.RED;
                case ORANGE -> NamedTextColor.GOLD;
                case YELLOW -> NamedTextColor.YELLOW;
                case GREEN -> NamedTextColor.GREEN;
                case BLUE -> NamedTextColor.AQUA;
                case PINK -> NamedTextColor.LIGHT_PURPLE;
            };
        }

        public DyedItemColor getDyeColor() {
            return switch (this) {
                case RED -> new DyedItemColor(11546150);
                case ORANGE -> new DyedItemColor(16351261);
                case YELLOW -> new DyedItemColor(16701501);
                case GREEN -> new DyedItemColor(8439583);
                case BLUE -> new DyedItemColor(3847130);
                case PINK -> new DyedItemColor(13061821);
            };
        }

        public Block getInkBlock() {
            return switch (this) {
                case RED -> Block.RED_CONCRETE;
                case ORANGE -> Block.ORANGE_CONCRETE;
                case YELLOW -> Block.YELLOW_CONCRETE;
                case GREEN -> Block.LIME_CONCRETE;
                case BLUE -> Block.LIGHT_BLUE_CONCRETE;
                case PINK -> Block.PINK_CONCRETE;
            };
        }

        public Material getInkDye() {
            return switch (this) {
                case RED -> Material.RED_DYE;
                case ORANGE -> Material.ORANGE_DYE;
                case YELLOW -> Material.YELLOW_DYE;
                case GREEN -> Material.LIME_DYE;
                case BLUE -> Material.LIGHT_BLUE_DYE;
                case PINK -> Material.PINK_DYE;
            };
        }
        public ItemStack getInkBlaster() {
            return ItemStack.of(switch (this) {
                case RED -> Material.RED_CANDLE;
                case ORANGE -> Material.ORANGE_CANDLE;
                case YELLOW -> Material.YELLOW_CANDLE;
                case GREEN -> Material.LIME_CANDLE;
                case BLUE -> Material.LIGHT_BLUE_CANDLE;
                case PINK -> Material.PINK_CANDLE;
            }).with(ItemComponent.ITEM_NAME, Component.text("Ink Blaster"));
        };

        public Color getComplementaryColor() {
            return switch (this) {
                case RED -> GREEN;
                case ORANGE -> BLUE;
                case YELLOW -> PINK;
                case GREEN -> RED;
                case BLUE -> ORANGE;
                case PINK -> YELLOW;
            };
        }
    }
}
