package de.checkerce.openAI;

import java.sql.Timestamp;

public class Completion {
    public final String id;
    public final String object;
    public final Timestamp created;
    public final String model;
    public final Usage usage;
    public final Choice[] choices;

    public Completion(String id, String object, Timestamp created, String model, Usage usage, Choice[] choices) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.model = model;
        this.usage = usage;
        this.choices = choices;
    }
}
