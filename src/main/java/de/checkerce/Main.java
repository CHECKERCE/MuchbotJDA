package de.checkerce;


import de.checkerce.utils.TokenReader;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.EnumSet;

public class Main {
    public static void main(String[] args) {
        JDABuilder.createLight(TokenReader.token(), EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .addEventListeners(new MuchBot())
                .build();
    }
}