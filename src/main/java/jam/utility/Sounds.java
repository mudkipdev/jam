package jam.utility;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;

public interface Sounds {
    Sound CLICK = Sound.sound(
            SoundEvent.UI_BUTTON_CLICK.key(),
            Sound.Source.MASTER,
            0.5F, 1.2F
    );

    Sound NOTE = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_HARP,
            Sound.Source.MASTER,
            1.0F, 1.0F);

    Sound DRAGON = Sound.sound(
            SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
            Sound.Source.MASTER,
            0.7F, 0.9F
    );

    Sound FIREWORK_WIN = Sound.sound(
            SoundEvent.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
            Sound.Source.MASTER,
            1.0F, 1.2F
    );

    Sound DEATH_LOSE = Sound.sound(
            SoundEvent.BLOCK_BEACON_DEACTIVATE,
            Sound.Source.MASTER,
            1.0F, 1.2F
    );

}
