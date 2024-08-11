package jam.utility;

public interface Titleable {
    String name();

    default String title() {
        String[] words = this.name().split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            builder
                    .append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }

        return builder.toString().trim();
    }
}
