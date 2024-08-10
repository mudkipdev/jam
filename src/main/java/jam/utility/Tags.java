package jam.utility;

import jam.game.Team;
import net.minestom.server.tag.Tag;

public interface Tags {
    Tag<Team> TEAM = Tag.Transient("team");
}
