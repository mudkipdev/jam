package jam;

import net.kyori.adventure.resource.ResourcePackInfo;

import java.net.URI;
import java.util.UUID;

public interface Config {
    // Developer Mode
    boolean DEBUG = Boolean.getBoolean("debug");

    // Resource Pack Metadata
    ResourcePackInfo RESOURCE_PACK = ResourcePackInfo.resourcePackInfo(
            UUID.fromString("dfd11d51-8309-4afc-8061-4e171ce77600"),
            URI.create("https://download.mc-packs.net/pack/5059b0c5fa0f6ffe579b1a07c14e085aca64605e.zip"),
            "5059b0c5fa0f6ffe579b1a07c14e085aca64605e");

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
