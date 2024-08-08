package jam;

public interface Config {
    // BungeeCord Forwarding
    String[] SECRETS = System.getProperty("forwarding", "").split(",");

    // Server Address
    String ADDRESS = System.getProperty("address", "0.0.0.0");

    // Server Port
    int PORT = Integer.getInteger("port", 25565);
}
