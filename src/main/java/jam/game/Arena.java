package jam.game;

import jam.Server;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.instance.IChunkLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public enum Arena {
    CITY;

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final Random RANDOM = new Random();

    public static Arena random() {
        return values()[RANDOM.nextInt(values().length)];
    }

    public IChunkLoader createLoader() {
        try (InputStream stream = Server.class.getResourceAsStream(
                "/" + this.name().toLowerCase() + ".polar")) {
            if (stream == null) {
                throw new IOException("Could not find Polar world.");
            }

            return new PolarLoader(stream);
        } catch (IOException e) {
            LOGGER.error("Failed to load Polar world.", e);
            System.exit(1);
            return null; // W code
        }
    }
}
