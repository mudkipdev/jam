package jam.game;

import jam.utility.Titleable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;

import java.util.concurrent.ThreadLocalRandom;

public enum JamColor implements Titleable {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    PINK;

    public static JamColor random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    public Block convertBlockMaterial(Block block) {
        return switch (this) {
            case RED -> Block.RED_CONCRETE;
            case ORANGE -> Block.ORANGE_CONCRETE;
            case YELLOW -> Block.YELLOW_CONCRETE;
            case GREEN -> Block.LIME_CONCRETE;
            case BLUE -> Block.LIGHT_BLUE_CONCRETE;
            case PINK -> Block.PINK_CONCRETE;
        };
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
    }
}
