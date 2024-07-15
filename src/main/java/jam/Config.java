package jam;

public interface Config {
    // mattw said we're using legacy forwarding :despair:
    String[] SECRETS = System.getProperty("forwarding", "").split(",");
    String ADDRESS = System.getProperty("address", "0.0.0.0");
    int PORT = Integer.getInteger("port", 25565);
}
