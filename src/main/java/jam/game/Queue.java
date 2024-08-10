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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Queue implements PacketGroupingAudience {
    private static final int WAIT_TIME = Config.DEBUG ? 10 : 30;
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
        this.players.add(player);
        this.countdown = new AtomicInteger(WAIT_TIME);

        if (this.countdownTask == null) {
            this.countdownTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
                if (this.countdown.get() == 0) {
                    this.clearTitle();

                    Game game = new Game();
                    this.players.stream()
                            .limit(MAXIMUM_PLAYERS) // make those bitches wait
                            .forEach(it -> {
                                this.removePlayer(it);
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
