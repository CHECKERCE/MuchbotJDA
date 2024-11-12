package de.checkerce.openAI;

import java.net.URL;
import java.sql.Timestamp;

public class ImageCompletion extends Completion{
    public URL[] images;

    public ImageCompletion(Timestamp created, URL[] images) {
        super(created);
        this.images = images;
    }
}
