package jam.utility;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * @author emortal
 */
public class BetterEntityProjectile extends EntityProjectile {
    private boolean ticking = true;

    public BetterEntityProjectile(Entity shooter, EntityType entityType) {
        super(shooter, entityType);
    }

    @Override
    public void tick(long time) {
        if (this.ticking) {
            super.tick(time);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> teleport(@NotNull Pos position, long[] chunks, int flags, boolean shouldConfirm) {
        if (instance == null) return CompletableFuture.completedFuture(null);
        return super.teleport(position, chunks, flags, shouldConfirm);
    }

    public void setPhysics(boolean physics) {
        this.hasPhysics = physics;
    }

    public void setTicking(boolean ticking) {
        this.ticking = ticking;
    }
}