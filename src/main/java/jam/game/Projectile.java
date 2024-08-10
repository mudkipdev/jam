package jam.game;

import jam.utility.Tags;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;

public final class Projectile extends Entity {
    public Projectile(Player player) {
        super(EntityType.ITEM_DISPLAY);
        var team = player.getTag(Tags.TEAM);

        this.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setTransformationInterpolationDuration(100);
            meta.setTransformationInterpolationStartDelta(-1);
            meta.setItemStack(ItemStack.of(team.color().getInkDye()));
        });

        this.setNoGravity(true);
        this.setVelocity((player.getPosition().direction()
//                .add(0.0D, player.getEyeHeight(), 0.0D)
                .mul(20.0F)));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (this.onGround) {
            this.remove();
        }
    }
}
