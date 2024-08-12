package jam.game;

import jam.utility.Tags;
import jam.utility.Titleable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.predicate.BlockPredicate;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BlockPredicates;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

public enum JamColor implements Titleable {
    RED(new BlockInfo(Block.RED_CONCRETE, Block.CRIMSON_STAIRS, Block.CRIMSON_SLAB, Block.CRIMSON_TRAPDOOR, Block.CRIMSON_FENCE, Block.CRIMSON_FENCE_GATE, Block.CRIMSON_PRESSURE_PLATE, Block.CRIMSON_BUTTON, Block.TUFF_WALL, Block.RED_WOOL)), // Crimson (tuff wall)
    ORANGE(new BlockInfo(Block.ORANGE_CONCRETE, Block.ACACIA_STAIRS, Block.ACACIA_SLAB, Block.ACACIA_TRAPDOOR, Block.ACACIA_FENCE, Block.ACACIA_FENCE_GATE, Block.ACACIA_PRESSURE_PLATE, Block.ACACIA_BUTTON, Block.POLISHED_TUFF_WALL, Block.ORANGE_WOOL)), // Acacia (polished_tuff wall)
    YELLOW(new BlockInfo(Block.YELLOW_CONCRETE, Block.MANGROVE_STAIRS, Block.MANGROVE_SLAB, Block.MANGROVE_TRAPDOOR, Block.MANGROVE_FENCE, Block.MANGROVE_FENCE_GATE, Block.MANGROVE_PRESSURE_PLATE, Block.MANGROVE_BUTTON, Block.TUFF_BRICK_WALL, Block.YELLOW_WOOL)), // Mangrove (tuff brick wall)
    GREEN(new BlockInfo(Block.LIME_CONCRETE, Block.DARK_OAK_STAIRS, Block.DARK_OAK_SLAB, Block.DARK_OAK_TRAPDOOR, Block.DARK_OAK_FENCE, Block.DARK_OAK_FENCE_GATE, Block.DARK_OAK_PRESSURE_PLATE, Block.DARK_OAK_BUTTON, Block.END_STONE_BRICK_WALL, Block.GREEN_WOOL)), // Dark oak (end_stone_brick wall)
    BLUE(new BlockInfo(Block.LIGHT_BLUE_CONCRETE, Block.WARPED_STAIRS, Block.WARPED_SLAB, Block.WARPED_TRAPDOOR, Block.WARPED_FENCE, Block.WARPED_FENCE_GATE, Block.WARPED_PRESSURE_PLATE, Block.WARPED_BUTTON, Block.RED_NETHER_BRICK_WALL, Block.LIGHT_BLUE_WOOL)), // Warped (red_nether_brick wall)
    PINK(new BlockInfo(Block.PINK_CONCRETE, Block.CHERRY_STAIRS, Block.CHERRY_SLAB, Block.CHERRY_TRAPDOOR, Block.CHERRY_FENCE, Block.CHERRY_FENCE_GATE, Block.CHERRY_PRESSURE_PLATE, Block.CHERRY_BUTTON, Block.PRISMARINE_WALL, Block.PINK_WOOL)); // Cherry (prismarine wall)

    private final BlockInfo blockInfo;

    JamColor(BlockInfo blockInfo) {
        this.blockInfo = blockInfo;
    }

    public record BlockInfo(Block solid, Block stairs, Block slab, Block trapdoor, Block fence,
                            Block fenceGate, Block pressurePlate, Block button, Block wall, Block wool) {

        private boolean contains(Block block) {
            // i love boilerplate
            int id = block.id();
            return id == solid.id()
                    || id == stairs.id()
                    || id == slab.id()
                    || id == trapdoor.id()
                    || id == fence.id()
                    || id == fenceGate.id()
                    || id == pressurePlate.id()
                    || id == button.id()
                    || id == wall.id();
        }
    }

    private static @NotNull Map.Entry<Predicate<NamespaceID>, Function<BlockInfo, Block>> make(@NotNull String tagName, @NotNull Function<BlockInfo, Block> getter) {
        return Map.entry(
                MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, "minecraft:" + tagName).getValues()::contains,
                getter
        );
    }

    private static final @NotNull Map<Predicate<NamespaceID>, Function<BlockInfo, Block>> MAPS = Map.ofEntries(
            make("stairs", BlockInfo::stairs),
            make("slabs", BlockInfo::slab),
            make("trapdoors", BlockInfo::trapdoor),
            make("fences", BlockInfo::fence),
            make("fence_gates", BlockInfo::fenceGate),
            make("pressure_plates", BlockInfo::pressurePlate),
            make("buttons", BlockInfo::button),
            make("walls", BlockInfo::wall),
            make("wool", BlockInfo::wool)
    );

    public static JamColor colorOfBlock(Block block) {
        for (var color : values()) {
            if (color.blockInfo.contains(block)) {
                return color;
            }
        }
        return null;
    }

    public static JamColor random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    public Block convertBlockMaterial(Block block) {
        if (!block.isSolid() || block == Block.BARRIER) {
            return block;
        }

        for (var entry : MAPS.entrySet()) {
            if (entry.getKey().test(block.namespace())) {
                var updated = entry.getValue().apply(this.blockInfo);

                if (!block.properties().isEmpty()) {
                    updated = updated.withProperties(block.properties());
                }

                return updated;
            }
        }

        return blockInfo.solid;
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
        }).with(ItemComponent.ITEM_NAME, Component.text("Ink Blaster", NamedTextColor.YELLOW))
                .withTag(Tags.EFFECT, Effect.INK_BLASTER);
    }

    public ItemStack getTnt() {
        return ItemStack.of(Material.TNT)
                .with(ItemComponent.ITEM_NAME, Component.text(title(), getTextColor()).append(Component.text(" TNT", NamedTextColor.RED)))
                .withTag(Tags.EFFECT, Effect.TNT)
                .withTag(Tags.COLOR, this)
                .with(ItemComponent.CAN_PLACE_ON, new BlockPredicates(BlockPredicate.ALL));
    }
}
