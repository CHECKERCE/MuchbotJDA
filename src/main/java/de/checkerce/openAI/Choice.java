package de.checkerce.openAI;

public class Choice {
    public final Message message;
    public final String finishReason;
    public final double index;

    public Choice(Message message, String finishReason, double index) {
        this.message = message;
        this.finishReason = finishReason;
        this.index = index;
    }
}
