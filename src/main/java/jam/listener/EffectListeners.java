package jam.listener;

import io.github.togar2.pvp.entity.projectile.Arrow;
import io.github.togar2.pvp.entity.projectile.ThrownEnderpearl;
import io.github.togar2.pvp.entity.projectile.ThrownPotion;
import io.github.togar2.pvp.feature.effect.EffectFeature;
import io.github.togar2.pvp.feature.fall.FallFeature;
import jam.game.Effect;
import jam.game.Game;
import jam.game.Tnt;
import jam.utility.Sounds;
import jam.utility.Sphere;
import jam.utility.Tags;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityPotionAddEvent;
import net.minestom.server.event.entity.EntityPotionRemoveEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.potion.CustomPotionEffect;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.PotionType;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

            new Tnt(item.getTag(Tags.COLOR)).setInstance(event.getInstance(), event.getBlockPosition().add(0.5, 0, 0.5));
            event.setCancelled(true);
        });
    }

    static EventNode<InstanceEvent> enderPearl() {

        return EventNode.type("ender_pearl", EventFilter.INSTANCE).addListener(PlayerUseItemEvent.class, event -> {
            var effect = event.getItemStack().getTag(Tags.EFFECT);

            if (effect != Effect.ENDER_PEARL) {
                return;
            }

            Player player = event.getPlayer();

            switch (event.getHand()) {
                case MAIN -> player.setItemInMainHand(player.getItemInMainHand().withAmount(i -> i - 1));
                case OFF -> player.setItemInOffHand(player.getItemInOffHand().withAmount(i -> i - 1));
            }

            ThrownEnderpearl pearl = new ThrownEnderpearl(player, FallFeature.NO_OP);
            pearl.setItem(Effect.PEARL_ITEM);
            event.getInstance().playSound(Sounds.PEARL_THROW.get(), player.getPosition());

            Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);
            pearl.setInstance(Objects.requireNonNull(player.getInstance()), position);

            pearl.shootFromRotation(position.pitch(), position.yaw(), 0, 1.5, 1.0);

            Vec playerVel = player.getVelocity();
            pearl.setVelocity(pearl.getVelocity().add(playerVel.x(),
                    player.isOnGround() ? 0.0D : playerVel.y(), playerVel.z()));

        });
    }

    static EventNode<InstanceEvent> splashColorblindness() {
        return EventNode.type("splash_colorblindness", EventFilter.INSTANCE).addListener(PlayerUseItemEvent.class, event -> {
            var effect = event.getItemStack().getTag(Tags.EFFECT);

            if (effect != Effect.SPLASH_COLORBLINDNESS) {
                return;
            }

            Player player = event.getPlayer();

            switch (event.getHand()) {
                case MAIN -> player.setItemInMainHand(player.getItemInMainHand().withAmount(i -> i - 1));
                case OFF -> player.setItemInOffHand(player.getItemInOffHand().withAmount(i -> i - 1));
            }

            ThrownPotion potion = new ThrownPotion(player, new EffectFeature() {

                static final Potion EFFECT = new Potion(PotionEffect.LUCK, (byte) 0, Effect.COLORBLINDNESS_DURATION, 0);

                @Override
                public int getPotionColor(PotionContents contents) {
                    return new Color(150, 150, 150).getRGB();
                }

                @Override
                public List<Potion> getAllPotions(PotionType potionType, Collection<CustomPotionEffect> customEffects) {
                    return List.of(EFFECT);
                }

                @Override
                public void updatePotionVisibility(LivingEntity entity) {

                }

                @Override
                public void addArrowEffects(LivingEntity entity, Arrow arrow) {

                }

                @Override
                public void addSplashPotionEffects(LivingEntity entity, PotionContents potionContents, double proximity, @Nullable Entity source, @Nullable Entity attacker) {
                    if (!(entity instanceof Player player)) return;

                    player.addEffect(EFFECT);
                }
            });
            potion.setItem(event.getItemStack());
            event.getInstance().playSound(Sounds.SPLASH_THROW.get(), player.getPosition());

            Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);
            potion.setInstance(Objects.requireNonNull(player.getInstance()), position);

            potion.shootFromRotation(position.pitch(), position.yaw(), -20, 0.5, 1.0);

            Vec playerVel = player.getVelocity();
            potion.setVelocity(potion.getVelocity().add(playerVel.x(),
                    player.isOnGround() ? 0.0 : playerVel.y(), playerVel.z()));
        }).addListener(EntityPotionAddEvent.class, event -> {
            if (!event.getPotion().effect().equals(PotionEffect.LUCK)) return;
            if (!(event.getEntity() instanceof Player player)) return;
            Game game = player.getTag(Tags.GAME);
            if (game == null) return;

            game.getColorblind().addViewer(player);
        }).addListener(EntityPotionRemoveEvent.class, event -> {
            if (!event.getPotion().effect().equals(PotionEffect.LUCK)) return;
            if (!(event.getEntity() instanceof Player player)) return;
            Game game = player.getTag(Tags.GAME);
            if (game == null) return;

            game.getColorblind().removeViewer(player);
        });
    }



}
