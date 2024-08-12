package jam;

import jam.game.Arena;
import jam.game.Queue;
import jam.utility.SignHandler;
import jam.utility.Sounds;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.WrittenBookContent;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class Lobby implements PacketGroupingAudience {
    public static final Pos SPAWN = new Pos(0.5D, 2.0D, 0.5D);
    public static final Set<BlockVec> SIGNS = Set.of(
            new BlockVec(0, 0, -2),
            new BlockVec(-1, 0, -2));

    public static DynamicRegistry.Key<DimensionType> dimension;
    private final InstanceContainer instance;
    private final Queue queue;

    public Lobby() {
        dimension = MinecraftServer.getDimensionTypeRegistry()
                .register("jam:jam", DimensionType.builder()
                        .ambientLight(1.0F)
                        .build());

        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        this.instance.setChunkLoader(Arena.createLoader("lobby"));
        this.instance.setTimeRate(0);
        this.instance.setTime(6000);
        this.queue = new Queue();

        // SCUFFED
        this.instance.eventNode().addListener(InstanceChunkLoadEvent.class, event -> {
            try {
                for (var position : SIGNS) {
                    var block = this.instance.getBlock(position);
                    if (!block.name().contains("sign")) continue;
                    this.instance.setBlock(position, block.withHandler(new SignHandler()));
                }
            } catch (Exception e) {
                // chunk is not loaded yet
            }
        });

        this.instance.eventNode().addListener(PlayerMoveEvent.class, event -> {
            if (event.getNewPosition().y() < -10.0D) {
                event.getPlayer().teleport(SPAWN);
                event.getPlayer().playSound(Sounds.TELEPORT);
            }
        });

        var trapdoors = MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, "minecraft:trapdoors");
        this.instance.eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (!trapdoors.contains(event.getBlock().namespace())) {
                return;
            }

            var open = Boolean.parseBoolean(event.getBlock().getProperty("open"));

            event.getInstance().setBlock(event.getBlockPosition(), event.getBlock()
                    .withProperty("open", !open ? "true" : "false"));
        });

        this.instance.eventNode().addListener(AddEntityToInstanceEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                player.getInventory().setItemStack(0, ItemStack.of(Material.WRITTEN_BOOK)
                        .with(ItemComponent.ITEM_NAME, Component.text("How to Play", NamedTextColor.GREEN))
                        .with(ItemComponent.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                                List.of(Component.empty()),
                                "How to Play",
                                "mudkip")));
            }
        });

        this.instance.eventNode().addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                player.getInventory().clear();
            }
        });

        this.instance.eventNode().addListener(PlayerUseItemEvent.class, event -> {
            if (!event.getItemStack().material().equals(Material.WRITTEN_BOOK)) {
                return;
            }

            event.getPlayer().openBook(Book.book(
                    Component.text("How to Play"),
                    Component.text("mudkip"),
                    Config.INSTRUCTIONS));
        });
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.instance.getPlayers();
    }

    public InstanceContainer getInstance() {
        return this.instance;
    }

    public Queue getQueue() {
        return this.queue;
    }
}
