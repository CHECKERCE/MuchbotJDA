package de.checkerce.openAI;

public class ImageMessage extends Message {
    public final String imageURL;

    public ImageMessage(Role role, String content, String imageURL) {
        super(role, content);
        this.imageURL = imageURL;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"role\": \"").append(role).append("\", \"content\": [{\"type\": \"text\", \"text\": \"")
                .append(content).append("\"}, {\"type\": \"image_url\", \"image_url\": {\"url\": \"")
                .append(imageURL).append("\", \"detail\": \"auto\"}}]}");
        return sb.toString();
    }
}
