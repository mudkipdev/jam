package jam.utility;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class Colorblind implements Viewable {

    public static final Team COLORBLIND_TEAM;

    static {
        COLORBLIND_TEAM = MinecraftServer.getTeamManager().createTeam("colorblind");
        COLORBLIND_TEAM.setTeamColor(NamedTextColor.DARK_GREEN);
    }

    private final List<Entity> entities = new ArrayList<>();

    private final Set<Player> viewers = new CopyOnWriteArraySet<>();
    private final Set<Player> unmodifiableViewers = Collections.unmodifiableSet(viewers);

    @SafeVarargs
    public Colorblind(@NotNull Instance instance, @NotNull Map.Entry<Pos, Double> @NotNull... entries) {
        for (var entry : entries) {
            LivingEntity slime = new LivingEntity(EntityType.SLIME);
            slime.editEntityMeta(SlimeMeta.class, meta -> {
                meta.setSize(10);
                meta.setHasGlowingEffect(true);
                meta.setInvisible(true);
            });
            slime.setNoGravity(true);
            slime.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(entry.getValue());
            slime.setAutoViewable(false);
            slime.setInstance(instance, entry.getKey());
            COLORBLIND_TEAM.addMember(slime.getUuid().toString());
            entities.add(slime);
        }
    }

    @Override
    public @NotNull Set<Player> getViewers() {
        return unmodifiableViewers;
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        final boolean result = this.viewers.add(player);
        entities.forEach(entity -> entity.addViewer(player));
        return result;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        final boolean result = this.viewers.remove(player);
        entities.forEach(entity -> entity.removeViewer(player));
        return result;
    }
}
