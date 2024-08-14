package jam;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Config {
    // Developer Mode
    boolean DEBUG = Boolean.getBoolean("debug");

    // Offline mode
    boolean OFFLINE_MODE = Boolean.getBoolean("offline-mode");

    // Enable resource pack
    boolean ENABLE_RESOURCE_PACK = Boolean.parseBoolean(System.getProperty("enable-resource-pack", "true"));

    // Commands and Permissions
    Set<UUID> DEVELOPERS = Set.of(
            UUID.fromString("0541ed27-7595-4e6a-9101-6c07f879b7b5"),  // mudkip
            UUID.fromString("7beddb4f-8574-4e21-ad45-9b6b88957725"),  // golden
            UUID.fromString("45bae2bd-1889-44f1-91f6-4f1730b4665b"), // cody
            UUID.fromString("7ec42789-1f67-4ddc-a56e-dbaa634e11d5"), // mudkip alt
            UUID.fromString("ae1472d1-3fc0-4bc4-8a11-cafe075c9d5e")); // golden alt

    // Resource Pack Metadata
    ResourcePackInfo RESOURCE_PACK = ResourcePackInfo.resourcePackInfo(
            UUID.fromString("dfd11d51-8309-4afc-8061-4e171ce77600"),
            URI.create("https://download.mc-packs.net/pack/7f3c415029eb1cc8f963be02574102c9a19afc3f.zip"),
            "7f3c415029eb1cc8f963be02574102c9a19afc3f");

    // BungeeCord Forwarding
    boolean FORWARDING = Boolean.parseBoolean(System.getProperty("forwarding", "true"));

    // Server Address
    String ADDRESS = System.getProperty("address", "0.0.0.0");

    // Server Port
    int PORT = Integer.getInteger("port", 25565);

    // How to Play Book
    List<Component> INSTRUCTIONS = Stream.of(
            """
            <dark_green><b>How to Play<reset>
            • When the hunters are released, they will try to chase down and tag/eliminate the runners.

            • The runners will win if they evade the hunters until the timer runs out.
            """.stripIndent(),

            """
            • If all runners get eliminated before the timer runs out, the hunters will win.

            • Your assigned color will change periodically, make sure to stay on the correct color of block!
            """.stripIndent(),

            """
            • Collect and use power-ups to gain an advantage over your opponents.

            <rainbow><b>Have fun!
            """.stripIndent())
                    .map(Server.MM::deserialize)
                    .collect(Collectors.toList());

    // Banlist (unfortunately we must have this)
    Set<String> BANNED_USERNAMES = Set.of("PizzaV_Bot");
}
