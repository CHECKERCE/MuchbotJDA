package de.checkerce.openAI;

public class Message {
    public final Role role;
    public final String content;

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"role\": \"").append(role).append("\", \"content\": \"").append(content).append("\"}");
        return sb.toString();
    }
}
