package jam.utility;

import jam.Config;
import jam.Server;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;

public interface JamConditions {
    CommandCondition DEVELOPER = (sender, commandString) -> {
        if (sender instanceof Player player) {
            return Config.DEVELOPERS.contains(player.getUuid());
        } else {
            return true;
        }
    };

    CommandCondition LOBBY = (sender, commandString) -> {
        if (commandString == null) {
            return true;
        }

        return sender instanceof Player player
                && Server.getLobby().getInstance().equals(player.getInstance());
    };
}
