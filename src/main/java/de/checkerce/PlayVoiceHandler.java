package de.checkerce;

import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

public class PlayVoiceHandler implements Runnable{
    private final AudioChannelUnion channel;
    private final String message;
    private final MuchBot muchBot;
    private TTSGenerateDoneHandler doneHandler;

    public PlayVoiceHandler(AudioChannelUnion channel, String message, MuchBot muchBot) {
        this.channel = channel;
        this.message = message;
        this.muchBot = muchBot;
    }

    public PlayVoiceHandler(AudioChannelUnion channel, String message, MuchBot muchBot, TTSGenerateDoneHandler doneHandler) {
        this.channel = channel;
        this.message = message;
        this.muchBot = muchBot;
        this.doneHandler = doneHandler;
    }

    @Override
    public void run() {
            String audioURL = FakeYouTTS.generateAudioRandomVoice(message);
            muchBot.playAudioFileInChannel(channel, audioURL);
            if (doneHandler != null) {
                doneHandler.onTTSGenerateDone(audioURL);
            }
    }
}
