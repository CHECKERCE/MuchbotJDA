package de.checkerce.openAI;

import java.sql.Timestamp;

public class ChatCompletion extends Completion {
    public final String id;
    public final String object;
    public final String model;
    public final Usage usage;
    public final Choice[] choices;

    public ChatCompletion(String id, String object, Timestamp created, String model, Usage usage, Choice[] choices) {
        super(created);
        this.id = id;
        this.object = object;
        this.created = created;
        this.model = model;
        this.usage = usage;
        this.choices = choices;
    }
}
