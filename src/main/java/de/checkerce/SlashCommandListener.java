package de.checkerce;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Objects;

public class SlashCommandListener extends ListenerAdapter {
    private final MuchBot muchBot;

    public SlashCommandListener(MuchBot muchBot) {
        this.muchBot = muchBot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "introduce" -> {
                MessageChannel channel = event.getChannel();
                event.reply(muchBot.introduce(channel)).queue();
            }
            case "devtest" -> {
                muchBot.audioTest(event);
            }
            case "say" -> {
                sayCommand(event);
            }
            default -> {
                return;
            }
        }
    }

    private void sayCommand(SlashCommandInteractionEvent event) {
        AudioChannelUnion channel = null;

        if (event.getOption("channel") != null) {
            try {
                channel = (AudioChannelUnion) Objects.requireNonNull(event.getOption("channel")).getAsChannel();
            } catch (ClassCastException e) {
                event.reply("Du musst einen Voice Channel als parameter übergeben!").setEphemeral(true).queue();
                return;
            }
        } else {
            Member member = event.getMember();

            if (member == null) {
                event.reply("Dieser Befehl kann nur auf einem Server benutzt werden").queue();
                return;
            }

            channel = muchBot.getVoiceChannelFromMember(member);
        }

        if (channel == null) {
            event.reply("Du musst in einem Voice Channel sein oder einen Voice Channel als parameter übergeben!").setEphemeral(true).queue();
            return;
        }

        String text = Objects.requireNonNull(event.getOption("text")).getAsString();

        event.deferReply().queue();

        new Thread(new PlayVoiceHandler(channel, text, muchBot, new TTSGenerateDoneHandler() {
            @Override
            public void onTTSGenerateDone(String audioURL) {
                event.getHook().sendMessage("Audio: " + audioURL).queue();
            }
        })).start();
    }
}
