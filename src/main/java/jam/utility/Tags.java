package jam.utility;

import jam.game.Effect;
import jam.game.Game;
import jam.game.JamColor;
import jam.game.Team;
import net.minestom.server.tag.Tag;

public interface Tags {
    // Player
    Tag<Game> GAME = Tag.Transient("game");
    Tag<Team> TEAM = Tag.Transient("team");
    Tag<JamColor> COLOR = Tag.Transient("color");

    // Item
    Tag<Effect> EFFECT = Tag.String("effect").map(Effect::valueOf, Effect::name);

    // Block
    Tag<Boolean> PLAYER = Tag.Transient("player");
}
