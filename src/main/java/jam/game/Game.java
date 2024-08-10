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
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class Game implements PacketGroupingAudience {
    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final int TIMER = 70;

    private final Instance instance;
    private final Set<Team> teams;
    private final BossBar bossBar;
    private final AtomicInteger timer = new AtomicInteger(TIMER);

    public Game() {
        this.instance = createArenaInstance();
        this.teams = new HashSet<>() {{
            Team.Color color = Team.Color.random();
            LOGGER.info("The teams are {} and {}.", color, color.getComplementaryColor());
            this.add(new Team(Game.this, new HashSet<>(), color, Team.NORTH_SPAWN));
            this.add(new Team(Game.this, new HashSet<>(), color.getComplementaryColor(), Team.SOUTH_SPAWN));
        }};

        // change the concrete spawn platform's color
        this.teams.forEach(team -> team.spawn().eachBlock(vector -> {
                this.instance.setBlock(vector, team.color().getInkBlock());
                LOGGER.trace("Filling {} with {}.", team.spawn().start(), team.color().getInkBlock());
        }));

        this.bossBar = BossBar.bossBar(
                Component.text(this.timer.get() + " seconds left"),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (timer.get() == 0) {
                        bossBar.removeViewer(this);
                        this.getPlayers().forEach(this::despawnPlayer);
                        return;
                    }

                    bossBar.name(Component.text(this.timer.get() + " seconds left"));
                    bossBar.progress((float) this.timer.getAndDecrement() / TIMER);
                    bossBar.color(
                            this.timer.get() < 20
                                    ? BossBar.Color.RED
                                    : (this.timer.get() < (TIMER / 3)
                                            ? BossBar.Color.YELLOW
                                            : BossBar.Color.GREEN));
                })
                .repeat(1, TimeUnit.SECOND)
                .schedule();
    }

    private static Instance createArenaInstance() {
        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        var arena = Arena.random();

        instance.setChunkSupplier(LightingChunk::new);
        instance.setChunkLoader(arena.createLoader());
        instance.setTimeRate(0);
        instance.setTime(18000);

        return instance;
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.teams.stream()
                .flatMap(team -> team.players().stream())
                .collect(Collectors.toSet());
    }

    public Instance getInstance() {
        return this.instance;
    }

    public void spawnPlayer(Player player) {
        player.setHealth((float) player.getAttributeValue(Attribute.GENERIC_MAX_HEALTH));
        player.setGameMode(Config.DEBUG ? GameMode.CREATIVE : GameMode.ADVENTURE);

        var team = this.teams.stream()
                .min(Comparator.comparingInt(it -> it.players().size()))
                .orElseThrow();

        var component = Component.textOfChildren(
                Component.text("You are on the ", NamedTextColor.GRAY),
                team.getTitle(),
                Component.text(" team.", NamedTextColor.GRAY));

        player.setTag(Tags.TEAM, team);
        player.setTeam(team.scoreboard());
        team.players().add(player);

        player.getInventory().setItemStack(0, team.color().getInkBlaster());
        player.getInventory().setItemStack(1, ItemStack.of(Material.IRON_HOE));
        player.getInventory().setItemStack(2, ItemStack.of(Material.WIND_CHARGE));

        player.getInventory().setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
                .with(ItemComponent.DYED_COLOR, team.color().getDyeColor()));

        player.getInventory().setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS)
                .with(ItemComponent.DYED_COLOR, team.color().getDyeColor()));

        player.getInventory().setBoots(ItemStack.of(Material.LEATHER_BOOTS)
                .with(ItemComponent.DYED_COLOR, team.color().getDyeColor()));

        bossBar.addViewer(player);
        player.sendTitlePart(TitlePart.TITLE, Component.empty());
        player.sendTitlePart(TitlePart.SUBTITLE, component);
        player.sendMessage(Component.newline().append(component).appendNewline());
    }

    public void despawnPlayer(Player player) {
        player.removeTag(Tags.TEAM);
        player.setTeam(null);
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInstance(Server.getLobby().getInstance());
    }
}
