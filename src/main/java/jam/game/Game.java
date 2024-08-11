package jam.game;

import jam.Config;
import jam.Server;
import jam.utility.Tags;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Game implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final int GRACE_PERIOD = 15;
    private static final int TIMER = 100 + GRACE_PERIOD;

    private final Instance instance;
    private final Set<Player> players;
    private final AtomicInteger timer = new AtomicInteger(TIMER);
    private final BossBar bossBar;

    public Game() {
        this.instance = Arena.random().createArenaInstance();
        this.players = new HashSet<>();
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
        return this.players;
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void spawnPlayer(Player player) {
        player.teleport(new Pos(Arena.SPAWN.randomBlock()));
        player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
        player.setGameMode(Config.DEBUG ? GameMode.CREATIVE : GameMode.ADVENTURE);

        // TODO
        var team = Team.RUNNER;
        var color = JamColor.GREEN;
        player.setTag(Tags.GAME, this);
        player.setTag(Tags.TEAM, team);
        player.setTag(Tags.COLOR, color);

        switch (team) {
            case HUNTER -> {
                player.sendTitlePart(TitlePart.SUBTITLE, Server.MINI_MESSAGE.deserialize(
                        "<gray>The hunters must stay on their color, try to tag them!"));

                player.sendTitlePart(TitlePart.TITLE, Component.text("Hunter Team"));
            }

            case RUNNER -> {
                player.sendTitlePart(TitlePart.SUBTITLE, Server.MINI_MESSAGE.deserialize(
                        "<gray>Run away from the hunters while staying on your color!"));

                player.sendTitlePart(TitlePart.TITLE, Component.text("Runner Team"));
            }
        }

        player.getInventory().setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        player.getInventory().setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        player.getInventory().setBoots(ItemStack.of(Material.LEATHER_BOOTS)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        bossBar.addViewer(player);
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
