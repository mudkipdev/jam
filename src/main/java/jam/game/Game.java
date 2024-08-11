package jam.game;

import jam.Config;
import jam.Server;
import jam.utility.Tags;
import jam.utility.Sounds;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.network.packet.server.play.EffectPacket;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Game implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final int GRACE_PERIOD = 15;
    private static final int GAME_TIME = 120;

    private final Arena arena;
    private final Instance instance;
    private final BossBar bossBar;

    // Stored as UUIDs to prevent potential memory leaks
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    private final AtomicBoolean ending = new AtomicBoolean(false);

    private final AtomicInteger gracePeriod = new AtomicInteger(GRACE_PERIOD);
    private @Nullable Task gracePeriodTask;

    private final AtomicInteger gameTime = new AtomicInteger(GAME_TIME);
    private @Nullable Task gameTickTask;

    private final Map<JamColor, net.minestom.server.scoreboard.Team> minecraftTeams = new HashMap<>();

    public Game() {
        this.arena = Arena.random();
        this.instance = arena.createArenaInstance();
        this.bossBar = BossBar.bossBar(
                Component.text("Starting game..."),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        for (JamColor color : JamColor.values()) {
            net.minestom.server.scoreboard.Team team = MinecraftServer.getTeamManager()
                    .createBuilder("color-" + color.name().toLowerCase() + "-")
                    .prefix(Component.text(
                            color.name().charAt(0),
                            color.getTextColor(),
                            TextDecoration.BOLD).appendSpace())
                    .teamColor(color.getTextColor())
                    .build();
            minecraftTeams.put(color, team);
        }
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

            JamColor color = JamColor.random();
            player.setTag(Tags.COLOR, color);

            minecraftTeams.get(color).addMember(player.getUsername());

            player.getInventory().setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
                    .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

            player.getInventory().setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS)
                    .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

            player.getInventory().setBoots(ItemStack.of(Material.LEATHER_BOOTS)
                    .with(ItemComponent.DYED_COLOR, color.getDyeColor()));
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
            player.setGlowing(true);
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

        this.gracePeriodTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::handleGracePeriod)
                .repeat(Duration.of(1, TimeUnit.SECOND))
                .schedule();
    }


    public void handlePlayerAttack(@NotNull Player attacker, @NotNull Player target) {
        if (ending.get()) return;

        Team attackerTeam = attacker.getTag(Tags.TEAM);
        Team targetTeam = target.getTag(Tags.TEAM);
        if (attackerTeam == null || targetTeam == null || attackerTeam == targetTeam) return;

        // magic reach check
        if (attacker.getPosition().distance(target.getPosition()) > 4.5) return;

        if (attackerTeam == Team.HUNTER && targetTeam == Team.RUNNER) {
            this.playEliminationAnimation(attacker, target);
            target.removeTag(Tags.TEAM);
            runners.remove(target.getUuid());
            target.setGameMode(GameMode.SPECTATOR);
            target.setInvisible(true);

            target.playSound(Sounds.DEATH_LOSE);

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

        gameTickTask.cancel();
        gameTickTask = null;

        bossBar.removeViewer(this);

        minecraftTeams.values().forEach(MinecraftServer.getTeamManager()::deleteTeam);

        switch (winner) {
            case HUNTER -> instance.showTitle(Title.title(
                    Component.text("Hunters have won!", NamedTextColor.RED),
                    Component.text("Every runner has been eliminated.")
            ));

            case RUNNER -> instance.showTitle(Title.title(
                    Component.text("Runners have won!", NamedTextColor.GREEN),
                    Component.text("The runners have evaded the hunters.")
            ));
        }

        for (var player : getPlayers()) {
            Team team = player.getTag(Tags.TEAM);
            if (team != Team.HUNTER && team != Team.RUNNER) continue;

            player.playSound(team == winner ? Sounds.FIREWORK_WIN : Sounds.DEATH_LOSE);
        }

        this.getPlayers().forEach(this::despawnPlayer);

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            for (Player player : instance.getPlayers()) {
                player.setInstance(Server.getLobby().getInstance());
            }
        }).delay(Duration.of(10, TimeUnit.SECOND)).schedule();
    }

    public void despawnPlayer(Player player) {
        player.removeTag(Tags.GAME);
        player.removeTag(Tags.TEAM);
        player.removeTag(Tags.COLOR);
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInvisible(true);
        player.setGlowing(false);
    }

    private void endGracePeriod() {
        this.gracePeriodTask.cancel();
        this.gracePeriodTask = null;

        for (var player : instance.getPlayers()) {
            // fucking vanilla
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1D);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.41999998688697815D);

            switch (player.getTag(Tags.TEAM)) {
                case RUNNER -> player.updateViewableRule(null);
                case HUNTER -> player.showTitle(Title.title(
                        Component.text("The hunt begins!"),
                        Server.MINI_MESSAGE.deserialize(
                                "<gray>The <yellow>grace period<gray> is over. <red>Hunt and eliminate<gray> the runners."
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(5000),
                                Duration.ofMillis(1000)
                        )
                ));
            }
        }

        this.playSound(Sounds.DRAGON);

        this.gameTickTask = MinecraftServer.getSchedulerManager().buildTask(this::handleGameTick).repeat(Duration.of(1, TimeUnit.SECOND)).schedule();
    }

    private void handleGracePeriod() {
        var remaining = gracePeriod.getAndDecrement();

        if (remaining == 0) {
            this.endGracePeriod();
            return;
        }

        this.bossBar.addViewer(this);

        for (var player : this.instance.getPlayers()) {
            if (player.getTag(Tags.TEAM) != Team.HUNTER) {
                continue;
            }

            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0D);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.0D);

            player.showTitle(Title.title(
                    Component.textOfChildren(
                            Component.text(remaining, NamedTextColor.RED),
                            Component.text(" second" + (remaining == 1 ? "" : "s") + " left", NamedTextColor.GRAY)
                    ),
                    Component.textOfChildren(
                            Component.text("of the ", NamedTextColor.GRAY),
                            Component.text("grace period", NamedTextColor.YELLOW),
                            Component.text("!", NamedTextColor.GRAY)
                    ),
                    Title.Times.times(
                            remaining == GRACE_PERIOD ? Title.DEFAULT_TIMES.fadeIn() : Duration.ZERO,
                            Title.DEFAULT_TIMES.stay(),
                            Title.DEFAULT_TIMES.fadeOut()
                    )
            ));
        }

        this.bossBar.name(Component.text(remaining + " second" + (remaining == 1 ? "" : "s") + " left (grace period)"));
        this.bossBar.color(BossBar.Color.BLUE);
        this.bossBar.progress((float) remaining / GRACE_PERIOD);
        this.playSound(Sounds.CLICK);
    }

    private void handleGameTick() {
        if (this.ending.get()) {
            return;
        }

        int remaining = this.gameTime.getAndDecrement();

        if (remaining == 0) {
            handleGameEnd(Team.RUNNER);
            return;
        }

        bossBar.name(Component.text(remaining + " second" + (remaining == 1 ? "" : "s") + " left"));
        bossBar.color(remaining < 0.2 * GAME_TIME ? BossBar.Color.RED : BossBar.Color.GREEN);
        bossBar.progress(remaining / (float) GAME_TIME);

        if (remaining != GAME_TIME && (remaining % 30 == 0 || (remaining <= 30 && remaining % 5 == 0) || (remaining <= 15))) {
            this.playSound(Sounds.CLICK);
        }

        if (remaining == 15) {
            sendMessage(Server.MINI_MESSAGE.deserialize(
                    "<red>15 seconds<gray> left! All <green>runners<gray> are now <yellow>glowing<gray>!"
            ));
            for (Player player : instance.getPlayers()) {
                if (player.getTag(Tags.TEAM) != Team.RUNNER) continue;

                player.setGlowing(true);
            }
        }
    }

    public static void playEliminationAnimation(Player hunter, Player runner) {
        var firework = new Entity(EntityType.FIREWORK_ROCKET);
        firework.setNoGravity(true);

        firework.editEntityMeta(FireworkRocketMeta.class, meta -> {
            meta.setFireworkInfo(ItemStack.of(Material.FIREWORK_ROCKET)
                    .with(ItemComponent.FIREWORK_EXPLOSION, new FireworkExplosion(
                            FireworkExplosion.Shape.LARGE_BALL,
                            List.of(),
                            List.of(),
                            false,
                            false)));
        });

        firework.setInstance(runner.getInstance(), runner.getPosition());

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            firework.getInstance().sendGroupedPacket(new EffectPacket(
                    1004,
                    firework.getPosition(),
                    0,
                    false));

            firework.remove();
        });

        runner.takeKnockback(
                0.5F,
                Math.sin(Math.toRadians(hunter.getPosition().yaw())),
                -Math.cos(Math.toRadians(hunter.getPosition().yaw())));
    }
}
