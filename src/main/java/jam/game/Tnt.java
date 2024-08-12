package jam.game;

import jam.utility.Components;
import jam.utility.Sphere;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.PrimedTntMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Tnt extends Entity {
    public static final double RANGE = 6;
    public static final Set<Point> POINTS = Sphere.getBlocksInSphere(RANGE);

    private final JamColor color;

    public Tnt(@NotNull JamColor color) {
        super(EntityType.TNT);

        this.color = color;

        double angle = ThreadLocalRandom.current().nextDouble() * 6.2831854820251465;
        setVelocity(new Vec(-Math.sin(angle) * 0.02, 0.2f, -Math.cos(angle) * 0.02)
                .mul(ServerFlag.SERVER_TICKS_PER_SECOND));
    }

    public int getFuse() {
        return ((PrimedTntMeta) getEntityMeta()).getFuseTime();
    }

    public void setFuse(int fuse) {
        ((PrimedTntMeta) getEntityMeta()).setFuseTime(fuse);
    }

    @Override
    public void update(long time) {
        if (onGround) velocity = velocity.mul(0.7, -0.5, 0.7);
        int newFuse = getFuse() - 1;
        setFuse(newFuse);
        if (newFuse <= 0) {
            Instance instance = this.instance;
            Pos pos = this.position;

            for (var point : POINTS) {
                var cur = point.add(pos);

                instance.setBlock(cur, color.convertBlockMaterial(instance.getBlock(cur)));
            }

            for (var entity : instance.getNearbyEntities(pos, RANGE)) {
                if (!(entity instanceof Player player)) continue;
                player.sendMessage(Component.textOfChildren(
                        Components.PREFIX,
                        Component.text("You were ", NamedTextColor.GRAY),
                        Component.text("hit", NamedTextColor.RED),
                        Component.text(" by ", NamedTextColor.GRAY),
                        Component.text(color.title(), color.getTextColor()),
                        Component.text(" TNT", NamedTextColor.RED),
                        Component.text("!", NamedTextColor.GRAY)
                ));

                Team team = player.getTag(Tags.TEAM);
                if (team == null || team == Team.SPECTATOR) continue;
                player.getTag(Tags.GAME).changeColor(player, color);
            }

            // Twice for good measure
            var packet = new ExplosionPacket(pos.x(), pos.y(), pos.z(), 25f,
                    new byte[0], 0f, 0f, 0f);
            sendPacketsToViewers(packet, packet);

            remove();
        }
    }

    @Override
    public double getEyeHeight() {
        return 0.15;
    }
}