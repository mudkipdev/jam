package jam.listener;

import jam.utility.Sphere;
import jam.utility.Tags;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;

import java.util.Set;

public interface EffectListeners {

    static EventListener<ProjectileCollideWithBlockEvent> inkBlaster() {
        Set<Point> POINTS = Sphere.getBlocksInSphere(2.0D);

        return EventListener.of(ProjectileCollideWithBlockEvent.class, event -> {
            if (!(event.getEntity() instanceof EntityProjectile projectile)) {
                return;
            }

            var shooter = (Player) projectile.getShooter();
            if (shooter == null) return;
            var color = shooter.getTag(Tags.COLOR);
            if (color == null) return;

            var nearbyBlocks = Sphere.getNearbyBlocks(
                    projectile.getPosition(),
                    POINTS,
                    projectile.getInstance(),
                    Block::isSolid);

            for (var nearbyBlock : nearbyBlocks) {
                Block block = projectile.getInstance().getBlock(nearbyBlock.position());

                if (block.isAir()) {
                    continue;
                }

                projectile.getInstance().setBlock(nearbyBlock.position(), color.convertBlockMaterial(block)
                        .withTag(Tags.PLAYER, true));
            }

            shooter.getInstance().playSound(
                    Sound.sound(
                            SoundEvent.ENTITY_DONKEY_CHEST,
                            Sound.Source.MASTER,
                            0.3F,
                            2.0F),
                    projectile.getPosition());

            projectile.remove();
        });
    }

}
