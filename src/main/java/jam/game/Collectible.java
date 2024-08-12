package jam.game;

import jam.utility.Sounds;
import jam.utility.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Collectible extends Entity {

    public static final double COLLECT_DISTANCE = 2.5;

    private final Effect effect;
    private final Entity label;
    private final AtomicBoolean collected = new AtomicBoolean(false);

    public Collectible(Effect effect) {
        super(EntityType.ITEM_DISPLAY);
        this.effect = effect;

        this.setNoGravity(true);
        this.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setHasGlowingEffect(true);
            meta.setGlowColorOverride(JamColor.YELLOW.getTextColor().value());
            meta.setItemStack(effect.createItemStack());
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        });

        this.label = new Entity(EntityType.TEXT_DISPLAY);
        this.label.setNoGravity(true);
        this.label.editEntityMeta(TextDisplayMeta.class, meta -> {
                meta.setText(Component.text(this.effect.title(), NamedTextColor.YELLOW, TextDecoration.BOLD));
                meta.setBillboardRenderConstraints(
                        AbstractDisplayMeta.BillboardConstraints.CENTER);
        });
    }

    @Override
    public void remove() {
        super.remove();
        this.label.remove();
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        var returned = super.setInstance(instance, spawnPosition.add(0, 0.5, 0));
        this.label.setInstance(instance, spawnPosition.add(0, 1.2, 0));
        return returned;
    }

    @Override
    public void spawn() {
        super.spawn();
        this.scheduleRemove(30, TimeUnit.SECOND);
        this.getInstance().playSound(Sounds.CLICK);
        this.getInstance().sendMessage(effect.getSpawnMessage());
    }

    public void collect(@NotNull Player player) {
        if (collected.getAndSet(true)) return;

        effect.activate(player, player.getTag(Tags.GAME));
        player.playSound(Sounds.PICKUP);
        this.remove();
    }
}
