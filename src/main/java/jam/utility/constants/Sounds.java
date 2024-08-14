package jam.utility.constants;

import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public interface Sounds {
    Sound CLICK = Sound.sound(
            SoundEvent.UI_BUTTON_CLICK.key(),
            Sound.Source.MASTER,
            0.5F, 1.2F);

    Sound TELEPORT = Sound.sound(
            SoundEvent.ENTITY_ENDERMAN_TELEPORT,
            Sound.Source.MASTER,
            1.0F, 1.0F);

    Sound NOTE = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_HARP,
            Sound.Source.MASTER,
            1.0F, 1.0F);

    Sound DRAGON = Sound.sound(
            SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
            Sound.Source.MASTER,
            0.7F, 0.9F);

    Sound FIREWORK_WIN = Sound.sound(
            SoundEvent.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
            Sound.Source.MASTER,
            1.0F, 1.2F);

    Sound DEATH_LOSE = Sound.sound(
            SoundEvent.BLOCK_BEACON_DEACTIVATE,
            Sound.Source.MASTER,
            1.0F, 1.2F);

    Sound PICKUP = Sound.sound(
            SoundEvent.ENTITY_ITEM_PICKUP,
            Sound.Source.MASTER,
            1.0F, 1.2F);

    Sound GHAST_SHOOT = Sound.sound(
            SoundEvent.ENTITY_GHAST_SHOOT,
            Sound.Source.MASTER,
            0.5F, 2.0F);

    Supplier<Sound> PEARL_THROW = () -> Sound.sound(
            SoundEvent.ENTITY_ENDER_PEARL_THROW,
            Sound.Source.NEUTRAL,
            0.5F, 0.4F / (ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f));

    Supplier<Sound> SPLASH_THROW = () -> Sound.sound(
            SoundEvent.ENTITY_SPLASH_POTION_THROW,
            Sound.Source.NEUTRAL,
            0.5F, 1.0F);

    Float2ObjectFunction<Sound> LAVA_HISS = volume -> Sound.sound(
            SoundEvent.BLOCK_LAVA_EXTINGUISH,
            Sound.Source.NEUTRAL,
            volume, 1.0F
    );

    Sound LARGE_BLAST = Sound.sound(
            SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
            Sound.Source.NEUTRAL,
            0.5F, 1.0F);
}
