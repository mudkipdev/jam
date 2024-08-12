package jam.listener;

import jam.game.Effect;
import jam.game.Game;
import jam.game.TNT;
import jam.utility.Sounds;
import jam.utility.Sphere;
import jam.utility.Tags;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;

import java.util.Set;

public interface EffectListeners {

    static EventNode<InstanceEvent> inkBlaster() {
        Set<Point> POINTS = Sphere.getBlocksInSphere(2.0D);

        return EventNode.type("inkblaster", EventFilter.INSTANCE).addListener(ProjectileCollideWithBlockEvent.class, event -> {
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
        }).addListener(PlayerUseItemEvent.class, event -> {
            var effect = event.getItemStack().getTag(Tags.EFFECT);

            if (effect != Effect.INK_BLASTER || event.getHand() != Player.Hand.MAIN) {
                return;
            }

            var player = event.getPlayer();
            event.setCancelled(true);

            for (var i = 0; i < Effect.BULLETS; i++) {
                Effect.spawnInkBall(event.getPlayer(), Effect.calculateEyeDirection(player).mul(26));
            }

            player.setItemInMainHand(event.getItemStack().withAmount(i -> i - 1));
            player.playSound(Sounds.GHAST_SHOOT, Sound.Emitter.self());
        });
    }

    static EventNode<InstanceEvent> tnt() {

        return EventNode.type("tnt", EventFilter.INSTANCE).addListener(PlayerBlockPlaceEvent.class, event -> {
            if (!event.getBlock().compare(Block.TNT)) return;
            Player player = event.getPlayer();

            Player.Hand hand = event.getHand();
            ItemStack item = switch (hand) {
                case MAIN -> player.getItemInMainHand();
                case OFF -> player.getItemInOffHand();
            };

            var effect = item.getTag(Tags.EFFECT);
            if (effect != Effect.TNT) return;

            switch (hand) {
                case MAIN -> player.setItemInMainHand(player.getItemInMainHand().withAmount(i -> i - 1));
                case OFF -> player.setItemInOffHand(player.getItemInOffHand().withAmount(i -> i - 1));
            }

            new TNT(item.getTag(Tags.COLOR)).setInstance(event.getInstance(), event.getBlockPosition().add(0.5, 0, 0.5));
            event.setCancelled(true);
        });
    }

}
