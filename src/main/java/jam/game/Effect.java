package jam.game;

import jam.Server;
import jam.utility.*;
import jam.utility.BetterEntityProjectile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.time.TimeUnit;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum Effect implements Titleable {
    INK_BLASTER(Material.WHITE_CANDLE, """
            <newline><prefix><gray>An <yellow><bold>Ink Blaster<reset><gray> has spawned in a random spot!
            <prefix>Shoot ink to <light_purple>change the colors of blocks<gray>!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            var itemStack = player.getTag(Tags.COLOR).getInkBlaster();
            player.getInventory().addItemStack(itemStack);
        }
    },
    TNT(Material.TNT, """
            <newline><prefix><gray>A <red><bold>TNT<reset><gray> has spawned in a random spot!
            <prefix>Collect and place it to <light_purple>explode a region into its color!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            var itemStack = JamColor.random().getTnt();
            player.getInventory().addItemStack(itemStack);
        }

        @Override
        public String title() {
            return "TNT";
        }
    },
    HEALTH_PACK(Material.GOLDEN_APPLE, """
            <newline><prefix><gray>A <yellow><bold>Health Pack<reset><gray> has spawned in a random spot!
            <prefix>Collect it to <light_purple>receive a few bonus hearts!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            player.setAdditionalHearts(player.getAdditionalHearts() + 10);
            player.setHealth(player.getHealth() + 10);
        }
    },
    ENDER_PEARL(Material.ENDER_PEARL, """
            <newline><prefix><gray>An <light_purple><bold>Ender Pearl<reset><gray> has spawned in a random spot!
            <prefix>Collect and throw it to <light_purple>teleport where it lands!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            player.getInventory().addItemStack(PEARL_ITEM);
        }
    },
    SPLASH_COLORBLINDNESS(ItemStack.of(Material.SPLASH_POTION).with(ItemComponent.POTION_CONTENTS, new PotionContents(null, new Color(150, 150, 150), List.of())),
            """
            <newline><prefix><gray>A <white><bold>Colorblindness Potion</bold><gray> has spawned in a random spot!
            <prefix>Collect and throw it to <light_purple>mess up other people's vision!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            var item = createItemStack()
                    .with(ItemComponent.HIDE_ADDITIONAL_TOOLTIP)
                    .with(ItemComponent.LORE, List.of(Component.text("Colorblindness (0:15)", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)));
            player.getInventory().addItemStack(item);
        }

        @Override
        public String title() {
            return "Splash Potion of Colorblindness";
        }
    },
    MAGIC_PATH(Material.DIAMOND_SHOVEL, """
            <newline><prefix><gray>A <blue><bold>Magic Path</bold><gray> has spawned in a random spot!
            <prefix>Collect it for a temporary <light_purple>trail of your color!<newline>
            """.trim()) {
        @Override
        public void activate(Player player, Game game) {
            player.addEffect(new Potion(PotionEffect.HERO_OF_THE_VILLAGE, (byte) 0, 15 * 20, Potion.ICON_FLAG));
        }
    };

    public static final ItemStack PEARL_ITEM = ItemStack.of(Material.ENDER_PEARL)
            .withTag(Tags.EFFECT, ENDER_PEARL)
            .with(ItemComponent.ITEM_NAME, Component.text("Ender Pearl", NamedTextColor.LIGHT_PURPLE));

    public static final int COLORBLINDNESS_DURATION = 15 * 20;

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

    private final ItemStack icon;
    private final Component spawnMessage;

    Effect(ItemStack icon, Component spawnMessage) {
        this.icon = icon;
        this.spawnMessage = spawnMessage;
    }

    Effect(ItemStack icon, String spawnMessage) {
        this(icon, Server.MM.deserialize(spawnMessage));
    }

    Effect(Material icon, String spawnMessage) {
        this(ItemStack.of(icon), Server.MM.deserialize(spawnMessage));
    }

    public static Effect random() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)];
    }

    public abstract void activate(Player player, Game game);

    public ItemStack getIcon() {
        return icon;
    }

    public Component getSpawnMessage() {
        return spawnMessage;
    }

    public ItemStack createItemStack() {
        return this.icon
                .with(ItemComponent.ITEM_NAME, Component.text(this.title()))
                .withTag(Tags.EFFECT, this);
    }
}
