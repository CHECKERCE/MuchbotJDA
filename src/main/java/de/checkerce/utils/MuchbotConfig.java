package de.checkerce.utils;

public class MuchbotConfig {
    public static String[] IMAGE_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "webp"};

    public static final int MAX_MESSAGE_LENGTH = 400;
    public static final int MAX_ANSWER_TOKENS = 150;
    public static final int MAX_MESSAGE_HISTORY = 25;

    public static final float RESPONSE_PROBABILITY = 0.05f;
    public static final float IMAGE_PROBABILITY = 0.05f;
    public static final float AUDIO_PROBABILITY = 0.05f;

    public static final int[] IMAGE_DIMENSIONS = {1024, 1024};

    public static final Boolean ALLOW_RESPOND_TO_OWN_MESSAGES = false;

    public static final String OPENAI_STANDARD_MODEL = "gpt-4o";
    public static final String OPENAI_IMAGE_MODEL = "dall-e-3";

    public static String botName = "Muchbot";

    public static final String MAIN_PROMPT_FILE = "src/main/java/de/checkerce/data/mainPrompt.txt";
    public static final String PERSONALITY_PROMPT_FILE = "src/main/java/de/checkerce/data/personality.txt";
    public static final String IMAGE_GENERATION_PROMPT_PROMPT = "Du sendest diese Discord nachricht, erstelle einen kurzen prompt womit ein bild generiert wird welches an die nachricht angehängt wird. antworte NUR mit dem prompt\n\nNachricht: ";
}
