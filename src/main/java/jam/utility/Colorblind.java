package jam.utility;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Colorblind {

    public static final Team COLORBLIND_TEAM;

    static {
        COLORBLIND_TEAM = MinecraftServer.getTeamManager().createTeam("colorblind");
        COLORBLIND_TEAM.setTeamColor(NamedTextColor.DARK_GREEN);
    }

    @SafeVarargs
    public static void slime(@NotNull Instance instance, @NotNull Map.Entry<Pos, Double> @NotNull... entries) {
        for (var entry : entries) {
            LivingEntity slime = new LivingEntity(EntityType.SLIME);
            slime.editEntityMeta(SlimeMeta.class, meta -> {
                meta.setSize(10);
                meta.setHasGlowingEffect(true);
                meta.setInvisible(true);
            });
            slime.setNoGravity(true);
            slime.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(entry.getValue());
            slime.setInstance(instance, entry.getKey());
            COLORBLIND_TEAM.addMember(slime.getUuid().toString());
        }
    }

}
