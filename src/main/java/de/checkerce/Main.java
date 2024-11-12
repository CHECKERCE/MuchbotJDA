package de.checkerce;


import de.checkerce.utils.TokenReader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.EnumSet;

public class Main {
    public static void main(String[] args) {
        MuchBot muchBot = new MuchBot();
        JDA jda = JDABuilder.createLight(TokenReader.token(), EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS))
                .addEventListeners(muchBot, new SlashCommandListener(muchBot))
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        CommandListUpdateAction commands = jda.updateCommands();

        // Add all commands on this action instance
        commands.addCommands(
                Commands.slash("introduce", "Der bot stellt sich vor🎙️"),
                Commands.slash("devtest", "TestCommand for developers").setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("say", "Der Bot sagt etwas")
                        .addOption(OptionType.STRING, "text", "Der Text, den der Bot sagen soll", true)
                        .addOption(OptionType.CHANNEL, "channel", "Der Channel, in dem der Bot sprechen soll", false)
        );

        // send commands to discord
        commands.queue();
    }
}