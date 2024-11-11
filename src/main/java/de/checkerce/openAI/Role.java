package de.checkerce.openAI;

public enum Role {
    USER("user"),
    SYSTEM("system");

    public final String name;

    Role(String name) {
        this.name = name;
    }

    public static Role fromString(String name) {
        for (Role role : Role.values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
