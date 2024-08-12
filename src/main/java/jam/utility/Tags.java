package jam.utility;

import jam.game.Effect;
import jam.game.Game;
import jam.game.JamColor;
import jam.game.Team;
import net.minestom.server.tag.Tag;

public interface Tags {
    Tag<Game> GAME = Tag.Transient("game");
    Tag<Team> TEAM = Tag.Transient("team");
    Tag<JamColor> COLOR = Tag.Transient("color");
    Tag<Effect> EFFECT = Tag.Transient("effect");
}
