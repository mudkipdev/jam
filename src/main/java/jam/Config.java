package jam;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;

import java.net.URI;
import java.util.UUID;

public interface Config {
    // Developer Mode
    boolean DEBUG = Boolean.getBoolean("debug");

    // Resource Pack Metadata
    ResourcePackInfo RESOURCE_PACK = ResourcePackInfo.resourcePackInfo(
            UUID.fromString("dfd11d51-8309-4afc-8061-4e171ce77600"),
            URI.create("https://download.mc-packs.net/pack/cf996189ef1c25f22d6a700d4eb64f991b19ec83.zip"),
            "cf996189ef1c25f22d6a700d4eb64f991b19ec83");

    // BungeeCord Forwarding
    String[] SECRETS = System.getProperty("forwarding", "").split(",");

    // Server Address
    String ADDRESS = System.getProperty("address", "0.0.0.0");

    // Server Port
    int PORT = Integer.getInteger("port", 25565);

    // How to Play Book
    String INSTRUCTIONS = """
            <rainbow><b>Color Clash<reset>
            Line 2
            Line 3
            Line 4
            Line 5
            Line 6
            Line 7
            Line 8
            Line 9
            Line 10
            Line 11
            Line 12
            Line 13
            Line 14 (last line)
            """.stripIndent();
}
