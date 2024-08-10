package jam.game;

import jam.Server;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Game {
    public static final @NotNull Tag<Game> TAG = Tag.Transient("Game");

    public static final int GRACE_PERIOD = 15; // Seconds

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private final Instance instance;

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    private @Nullable Task gameStartTask;

    private final AtomicBoolean ending = new AtomicBoolean(false);

    public Game() {
        this.instance = createArenaInstance();
    }

    private static Instance createArenaInstance() {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();
        Arena arena = Arena.random();

        instance.setChunkSupplier(LightingChunk::new);
        instance.setChunkLoader(arena.createLoader());
        instance.setTimeRate(0);
        instance.setTime(6000);

        return instance;
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void spawnPlayers(@NotNull Collection<Player> players) {
        int hunters = (int) Math.ceil(players.size() / 3.0);

        List<Player> initial = new ArrayList<>(players);

        // Init hunters
        for (int i = 0; i < hunters; i++) {
            int index = ThreadLocalRandom.current().nextInt(initial.size());

            Player player = initial.remove(index);
            player.setTag(Team.TAG, Team.HUNTER);

            this.hunters.add(player.getUuid());
        }

        // Init runners
        for (Player player : initial) {
            player.setTag(Team.TAG, Team.RUNNER);
            this.runners.add(player.getUuid());

            player.updateViewableRule(other -> other.getTag(Team.TAG) == Team.RUNNER);
        }

        AtomicInteger timer = new AtomicInteger(GRACE_PERIOD);
        this.gameStartTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            int remaining = timer.getAndDecrement();
            if (remaining == 0) {
                this.gameStartTask.cancel();
                this.gameStartTask = null;

                for (Player player : instance.getPlayers()) {
                    switch (player.getTag(Team.TAG)) {
                        case RUNNER -> player.updateViewableRule(null);
                        case HUNTER -> {
                            player.showTitle(Title.title(
                                    Component.textOfChildren(
                                            Component.text("The hunt begins!", NamedTextColor.RED)
                                    ),
                                    Component.textOfChildren(
                                            Component.text("Your "),
                                            Component.text("grace period", NamedTextColor.YELLOW),
                                            Component.text(" is over. "),
                                            Component.text("Hunt", NamedTextColor.RED),
                                            Component.text(" and "),
                                            Component.text("eliminate", NamedTextColor.RED),
                                            Component.text("the runners.")
                                    )
                            ));
                        }
                    }
                }
                return;
            }

            for (Player player : instance.getPlayers()) {
                if (player.getTag(Team.TAG) != Team.HUNTER) continue;

                player.showTitle(Title.title(
                        Component.textOfChildren(
                                Component.text(remaining, NamedTextColor.RED),
                                Component.text(" second" + (remaining == 1 ? "" : "s") + " left")
                        ),
                        Component.textOfChildren(
                                Component.text("of your "),
                                Component.text("grace period", NamedTextColor.YELLOW),
                                Component.text("!")
                        )
                ));
            }

        }).repeat(Duration.of(1, ChronoUnit.SECONDS)).schedule();

        for (Player player : players) {
            player.setTag(TAG, this);
            player.setInstance(instance);
            player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
            player.setGameMode(GameMode.CREATIVE);
            player.teleport(new Pos(0, 1, 0));
            player.setInvisible(false);

            switch (player.getTag(Team.TAG)) {
                case HUNTER -> {
                    Component title = Component.text("You are a ")
                            .append(Component.text("hunter", NamedTextColor.RED))
                            .append(Component.text("!"));

                    player.showTitle(Title.title(title, Component.text("Tag and eliminate each runner!")));
                }
                case RUNNER -> {
                    Component title = Component.text("You are a ")
                            .append(Component.text("runner", NamedTextColor.GREEN))
                            .append(Component.text("!"));

                    player.showTitle(Title.title(title, Component.text("Avoid the hunters until the time runs out!")));
                }
            }
        }
    }

    public void handlePlayerAttack(@NotNull Player attacker, @NotNull Player target) {
        if (ending.get()) return;

        Team attackerTeam = attacker.getTag(Team.TAG);
        Team targetTeam = target.getTag(Team.TAG);
        if (attackerTeam == null || targetTeam == null || attackerTeam == targetTeam) return;

        if (attackerTeam == Team.HUNTER && targetTeam == Team.RUNNER) {
            target.removeTag(Team.TAG);
            runners.remove(target.getUuid());
            target.setGameMode(GameMode.SPECTATOR);
            target.setInvisible(true);

            if (runners.isEmpty()) {
                handleGameEnd(Team.HUNTER);
            }

            Component message = Component.textOfChildren(
                    Component.text(target.getUsername(), NamedTextColor.GREEN),
                    Component.text(" was tagged by ", NamedTextColor.YELLOW),
                    Component.text(attacker.getUsername(), NamedTextColor.RED),
                    Component.text("!", NamedTextColor.YELLOW),
                    Component.text(" There are ", NamedTextColor.GRAY),
                    Component.text(runners.size(), NamedTextColor.YELLOW),
                    Component.text(" runners remaining.", NamedTextColor.GRAY)
            );

            instance.sendMessage(message);
        }
    }

    public void handleGameEnd(@NotNull Team winner) {
        if (ending.getAndSet(true)) return;

        switch (winner) {
            case HUNTER -> {
                instance.showTitle(Title.title(
                        Component.text("Hunters have won!", NamedTextColor.RED),
                        Component.text("Every runner has been eliminated.")
                ));
            }
            case RUNNER -> {
                instance.showTitle(Title.title(
                        Component.text("Runners have won!", NamedTextColor.GREEN),
                        Component.text("The runners have evaded the hunters.")
                ));
            }
        }

        for (Player player : instance.getPlayers()) {
            player.removeTag(Game.TAG);
            player.removeTag(Team.TAG);
            player.setInvisible(false);
        }

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            for (Player player : instance.getPlayers()) {
                player.setInstance(Server.getLobby().getInstance());
            }
        }).delay(Duration.of(10, TimeUnit.SECOND)).schedule();
    }
}
