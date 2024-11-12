package de.checkerce.utils;

public class TokenReader {

    static final String FILENAME = "src/main/java/de/checkerce/data/discord_token";

    public static String token() {
        String data = FileReader.readFile(FILENAME);
        if (data == null) {
            throw new RuntimeException("Failed to read Token");
        }

        return data;
    }
}
