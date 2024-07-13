package jam;

public interface Config {
    String FORWARDING_SECRET = System.getProperty("FORWARDING_SECRET");
    String ADDRESS = System.getProperty("ADDRESS", "0.0.0.0");
    int PORT = Integer.getInteger("PORT", 25565);
}
