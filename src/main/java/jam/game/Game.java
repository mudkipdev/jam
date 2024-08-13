package jam.game;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jam.Config;
import jam.Server;
import jam.listener.EffectListeners;
import jam.utility.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.scoreboard.Sidebar;
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
import java.util.stream.Collectors;

import static jam.Server.MM;

public final class Game implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final int GRACE_PERIOD = Config.DEBUG ? 1 : 15;

    private static final double COLOR_CHANGE_RANGE = 3.5;
    private static final Set<Point> COLOR_CHANGE_POINTS = Sphere.getBlocksInSphere(COLOR_CHANGE_RANGE);
    private static final Zone COLOR_CHANGE_ZONE = new Zone(
            new Vec(-20, 0, -3),
            new Vec(20, 14, 50)
    );

    private final Arena arena;
    private final Instance instance;
    private final BossBar bossBar;

    private final Sidebar sidebar;

    private final Set<Player> players;

    // Stored as UUIDs to prevent potential memory leaks
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    private final AtomicInteger pointPotential = new AtomicInteger();

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

    private final Colorblind colorblind;

    private final Object2IntMap<UUID> damageEvasionCounter = new Object2IntOpenHashMap<>();

    public Game(Collection<Player> players) {
        this.players = new HashSet<>(players);
        this.queuedHunters = new ArrayList<>(players);

        for (var player : players) {
            points.put(player.getUuid(), 0);
        }

        this.arena = Arena.random();
        this.instance = arena.createArenaInstance();
        this.bossBar = BossBar.bossBar(
                Component.text("Starting game..."),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        this.sidebar = new Sidebar(MM.deserialize("<rainbow><b>Color Chase"));

        for (var player : players) {
            this.sidebar.addViewer(player);
        }

        this.initSidebar();

        instance.eventNode().addListener(InstanceTickEvent.class, event -> {
            for (var player : instance.getPlayers()) {
                Team team = player.getTag(Tags.TEAM);
                if (team == null) continue;

                JamColor color = player.getTag(Tags.COLOR);
                if (color == null) continue;

                Pos pos = player.getPosition();
                if (player.hasEffect(PotionEffect.HERO_OF_THE_VILLAGE)) {
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Pos newPos = pos.add(x, y, z);
                                instance.setBlock(newPos, color.convertBlockMaterial(instance.getBlock(newPos)));
                            }
                        }
                    }
                }

                ParticlePacket packet = new ParticlePacket(
                        Particle.DUST.withColor(color.getTextColor()),
                        pos.add(0, 1, 0),
                        new Vec(
                                ThreadLocalRandom.current().nextDouble() - 0.5,
                                ThreadLocalRandom.current().nextDouble() - 0.5,
                                ThreadLocalRandom.current().nextDouble() - 0.5
                        ),
                        0.2f,
                        player.hasEffect(PotionEffect.HERO_OF_THE_VILLAGE) ? 5 : 1
                );

                player.sendPacketToViewersAndSelf(packet);
            }
        });

        // Add the event listeners for effects
        instance.eventNode().addChild(EffectListeners.inkBlaster());
        instance.eventNode().addChild(EffectListeners.tnt());
        instance.eventNode().addChild(EffectListeners.enderPearl());
        instance.eventNode().addChild(EffectListeners.splashColorblindness());

        List<Map.Entry<Pos, Double>> positions = new ArrayList<>();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 4; z++) {
                positions.add(Map.entry(new Pos(x * 16 - 16, -16, z * 16 + 4), 3.25));
                positions.add(Map.entry(new Pos(x * 16 - 16, -16+32, z * 16 + 4), 3.25));
            }
        }

        for (int x = 0; x < 3; x++) {
            positions.add(Map.entry(new Pos(x * 16 - 16, 0, -1 * 16 + 4), 3.25));
            positions.add(Map.entry(new Pos(x * 16 - 16, 0, 4 * 16 + 4), 3.25));
        }

        for (int z = 0; z < 4; z++) {
            positions.add(Map.entry(new Pos(-1 * 16 - 16, 0, z * 16 + 4), 3.25));
            positions.add(Map.entry(new Pos(3 * 16 - 16, 0, z * 16 + 4), 3.25));
        }

        colorblind = new Colorblind(instance, positions);
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.getInstance().getPlayers();
    }

    public Instance getInstance() {
        return this.instance;
    }

    public Colorblind getColorblind() {
        return colorblind;
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
        }

        var huntersAmount = (int) Math.ceil(players.size() / 3.0);
        var initial = new ArrayList<>(players);

        // Init hunters
        for (var i = 0; i < huntersAmount && !queuedHunters.isEmpty(); i++) {
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
                    MM.deserialize("You are a <green>hunter<gray>!"),
                    Component.text("Avoid the hunters until the time runs out!")
            ));
        }

        // Set the point potential
        this.pointPotential.set(initial.size());

        // Init runners
        for (var player : initial) {
            player.setTag(Tags.TEAM, Team.RUNNER);
            this.runners.add(player.getUuid());

            player.updateViewableRule(other -> other.getTag(Tags.TEAM) == Team.RUNNER);

            player.showTitle(Title.title(
                    MM.deserialize("You are a <green>runner<gray>!"),
                    Component.text("Avoid the hunters until the time runs out!")
            ));

            if (round == 1) {
                player.setInstance(instance, arena.runnerSpawn());
            } else {
                player.teleport(arena.runnerSpawn());
            }
        }

        this.sendMessage(Component.textOfChildren(
                Component.newline(),
                Components.PREFIX,
                Component.text("Beginning round ", NamedTextColor.GRAY),
                Component.text(round, NamedTextColor.GREEN),
                Component.text("! The hunters are: ", NamedTextColor.GRAY),

                Component.text(this.players.stream()
                        .filter(player -> player.getTag(Tags.TEAM) == Team.HUNTER)
                        .map(Player::getUsername)
                        .collect(Collectors.joining(", "))),

                Component.newline()));

        this.maxGameTime = Config.DEBUG ? 300 : (15 + (30 * runners.size()));
        this.gameTime = new AtomicInteger(this.maxGameTime);
        this.bossBar.addViewer(this);

        this.gracePeriodTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::handleGracePeriod)
                .repeat(Duration.of(1, TimeUnit.SECOND))
                .schedule();

        updateSidebar();
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
            // Give the hunter 1 point
            this.handleDeath(attacker, target);
        }
    }

    public void handleRoundEnd(@NotNull Team winner) {
        if (queuedHunters.isEmpty() && ending.getAndSet(true)) return;

        gameTickTask.cancel();
        gameTickTask = null;

        bossBar.removeViewer(this);

        this.showTitle(Title.title(
                Component.text("Round over!", NamedTextColor.WHITE),
                (switch (winner) {
                    case RUNNER -> Component.text("Runners", NamedTextColor.GREEN);
                    case HUNTER -> Component.text("Hunters", NamedTextColor.RED);
                }).append(Component.text(" have won this round.", NamedTextColor.GRAY))
        ));

        // The amount of points given is the number of runners at the beginning minus the current amount of runners
        // plus 1. This means that surviving as others are killed will give you more points than a game where nobody
        // is killed.
        var pointsGained = pointPotential.get() - runners.size() + 1;
        for (var runnerUuid : runners) {
            var runner = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(runnerUuid);
            if (runner != null) {
                runner.sendMessage(MM.deserialize("<prefix><gray>You gained <gold>" + pointsGained + "</gold> points for surviving the round!"));
                points.computeIfPresent(runnerUuid, (ignored, curr) -> curr + pointsGained);
                updateSidebar();
            }
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
                    // Hide the sidebar from the player
                    this.sidebar.removeViewer(player);

                    // Send the player to the lobby
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
        player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
        player.setGlowing(false);
        player.setAdditionalHearts(0);
        if (player.getTeam() != null) player.setTeam(null);
        colorblind.removeViewer(player);
    }

    private void endGracePeriod() {
        this.gracePeriodTask.cancel();
        this.gracePeriodTask = null;
        this.changeMapColor(350, false);

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
                if (player.getTag(Tags.TEAM) == null) continue;

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

        float progress = Math.min(1, remaining / (float) this.maxGameTime);
        BossBar.Color bossbarColor;
        if (progress < 0.2) {
            bossbarColor = BossBar.Color.RED;
        } else if (progress < 0.5) {
            bossbarColor = BossBar.Color.YELLOW;
        } else {
            bossbarColor = BossBar.Color.GREEN;
        }

        bossBar.name(Component.text(remaining + " second" + (remaining == 1 ? "" : "s") + " left"));
        bossBar.color(bossbarColor);
        bossBar.progress(progress);

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

            Tag climbable = MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, "minecraft:climbable");

            Block block;
            JamColor color = null;
            for (int x = 0; x < 3; x++) {
                block = this.instance.getBlock(pos.add(0, -x, 0));
                color = JamColor.colorOfBlock(block);
                if (block.isSolid() || climbable.contains(block.namespace())) break;
                if (color != null) break;
            }

            var playerColor = player.getTag(Tags.COLOR);
            if (playerColor == null) continue;

            var inWater = Block.WATER.equals(this.instance.getBlock(player.getPosition()));

            final JamColor finalColor = color;
            int counter = damageEvasionCounter.compute(player.getUuid(), (uuid, count) -> (finalColor == playerColor) ? 0 : ((count == null ? 0 : count) + 1));

            if (inWater || counter >= 5 || (color != null && color != playerColor)) {
                player.sendActionBar(Component.textOfChildren(
                        Component.text("Wrong color! ", NamedTextColor.WHITE),
                        Component.text("Get to the ", NamedTextColor.GRAY),
                        Component.text(playerColor.title(), playerColor.getTextColor()),
                        Component.text("!", NamedTextColor.GRAY)));

                // TODO: add 2-3s invulnerability after color changes
                player.damage(DamageType.GENERIC, 1.0F);
            }
        }

        this.changeMapColor(10, true);

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
            sendMessage(MM.deserialize(
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
            if (hunter.getDistance(player) > 4.5D) {
                return;
            }

            // Give a point to the hunter
            hunter.sendMessage(MM.deserialize("<prefix><gray>You got <white>1<gray> point for killing " + player.getUsername() + "."));
            points.computeIfPresent(hunter.getUuid(), (ignored, curr) -> curr + 1);
            updateSidebar();

            player.takeKnockback(
                    0.5F,
                    Math.sin(Math.toRadians(hunter.getPosition().yaw())),
                    -Math.cos(Math.toRadians(hunter.getPosition().yaw())));
        }

        colorblind.removeViewer(player);

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
        player.setGlowing(false);

        Component message;

        if (hunter != null) {
            message = Component.textOfChildren(
                    Components.PREFIX,
                    Component.text(player.getUsername(), NamedTextColor.WHITE),
                    Component.text(" was tagged by ", NamedTextColor.GRAY),
                    Component.text(hunter.getUsername(), NamedTextColor.WHITE),
                    Component.text("!", NamedTextColor.GRAY),
                    Component.text(" There are ", NamedTextColor.GRAY),
                    Component.text(runners.size(), NamedTextColor.WHITE),
                    Component.text(" runners remaining.", NamedTextColor.GRAY));
        } else if (team == Team.RUNNER) {
            message = Component.textOfChildren(
                    Components.PREFIX,
                    Component.text(player.getUsername(), NamedTextColor.WHITE),
                    Component.text(" was eliminated! ", NamedTextColor.GRAY),
                    Component.text("There are ", NamedTextColor.GRAY),
                    Component.text(runners.size(), NamedTextColor.WHITE),
                    Component.text(" runners remaining.", NamedTextColor.GRAY));
        } else {
            message = Component.textOfChildren(
                    Components.PREFIX,
                    Component.text(player.getUsername(), NamedTextColor.WHITE),
                    Component.text(" was eliminated! ", NamedTextColor.GRAY),
                    Component.text("There are ", NamedTextColor.GRAY),
                    Component.text(hunters.size(), NamedTextColor.WHITE),
                    Component.text(" hunters remaining.", NamedTextColor.GRAY));
        }

        this.sendMessage(message);
    }

    public void changeColor(Player player, JamColor color) {
        JamColor old = player.getTag(Tags.COLOR);
        player.setTag(Tags.COLOR, color);

        if (old != null) {
            if (player.getTeam() != null) player.setTeam(null);
        }

        player.setTeam(JamColor.MINECRAFT_TEAMS.get(color));

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItemStack(i);

            Effect effect = item.getTag(Tags.EFFECT);
            if (effect == Effect.INK_BLASTER) {
                ItemStack newItem = player.getTag(Tags.COLOR).getInkBlaster().with(ItemComponent.DAMAGE, item.get(ItemComponent.DAMAGE));
                player.getInventory().setItemStack(i, newItem);
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

    public void changeMapColor(int rounds, boolean effects) {
        if (ending.get()) return;
        var blockBatch = new AbsoluteBlockBatch();

        for (int i = 0; i < rounds; i++) {
            // This task is laggy - make sure the game doesn't end while it's going on.
            if (ending.get()) return;

            var color = JamColor.random();
            var block = COLOR_CHANGE_ZONE.randomBlock();

            Block value;

            try {
                value = instance.getBlock(block);
            } catch (NullPointerException e) {
                value = null; // chunk unloaded fuck
            }

            if (value == null || value.isAir() || value.compare(Block.BARRIER)) {
                i--;
                continue;
            }

            if (effects) {
                instance.playSound(Sounds.LAVA_HISS.get(0.025F), block);
            }

            for (var loc : COLOR_CHANGE_POINTS) {
                int x = loc.blockX() + block.blockX();
                int y = loc.blockY() + block.blockY();
                int z = loc.blockZ() + block.blockZ();
                Block atPos = instance.getBlock(x, y, z);

                // ink blaster
                if (atPos.hasTag(Tags.PLAYER)) {
                    continue;
                }

                Block newBlock = color.convertBlockMaterial(atPos);

                if (effects && !newBlock.isAir() && ThreadLocalRandom.current().nextDouble() > 0.8) {
                    var packet = new ParticlePacket(Particle.DUST.withColor(color.getTextColor()), x, y+1.2, z, 0.1f, 0.1f, 0.1f, 0.2f, 1);
                    instance.sendGroupedPacket(packet);
                }

                blockBatch.setBlock(x, y, z, newBlock);
            }
        }

        blockBatch.apply(this.instance, () -> {
        });
    }

    private void initSidebar() {
        // Make the structure of the sidebar
        sidebar.createLine(new Sidebar.ScoreboardLine("round", Component.empty(), 0, Sidebar.NumberFormat.blank()));
        sidebar.updateLineScore("round", 999);
        sidebar.createLine(new Sidebar.ScoreboardLine("b1", Component.empty(), 1, Sidebar.NumberFormat.blank()));
        sidebar.updateLineScore("b1", 998);
        sidebar.createLine(new Sidebar.ScoreboardLine("top", MM.deserialize("<gray>Top 5"), 2, Sidebar.NumberFormat.blank()));
        sidebar.updateLineScore("top", 997);
        for (var i = 0; i < Math.min(5, players.size()); i++) {
            sidebar.createLine(new Sidebar.ScoreboardLine("top"+(i+1), Component.empty(), 3+i, Sidebar.NumberFormat.styled(Component.text("", NamedTextColor.YELLOW))));
        }

        // Initialize the sidebar
        this.updateSidebar();
    }

    private void updateSidebar() {
        sidebar.updateLineContent("round", MM.deserialize("<gray>Round: <white>" + round.get()));

        List<Map.Entry<UUID, Integer>> pointsSorted = new ArrayList<>(points.entrySet());
        pointsSorted.sort(Map.Entry.comparingByValue());

        for (var i = 0; i < Math.min(5, pointsSorted.size()); i++) {
            var entry = pointsSorted.get(i);
            var points = entry.getValue();
            var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(entry.getKey());
            if (player == null) {
                continue;
            }
            sidebar.updateLineContent("top"+(i+1), MM.deserialize(" " + player.getUsername()));
            sidebar.updateLineScore("top"+(i+1), points);
        }
    }
}
