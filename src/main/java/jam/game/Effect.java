package jam.game;

import jam.Server;
import jam.utility.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.time.TimeUnit;

import java.util.concurrent.ThreadLocalRandom;

public enum Effect implements Titleable {
    INK_BLASTER(Material.WHITE_CANDLE, """
            <newline><prefix><gray>An <yellow><bold>Ink Blaster<reset><gray> has spawned in a random spot!
            <prefix>Shoot ink to <light_purple>change the colors of blocks<gray>!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            var itemStack = player.getTag(Tags.COLOR).getInkBlaster().withAmount(16);
            player.getInventory().addItemStack(itemStack);
        }
    };

    public static final double SPREAD = 0.15D;
    public static final double BULLETS = 10.0D;

    public static Vec calculateEyeDirection(Player player) {
        var random = ThreadLocalRandom.current();

        return player.getPosition().direction()
                .rotateAroundX(random.nextDouble(-SPREAD, SPREAD))
                .rotateAroundY(random.nextDouble(-SPREAD, SPREAD))
                .rotateAroundZ(random.nextDouble(-SPREAD, SPREAD));
    }

    public static void spawnInkBall(Player player, Vec velocity) {
        var instance = player.getInstance();
        var inkBall = new BetterEntityProjectile(player, EntityType.SNOWBALL);

        var meta = (SnowballMeta) inkBall.getEntityMeta();
        meta.setItem(ItemStack.of(player.getTag(Tags.COLOR).getInkDye()));
        inkBall.setVelocity(velocity);
        inkBall.setInstance(instance, player.getPosition().add(0.0D, player.getEyeHeight(), 0.0D));
        inkBall.scheduleRemove(10, TimeUnit.SECOND);
    }

    private final Material icon;
    private final Component spawnMessage;

    Effect(Material icon, String spawnMessage) {
        this.icon = icon;
        this.spawnMessage = Server.MINI_MESSAGE.deserialize(spawnMessage);
    }

    public static Effect random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    abstract void activate(Player player, Game game);

    public Material getIcon() {
        return this.icon;
    }

    public Component getSpawnMessage() {
        return spawnMessage;
    }

    public ItemStack createItemStack() {
        return ItemStack.of(this.icon)
                .with(ItemComponent.ITEM_NAME, Component.text(this.title()))
                .withTag(Tags.EFFECT, this);
    }
}
