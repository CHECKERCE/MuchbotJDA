package de.checkerce.openAI;

import java.sql.Timestamp;

public abstract class Completion {
    public Timestamp created;

    public Completion(Timestamp created) {
        this.created = created;
    }
}
