package de.checkerce.audioplayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class GuildAudioManager extends AudioEventAdapter {
    /**
     * Audio player for the guild.
     */
    public final AudioPlayer player;
    private final AudioManager audioManager;

    public GuildAudioManager(AudioPlayerManager manager, AudioManager audioManager) {
        player = manager.createPlayer();
        player.addListener(this);
        this.audioManager = audioManager;
    }

    public void play(AudioTrack track) {
        player.playTrack(track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        System.out.println("Track ended");
        audioManager.closeAudioConnection();
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
