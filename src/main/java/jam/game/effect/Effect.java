package jam.game.effect;

import jam.game.Game;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

public interface Effect {
    String name();

    Material icon();

    void activate(Player player, Game game);
}
