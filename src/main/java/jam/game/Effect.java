package jam.game;

import jam.utility.BetterEntityProjectile;
import jam.utility.Sphere;
import jam.utility.Tags;
import jam.utility.Titleable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.time.TimeUnit;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public enum Effect implements Titleable {
    INK_BLASTER(Material.WHITE_CANDLE) {
        private static final Set<Point> POINTS = Sphere.getBlocksInSphere(2.0D);
        private static final double SPREAD = 0.1D;
        private static final double BULLETS = 10.0D;

        private static Vec calculateEyeDirection(Player player) {
            var random = ThreadLocalRandom.current();

            return player.getPosition().direction()
                    .rotateAroundX(random.nextDouble(-SPREAD, SPREAD))
                    .rotateAroundY(random.nextDouble(-SPREAD, SPREAD))
                    .rotateAroundZ(random.nextDouble(-SPREAD, SPREAD));
        }

        private static void spawnInkBall(Player player, Vec velocity) {
            var instance = player.getInstance();
            var inkBall = new BetterEntityProjectile(player, EntityType.SNOWBALL);

            var meta = (SnowballMeta) inkBall.getEntityMeta();
            meta.setItem(ItemStack.of(player.getTag(Tags.COLOR).getInkDye()));
            inkBall.setVelocity(velocity);
            inkBall.setInstance(instance, player.getPosition().add(0.0D, player.getEyeHeight(), 0.0D));
            inkBall.scheduleRemove(10, TimeUnit.SECOND);
        }

        @Override
        public void activate(Player player, Game game) {
            var itemStack = player.getTag(Tags.COLOR).getInkBlaster();
            player.getInventory().setItemStack(0, itemStack);

            player.eventNode().addListener(PlayerUseItemEvent.class, event -> {
                // TODO: account for color changes
                if (!event.getItemStack().equals(itemStack)) {
                    return;
                }

                if (event.getHand() != Player.Hand.MAIN) {
                    return;
                }

                event.setCancelled(true);

                for (var i = 0; i < BULLETS; i++) {
                    spawnInkBall(event.getPlayer(), calculateEyeDirection(player).mul(26));
                }

                game.getInstance().playSound(
                        Sound.sound(
                                SoundEvent.ENTITY_GHAST_SHOOT,
                                Sound.Source.MASTER,
                                0.5F,
                                2.0F),
                        Sound.Emitter.self());
            });

            game.getInstance().eventNode().addListener(ProjectileCollideWithBlockEvent.class, event -> {
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

                    if (block.isAir()) continue;

                    projectile.getInstance().setBlock(nearbyBlock.position(), color.convertBlockMaterial(block));
                }

                shooter.getInstance().playSound(
                        Sound.sound(
                                SoundEvent.ENTITY_DONKEY_CHEST,
                                Sound.Source.MASTER,
                                0.3F,
                                2.0F),
                        projectile.getPosition()) ;

                projectile.remove();
            });
        }
    };

    private final Material icon;

    Effect(Material icon) {
        this.icon = icon;
    }

    public static Effect random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    public void activate(Player player, Game game) {

    }

    public Material getIcon() {
        return this.icon;
    }

    public ItemStack createItemStack() {
        return ItemStack.of(this.icon)
                .with(ItemComponent.ITEM_NAME, Component.text(this.title()));
    }
}
