package jam.game;

import jam.Lobby;
import jam.Server;
import jam.utility.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class Queue implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Queue.class);
    private static final int WAIT_TIME = 60;
    private static final int MINIMUM_PLAYERS = 2;
    private static final int MAXIMUM_PLAYERS = 8;

    private final Set<Player> players;
    private @Nullable Task countdownTask;
    private @Nullable AtomicInteger countdown;

    public Queue() {
        this.players = new HashSet<>();
    }

    @Override
    public @NotNull Set<Player> getPlayers() {
        return this.players;
    }

    // this code is sponsored by my server
    public void addPlayer(Player player) {
        if (this.players.contains(player)) {
            return;
        }

        player.teleport(Lobby.SPAWN);

        this.players.add(player);
        this.countdown = new AtomicInteger(WAIT_TIME);

        this.sendMessage(Component.textOfChildren(
                Component.text("+ " + player.getUsername(), NamedTextColor.GREEN),
                Component.text(" (" + this.players.size() + "/" + MINIMUM_PLAYERS + ")", NamedTextColor.GRAY)));

        if (this.players.size() >= MINIMUM_PLAYERS && this.countdownTask == null) {
            this.countdownTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
                int time = this.countdown.get();

                if (time == 0) {
                    this.start();
                }

                if (time % 10 == 0) {
                    this.playSound(Sounds.CLICK);
                    sendMessage(Server.MM.deserialize("<prefix>Starting in <white>" + time + "<gray> seconds!"));
                }

                if (time <= 5) {
                    this.playSound(Sounds.CLICK);
                    this.sendTitle(Component.textOfChildren(
                            Component.text("Starting in ", NamedTextColor.GRAY),
                            Component.text(time, NamedTextColor.WHITE),
                            Component.text(" seconds", NamedTextColor.GRAY)));
                }

                this.countdown.getAndDecrement();
            }).repeat(1, TimeUnit.SECOND).schedule();
        }
    }

    public void removePlayer(Player player) {
        this.players.remove(player);

        this.sendMessage(Component.textOfChildren(
                Component.text(
                        "- " + player.getUsername(),
                        NamedTextColor.RED),

                Component.text(
                        " (" + this.players.size() + "/" + MINIMUM_PLAYERS + ")",
                        NamedTextColor.GRAY)));

        if (this.players.size() < MINIMUM_PLAYERS && this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;

            this.playSound(Sounds.CLICK);
            this.sendTitle(Component.text(
                    "Not enough players!",
                    NamedTextColor.RED));
        }
    }

    public void start() {
        LOGGER.info("Starting the game with {} players in queue.", this.players.size());
        this.clearTitle();

        Set<Player> finalPlayers = this.players.stream()
                .limit(MAXIMUM_PLAYERS)
                .collect(Collectors.toSet());
        Game game = new Game(finalPlayers);
        game.beginNextRound();
        this.players.removeAll(finalPlayers);
    }

    private void sendTitle(Component component) {
        this.sendTitlePart(TitlePart.TITLE, Component.empty());
        this.sendTitlePart(TitlePart.SUBTITLE, component);
    }
}
