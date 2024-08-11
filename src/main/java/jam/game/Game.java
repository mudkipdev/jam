package jam.game;

import jam.Config;
import jam.Server;
import jam.utility.Tags;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Game implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final int GRACE_PERIOD = 15;
    private static final int TIMER = 100 + GRACE_PERIOD;

    private final Arena arena;
    private final Instance instance;
    private final AtomicInteger timer = new AtomicInteger(TIMER);
    private final BossBar bossBar;

    // Stored as UUIDs to prevent potential memory leaks
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    private @Nullable Task gameStartTask;

    public Game() {
        this.arena = Arena.random();
        this.instance = arena.createArenaInstance();
        this.bossBar = BossBar.bossBar(
                Component.text(this.timer.get() + " seconds left"),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (timer.get() == 0) {
                        bossBar.removeViewer(this);

                        MinecraftServer.getSchedulerManager().buildTask(() -> {
                            this.getPlayers().forEach(this::despawnPlayer);
                        }).delay(10, TimeUnit.SECOND).schedule();

                        return;
                    }

                    var grace = this.timer.get() > (TIMER - GRACE_PERIOD);
                    var total = grace ? GRACE_PERIOD : TIMER;
                    var current = grace ? this.timer.get() - TIMER : this.timer.get();

                    if (grace) {
                        bossBar.name(Component.text(current + " seconds left"));
                        bossBar.color(current < 20 ? BossBar.Color.RED : BossBar.Color.GREEN);
                    } else {
                        bossBar.name(Component.text(current + " seconds left (grace period)"));
                        bossBar.color(BossBar.Color.BLUE);
                    }

                    bossBar.progress((float) current / total);
                    this.timer.decrementAndGet();
                })
                .repeat(1, TimeUnit.SECOND)
                .schedule();
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.getInstance().getPlayers();
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void spawnPlayers(Collection<Player> players) {
        for (Player player : players) {
            player.setTag(Tags.GAME, this);
            player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
            player.setGameMode(Config.DEBUG ? GameMode.CREATIVE : GameMode.ADVENTURE);
            player.setInvisible(false);
        }

        int hunters = (int) Math.ceil(players.size() / 3.0);

        List<Player> initial = new ArrayList<>(players);

        // Init hunters
        for (int i = 0; i < hunters; i++) {
            int index = ThreadLocalRandom.current().nextInt(initial.size());

            Player player = initial.remove(index);
            player.setTag(Tags.TEAM, Team.HUNTER);

            this.hunters.add(player.getUuid());

            player.setInstance(instance, arena.hunterSpawn());
            player.addEffect(new Potion(PotionEffect.BLINDNESS, (byte) 0, (GRACE_PERIOD + 1) * 20, 0));
        }

        // Init runners
        for (Player player : initial) {
            player.setTag(Tags.TEAM, Team.RUNNER);
            this.runners.add(player.getUuid());

            player.updateViewableRule(other -> other.getTag(Tags.TEAM) == Team.RUNNER);

            player.showTitle(Title.title(
                    Server.MINI_MESSAGE.deserialize(
                            "You are a <green>runner<gray>!"
                    ),
                    Component.text("Avoid the hunters until the time runs out!")
            ));

            player.setInstance(instance, arena.runnerSpawn());
        }

        AtomicInteger timer = new AtomicInteger(GRACE_PERIOD);
        this.gameStartTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            int remaining = timer.getAndDecrement();
            if (remaining == 0) {
                this.gameStartTask.cancel();
                this.gameStartTask = null;

                for (Player player : instance.getPlayers()) {
                    switch (player.getTag(Tags.TEAM)) {
                        case RUNNER -> player.updateViewableRule(null);
                        case HUNTER -> {
                            player.showTitle(Title.title(
                                    Component.text("The hunt begins!", NamedTextColor.RED),
                                    Server.MINI_MESSAGE.deserialize(
                                            "<gray>The <yellow>grace period<gray> is over. <red>Hunt<gray> and <red>eliminate<gray> the runners."
                                    ),
                                    Title.Times.times(
                                            Duration.ZERO,
                                            Duration.ofMillis(1500),
                                            Duration.ofMillis(1000)
                                    )
                            ));
                        }
                    }
                }
                return;
            }

            for (Player player : instance.getPlayers()) {
                if (player.getTag(Tags.TEAM) != Team.HUNTER) continue;

                player.showTitle(Title.title(
                        Component.textOfChildren(
                                Component.text(remaining, NamedTextColor.RED),
                                Component.text(" second" + (remaining == 1 ? "" : "s") + " left")
                        ),
                        Component.textOfChildren(
                                Component.text("of the "),
                                Component.text("grace period", NamedTextColor.YELLOW),
                                Component.text("!")
                        )
                ));
            }

        }).repeat(Duration.of(1, TimeUnit.SECOND)).schedule();

//        TODO: Implement colors
//        var color = JamColor.GREEN;
//        player.setTag(Tags.COLOR, color);
//
//        player.getInventory().setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
//                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));
//
//        player.getInventory().setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS)
//                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));
//
//        player.getInventory().setBoots(ItemStack.of(Material.LEATHER_BOOTS)
//                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));
//
//        bossBar.addViewer(player);
    }

    public void despawnPlayer(Player player) {
        player.removeTag(Tags.GAME);
        player.removeTag(Tags.TEAM);
        player.removeTag(Tags.COLOR);
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInstance(Server.getLobby().getInstance());
    }
}
