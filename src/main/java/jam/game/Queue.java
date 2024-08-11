package jam.game;

import jam.Config;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
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
    private static final int WAIT_TIME = Config.DEBUG ? 0 : 30;
    private static final int MINIMUM_PLAYERS = Config.DEBUG ? 1 : 2;
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

        this.players.add(player);
        this.countdown = new AtomicInteger(WAIT_TIME);

        this.sendMessage(Component.textOfChildren(
                Component.text(
                        "+ " + player.getUsername(),
                        NamedTextColor.GREEN),

                Component.text(
                        " (" + this.players.size() + "/" + MINIMUM_PLAYERS + ")",
                        NamedTextColor.GRAY)));

        if (this.players.size() >= MINIMUM_PLAYERS && this.countdownTask == null) {
            this.countdownTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
                if (this.countdown.get() == 0) {
                    LOGGER.info("Starting the game with {} players in queue.", this.players.size());
                    this.clearTitle();

                    Game game = new Game();
                    Set<Player> finalPlayers = this.players.stream()
                            .limit(MAXIMUM_PLAYERS)
                            .collect(Collectors.toSet());

                    this.players.removeAll(finalPlayers);
                    finalPlayers.forEach(it -> {
                        it.setInstance(game.getInstance());
                        game.spawnPlayer(it);
                    });
                }

                this.playClickSound();
                this.sendTitle(Component.textOfChildren(
                        Component.text("Starting in ", NamedTextColor.GRAY),
                        Component.text(this.countdown.getAndDecrement(), NamedTextColor.WHITE),
                        Component.text(" seconds", NamedTextColor.GRAY)));
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

            this.playClickSound();
            this.sendTitle(Component.text(
                    "Not enough players!",
                    NamedTextColor.RED));
        }
    }

    private void playClickSound() {
        this.playSound(Sound.sound(
                SoundEvent.UI_BUTTON_CLICK.key(),
                Sound.Source.MASTER,
                0.5F, 1.2F));
    }

    private void sendTitle(Component component) {
        this.sendTitlePart(TitlePart.TITLE, Component.empty());
        this.sendTitlePart(TitlePart.SUBTITLE, component);
    }
}
