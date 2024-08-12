package jam.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public interface Components {
    Component PREFIX = Component.text("» ", NamedTextColor.YELLOW, TextDecoration.BOLD);

    String PREFIX_MM = "<yellow><bold>» </bold><gray>";



}
