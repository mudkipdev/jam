package jam.game;

import jam.Config;
import jam.Server;
import jam.listener.EffectListeners;
import jam.utility.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
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
    private static final int GRACE_PERIOD = Config.DEBUG ? 1 : 15;

    private static final double COLOR_CHANGE_RANGE = 3.5;
    private static final Set<Point> COLOR_CHANGE_POINTS = Sphere.getBlocksInSphere(COLOR_CHANGE_RANGE);
    private static final Zone COLOR_CHANGE_ZONE = new Zone(
            new Vec(-20, -5, -10),
            new Vec(20, 10, 50)
    );

    private final Arena arena;
    private final Instance instance;
    private final BossBar bossBar;

    private final Set<Player> players;

    // Stored as UUIDs to prevent potential memory leaks
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    // Store the players that will be hunters in the next rounds
    private final List<Player> queuedHunters;

    private final Map<UUID, Integer> points = new HashMap<>();
    private final AtomicInteger round = new AtomicInteger();

    private final AtomicBoolean ending = new AtomicBoolean(false);

    private final List<Collectible> collectibles = new ArrayList<>();

    private final AtomicInteger gracePeriod = new AtomicInteger(GRACE_PERIOD + 1); // Buffer time so it actually displays 15
    private @Nullable Task gracePeriodTask;

    private int maxGameTime;
    private AtomicInteger gameTime;
    private @Nullable Task gameTickTask;

    private final Map<JamColor, net.minestom.server.scoreboard.Team> minecraftTeams = new HashMap<>();

    public Game(Collection<Player> players) {
        this.players = new HashSet<>(players);
        this.queuedHunters = new ArrayList<>(players);

        this.arena = Arena.random();
        this.instance = arena.createArenaInstance();
        this.bossBar = BossBar.bossBar(
                Component.text("Starting game..."),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        for (JamColor color : JamColor.values()) {
            net.minestom.server.scoreboard.Team team = MinecraftServer.getTeamManager()
                    .createBuilder("color-" + color.name().toLowerCase())
                    .prefix(Component.text(
                            color.name().charAt(0),
                            color.getTextColor(),
                            TextDecoration.BOLD).appendSpace())
                    .teamColor(color.getTextColor())
                    .build();
            minecraftTeams.put(color, team);
        }

        this.instance.eventNode().addListener(PlayerMoveEvent.class, event -> {
            if (event.getPlayer().getTag(Tags.TEAM) == null) {
                return;
            }

            Player player = event.getPlayer();
            double range = Collectible.COLLECT_DISTANCE * Collectible.COLLECT_DISTANCE;

            for (var collectible : collectibles) {
                if (collectible.getDistanceSquared(event.getNewPosition()) <= range) {
                    collectible.collect(player);
                }
            }
        });

        // Add the event listeners for effects
        instance.eventNode().addChild(EffectListeners.inkBlaster());
        instance.eventNode().addChild(EffectListeners.tnt());
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.getInstance().getPlayers();
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void beginNextRound() {
        // Reset everything
        this.hunters.clear();
        this.runners.clear();
        this.collectibles.clear();
        this.gracePeriod.set(GRACE_PERIOD + 1);

        // Purge offline players & incr round count
        this.purgeOfflinePlayers();
        int round = this.round.incrementAndGet();

        for (var player : players) {
            player.setTag(Tags.GAME, this);
            player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvisible(false);
            player.setEnableRespawnScreen(false);

            player.sendMessage(Component.textOfChildren(
                    Components.PREFIX,
                    Component.text("Beginning round ", NamedTextColor.GRAY),
                    Component.text(round, NamedTextColor.GREEN),
                    Component.text("!", NamedTextColor.GRAY)));
        }

        var hunters = (int) Math.ceil(players.size() / 3.0);
        var initial = new ArrayList<>(players);

        // Init hunters
        for (var i = 0; i < hunters && !queuedHunters.isEmpty(); i++) {
            int index = ThreadLocalRandom.current().nextInt(queuedHunters.size());
            var player = queuedHunters.remove(index);
            player.setTag(Tags.TEAM, Team.HUNTER);

            initial.remove(player);

            this.hunters.add(player.getUuid());

            if (round == 1) {
                player.setInstance(instance, arena.hunterSpawn());
            } else {
                player.teleport(arena.hunterSpawn());
            }

            player.addEffect(new Potion(PotionEffect.BLINDNESS, (byte) 0, (GRACE_PERIOD + 2) * 20, 0));
            player.setGlowing(true);

            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0D);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.0D);

            player.showTitle(Title.title(
                    Server.MINI_MESSAGE.deserialize("You are a <green>hunter<gray>!"),
                    Component.text("Avoid the hunters until the time runs out!")
            ));
        }

        // Init runners
        for (var player : initial) {
            player.setTag(Tags.TEAM, Team.RUNNER);
            this.runners.add(player.getUuid());

            player.updateViewableRule(other -> other.getTag(Tags.TEAM) == Team.RUNNER);

            player.showTitle(Title.title(
                    Server.MINI_MESSAGE.deserialize("You are a <green>runner<gray>!"),
                    Component.text("Avoid the hunters until the time runs out!")
            ));

            if (round == 1) {
                player.setInstance(instance, arena.runnerSpawn());
            } else {
                player.teleport(arena.runnerSpawn());
            }
        }

        this.maxGameTime = 15 + (30 * runners.size());
        this.gameTime = new AtomicInteger(this.maxGameTime);
        this.bossBar.addViewer(this);

        this.gracePeriodTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::handleGracePeriod)
                .repeat(Duration.of(1, TimeUnit.SECOND))
                .schedule();
    }

    private void purgeOfflinePlayers() {
        players.removeIf(player -> !player.isOnline());
        queuedHunters.removeIf(player -> !player.isOnline());
    }

    public void handlePlayerAttack(@NotNull Player attacker, @NotNull Player target) {
        if (ending.get()) return;

        Team attackerTeam = attacker.getTag(Tags.TEAM);
        Team targetTeam = target.getTag(Tags.TEAM);
        if (attackerTeam == null || targetTeam == null || attackerTeam == targetTeam) return;

        // magic reach check
        if (attacker.getPosition().distance(target.getPosition()) > 4.5) return;

        if (attackerTeam == Team.HUNTER && targetTeam == Team.RUNNER) {
            this.handleDeath(attacker, target);
        }
    }

    public void handleRoundEnd(@NotNull Team winner) {
        if (queuedHunters.isEmpty() && ending.getAndSet(true)) return;

        gameTickTask.cancel();
        gameTickTask = null;

        bossBar.removeViewer(this);

        minecraftTeams.values().forEach(MinecraftServer.getTeamManager()::deleteTeam);

        switch (winner) {
            case HUNTER -> this.showTitle(Title.title(
                    Component.text("Hunters have won the round!", NamedTextColor.RED),
                    Component.text("Every runner has been eliminated.")
            ));

            case RUNNER -> this.showTitle(Title.title(
                    Component.text("Runners have won the round!", NamedTextColor.GREEN),
                    Component.text("The runners have evaded the hunters.")
            ));
        }

        for (var player : getPlayers()) {
            Team team = player.getTag(Tags.TEAM);
            if (team != Team.HUNTER && team != Team.RUNNER) continue;

            player.playSound(team == winner ? Sounds.FIREWORK_WIN : Sounds.DEATH_LOSE);
        }

        this.getPlayers().forEach(this::despawnPlayer);

        if (queuedHunters.isEmpty()) {
            MinecraftServer.getSchedulerManager().buildTask(() -> {
                for (Player player : instance.getPlayers()) {
                    player.setInstance(Server.getLobby().getInstance());
                }
            }).delay(Duration.of(10, TimeUnit.SECOND)).schedule();
        } else {
            // Begin the next round after 5 seconds
            MinecraftServer.getSchedulerManager()
                    .buildTask(this::beginNextRound)
                    .delay(Duration.of(5, TimeUnit.SECOND))
                    .schedule();
        }
    }

    public void despawnPlayer(Player player) {
        player.removeTag(Tags.GAME);
        player.removeTag(Tags.TEAM);
        player.removeTag(Tags.COLOR);
        player.getInventory().clear();
        player.setGlowing(false);
    }

    private void endGracePeriod() {
        this.gracePeriodTask.cancel();
        this.gracePeriodTask = null;
        this.changeMapColor(350);

        for (var player : instance.getPlayers()) {
            // fucking vanilla
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1D);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.41999998688697815D);

            switch (player.getTag(Tags.TEAM)) {
                case RUNNER -> player.updateViewableRule(null);
                case HUNTER -> player.sendMessage(Component.textOfChildren(
                        Component.newline(),
                        Components.PREFIX,
                        Component.text("The hunt begins!", NamedTextColor.RED),
                        Component.newline(),
                        Components.PREFIX,
                        Component.text("The ", NamedTextColor.GRAY),
                        Component.text("grace period", NamedTextColor.YELLOW),
                        Component.text(" is over. ", NamedTextColor.GRAY),
                        Component.text("Hunt", NamedTextColor.RED),
                        Component.text(" and ", NamedTextColor.GRAY),
                        Component.text("eliminate", NamedTextColor.RED),
                        Component.text(" the runners.", NamedTextColor.RED),
                        Component.newline()
                ));
            }
        }

        this.playSound(Sounds.DRAGON);

        this.gameTickTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::handleGameTick)
                .repeat(Duration.of(1, TimeUnit.SECOND))
                .schedule();

        MinecraftServer.getSchedulerManager().buildTask(() -> MinecraftServer.getSchedulerManager().submitTask(() -> {
            if (this.ending.get()) {
                return TaskSchedule.stop();
            }

            this.spawnRandomEffect();
            var random = ThreadLocalRandom.current();
            return TaskSchedule.duration(random.nextInt(20, 30), TimeUnit.SECOND);
        })).delay(10, TimeUnit.SECOND).schedule();
    }

    private void handleGracePeriod() {
        var remaining = gracePeriod.getAndDecrement();

        if (remaining == 0) {
            this.endGracePeriod();
            return;
        }

        if (remaining == GRACE_PERIOD) {
            for (var player : this.instance.getPlayers()) {
                var team = player.getTag(Tags.TEAM);
                if (team == null || team == Team.SPECTATOR) continue;

                this.changeColor(player, JamColor.random());
            }
        }

        this.bossBar.addViewer(this);

        for (var player : this.instance.getPlayers()) {
            if (player.getTag(Tags.TEAM) != Team.HUNTER) {
                continue;
            }

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
        this.bossBar.progress(Math.min(1, (float) remaining / GRACE_PERIOD));
        this.playSound(Sounds.CLICK);
    }

    private void handleGameTick() {
        if (this.ending.get()) {
            return;
        }

        int remaining = this.gameTime.getAndDecrement();

        if (remaining == 0) {
            handleRoundEnd(Team.RUNNER);
            return;
        }

        bossBar.name(Component.text(remaining + " second" + (remaining == 1 ? "" : "s") + " left"));
        bossBar.color(remaining < 0.2 * this.maxGameTime ? BossBar.Color.RED : BossBar.Color.GREEN);
        bossBar.progress(remaining / (float) this.maxGameTime);

        if (remaining < 15 || remaining == 30 || remaining == 60) {
            this.playSound(Sounds.CLICK);
        }

        for (var player : this.instance.getPlayers()) {
            if (remaining > (this.maxGameTime - 5)) {
                continue; // invulnerability
            }

            if (!player.hasTag(Tags.COLOR)) {
                continue;
            }

            if (player.getHealth() == 0.0F) {
                this.handleDeath(null, player);
            }

            var pos = player.getPosition();
            var color = JamColor.colorOfBlock(this.instance.getBlock(pos));

            if (color == null) { // Try another block
                color = JamColor.colorOfBlock(this.instance.getBlock(pos.add(0, -1, 0)));
            }

            if (!player.hasTag(Tags.COLOR)) {
                continue;
            }

            var playerColor = player.getTag(Tags.COLOR);
            var inWater = Block.WATER.equals(this.instance.getBlock(player.getPosition()));

            if (inWater || (color != null && color != playerColor)) {
                player.sendActionBar(Component.textOfChildren(
                        Component.text("Wrong color! ", NamedTextColor.WHITE),
                        Component.text("Get to the ", NamedTextColor.GRAY),
                        Component.text(playerColor.title(), playerColor.getTextColor()),
                        Component.text("!", NamedTextColor.GRAY)));

                // TODO: add 2-3s invulnerability after color changes
                player.damage(DamageType.GENERIC, 2.0F);
            }
        }

        this.changeMapColor(10);

        if (remaining % 20 == 0) {
            this.playSound(Sounds.NOTE);

            for (var player : this.getInstance().getPlayers()) {
                if (!player.hasTag(Tags.COLOR)) {
                    continue;
                }

                var color = JamColor.random();
                this.changeColor(player, color);

                player.sendTitlePart(TitlePart.TITLE, Component.empty());
                player.sendTitlePart(TitlePart.SUBTITLE, Component.textOfChildren(
                        Component.text("Your color is now ", NamedTextColor.WHITE),
                        Component.text(color.title(), color.getTextColor()),
                        Component.text("!", NamedTextColor.WHITE)));
            }
        }

        if (remaining % 3 == 0) {
            for (var player : this.getInstance().getPlayers()) {
                var max = player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH);
                player.setHealth(Math.min(player.getHealth() + 1.0F, (float) max));
            }
        }

        if (remaining == 15) {
            sendMessage(Server.MINI_MESSAGE.deserialize(
                    "<prefix><red>15 seconds<gray> left! All <green>runners<gray> are now <yellow>glowing<gray>!"
            ));
            for (Player player : instance.getPlayers()) {
                if (player.getTag(Tags.TEAM) != Team.RUNNER) continue;

                player.setGlowing(true);
            }
        }
    }

    private void handleDeath(@Nullable Player hunter, @NotNull Player player) {
        Team team = player.getTag(Tags.TEAM);
        JamColor color = player.getTag(Tags.COLOR);
        if (team == null || color == null) return;

        if (hunter != null) {
            player.takeKnockback(
                    0.5F,
                    Math.sin(Math.toRadians(hunter.getPosition().yaw())),
                    -Math.cos(Math.toRadians(hunter.getPosition().yaw())));
        }

        var firework = new Entity(EntityType.FIREWORK_ROCKET);
        firework.setNoGravity(true);

        var explosion = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                List.of(color.getTextColor()),
                List.of(color.getTextColor()),
                false,
                false);

        firework.editEntityMeta(FireworkRocketMeta.class, meta ->
                meta.setFireworkInfo(ItemStack.of(Material.FIREWORK_ROCKET)
                        .with(ItemComponent.FIREWORKS, new FireworkList(
                                (byte) 0, List.of(explosion)))));

        firework.setInstance(player.getInstance(), player.getPosition().add(0.0D, 2.0D, 0.0D));
        firework.triggerStatus((byte) 17);
        firework.getInstance().sendGroupedPacket(new EffectPacket(
                1004,
                firework.getPosition(),
                0,
                false));

        MinecraftServer.getSchedulerManager().scheduleNextTick(firework::remove);

        if (team == Team.RUNNER) {
            this.runners.remove(player.getUuid());

            if (this.runners.isEmpty()) {
                this.handleRoundEnd(Team.HUNTER);
            }
        } else if (team == Team.HUNTER) {
            this.hunters.remove(player.getUuid());

            if (this.hunters.isEmpty()) {
                this.handleRoundEnd(Team.RUNNER);
            }
        }

        player.removeTag(Tags.TEAM);
        player.setGameMode(GameMode.SPECTATOR);
        player.playSound(Sounds.DEATH_LOSE);
        player.setInvisible(true);

        Component message;

        if (hunter != null) {
            message = Component.textOfChildren(
                    Components.PREFIX,
                    Component.text(player.getUsername(), NamedTextColor.GREEN),
                    Component.text(" was tagged by ", NamedTextColor.YELLOW),
                    Component.text(hunter.getUsername(), NamedTextColor.RED),
                    Component.text("!", NamedTextColor.YELLOW),
                    Component.text(" There are ", NamedTextColor.GRAY),
                    Component.text(runners.size(), NamedTextColor.YELLOW),
                    Component.text(" runners remaining.", NamedTextColor.GRAY));
        } else {
            message = Component.textOfChildren(
                    Components.PREFIX,
                    Component.text(player.getUsername(), NamedTextColor.GREEN),
                    Component.text(" was eliminated! ", NamedTextColor.YELLOW),
                    Component.text("There are ", NamedTextColor.GRAY),
                    Component.text(runners.size(), NamedTextColor.YELLOW),
                    Component.text(" runners remaining.", NamedTextColor.GRAY));
        }

        this.sendMessage(message);
    }

    public void changeColor(Player player, JamColor color) {
        JamColor old = player.getTag(Tags.COLOR);
        player.setTag(Tags.COLOR, color);

        if (old != null) {
            this.minecraftTeams.get(old).removeMember(player.getUsername());
        }
        this.minecraftTeams.get(color).addMember(player.getUsername());

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItemStack(i);

            Effect effect = item.getTag(Tags.EFFECT);
            if (effect == Effect.INK_BLASTER) {
                player.getInventory().setItemStack(i, player.getTag(Tags.COLOR).getInkBlaster().withAmount(item.amount()));
            }
        }

        player.getInventory().setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        player.getInventory().setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        player.getInventory().setBoots(ItemStack.of(Material.LEATHER_BOOTS)
                .with(ItemComponent.DYED_COLOR, color.getDyeColor()));

        player.getInventory().setItemStack(8, color.getTeamIndicator());

        player.sendMessage(Component.textOfChildren(
                Components.PREFIX,
                Component.text("You are now ", NamedTextColor.GRAY),
                Component.text(color.title(), color.getTextColor()),
                Component.text(".", NamedTextColor.GRAY)
        ));
    }

    private void spawnRandomEffect() {
        var effect = Effect.random();
        var position = this.arena.pickEffectSpawn();

        int y;
        for (y = position.blockY() + 50; y >= position.blockY(); y--) {
            Block block = instance.getBlock(position.blockX(), y, position.blockZ());
            if (!block.isAir() && block.id() != Block.BARRIER.id()) {
                break;
            }
        }

        var newPos = position.withY(y).add(0.5, 1, 0.5);

        var collectible = new Collectible(effect);
        collectible.setInstance(this.instance, newPos);
        collectibles.add(collectible);
    }

    public void handleExternalPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Team team = player.getTag(Tags.TEAM);
        if (team == null) return;

        event.setChatMessage(null);
        handleDeath(null, player);
    }

    public void changeMapColor(int rounds) {
        if (ending.get()) return;
        var blockBatch = new AbsoluteBlockBatch();

        for (int i = 0; i < rounds; i++) {
            // This task is laggy - make sure the game doesn't end while it's going on.
            if (ending.get()) return;

            var color = JamColor.random();
            var block = COLOR_CHANGE_ZONE.randomBlock();

            for (var loc : COLOR_CHANGE_POINTS) {
                int x = loc.blockX() + block.blockX();
                int y = loc.blockY() + block.blockY();
                int z = loc.blockZ() + block.blockZ();
                Block atPos = instance.getBlock(x, y, z);

                // ink blaster
                if (atPos.hasTag(Tags.PLAYER)) {
                    continue;
                }

                blockBatch.setBlock(x, y, z, color.convertBlockMaterial(atPos));
            }
        }

        blockBatch.apply(this.instance, () -> {
        });
    }
}
