package de.checkerce;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.checkerce.audioplayer.GuildAudioManager;
import de.checkerce.openAI.*;
import de.checkerce.openAI.Role;
import de.checkerce.utils.AtmosphericRandom;
import de.checkerce.utils.FileReader;
import de.checkerce.utils.MuchbotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MuchBot extends ListenerAdapter {
    private JDA jda;

    final String OPENAI_API_KEY = Objects.requireNonNull(FileReader.readFile("src/main/java/de/checkerce/data/openai-api-key"));
    final OpenAI openAI = new OpenAI(OPENAI_API_KEY);
    private final Map<Long, GuildAudioManager> audioManagers = new java.util.HashMap<>();

    AudioPlayerManager playerManager;


    final String MAIN_PROMPT = Objects.requireNonNull(FileReader.readFile(MuchbotConfig.MAIN_PROMPT_FILE)).replace("<botName>", MuchbotConfig.botName).replace("\"", "\\\"");
    final String PERSONALITY_PROMPT = Objects.requireNonNull(FileReader.readFile(MuchbotConfig.PERSONALITY_PROMPT_FILE)).replace("\"", "\\\"");

    @Override
    public void onReady(ReadyEvent e) {
        jda = e.getJDA();
        System.out.println("bot running");
        MuchbotConfig.botName = jda.getSelfUser().getName();
        System.out.printf("logged in as %s\n\n", MuchbotConfig.botName);
        playerManager = new DefaultAudioPlayerManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //get message information
        Message ReceivedMessage = event.getMessage();
        MessageChannel messageChannel = event.getChannel();
        User messageAuthor = event.getAuthor();

        //print message to console
        System.out.printf("received Message: [%s] %#s: %s\n",
                messageChannel.getName(),
                messageAuthor,
                ReceivedMessage.getContentDisplay());

        handleReceivedMessage(ReceivedMessage, messageChannel, messageAuthor);

        super.onMessageReceived(event);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        //get message information
        Message ReceivedMessage = event.getMessage();
        MessageChannel messageChannel = event.getChannel();
        User messageAuthor = event.getAuthor();

        //print message to console
        System.out.printf("updated Message: [%s] %#s: %s\n",
                messageChannel.getName(),
                messageAuthor,
                ReceivedMessage.getContentDisplay());

        //only respond to edited messages if the bot is mentioned
        if (botMentioned(ReceivedMessage)) {
            handleReceivedMessage(ReceivedMessage, messageChannel, messageAuthor);
        }
    }

    private void handleReceivedMessage(Message ReceivedMessage, MessageChannel messageChannel, User messageAuthor) {
        ///////////////////////////////////////
        /// check if the bot should respond ///
        ///////////////////////////////////////
        {
            // check if the message was sent by the bot itself
            if (messageAuthor.equals(jda.getSelfUser()) && !MuchbotConfig.ALLOW_RESPOND_TO_OWN_MESSAGES) {
                System.out.println("Ignoring Own Message");
                return;
            }

            // check if the message exceeds the maximum message length
            if (ReceivedMessage.getContentDisplay().length() > MuchbotConfig.MAX_MESSAGE_LENGTH) {
                System.out.println("Ignoring Message: Message too long");
                return;
            }

            // check if the bot was mentioned
            Boolean botMentioned = botMentioned(ReceivedMessage);
            if (botMentioned) {
                System.out.println("Bot Mentioned");
            } else {
                System.out.println("Bot Not Mentioned");
            }

            // check if the bot was mentioned or should respond anyway due to random chance
            if (!botMentioned && AtmosphericRandom.nextDouble() > MuchbotConfig.RESPONSE_PROBABILITY) {
                System.out.println("Ignoring Message: Random Chance failed");
                return;
            }
        }


        //////////////////////////
        /// bot should respond ///
        //////////////////////////
        System.out.println("Bot Responding...");
        messageChannel.sendTyping().queue();

        // get message History
        List<Message> messageHistory = getMessageHistory(messageChannel);
        System.out.print("Message History: ");
        System.out.println(messageHistory);

        // check for image attachment
        String imageAttachmentURL = getImageAttachmentURL(ReceivedMessage);
        if (imageAttachmentURL != null) {
            System.out.println("Image URL: " + imageAttachmentURL);
        }

        // get referenced message
        Message referencedMessage = ReceivedMessage.getReferencedMessage();
        if (referencedMessage != null) {
            System.out.println("Referenced Message: " + referencedMessage.getContentDisplay());
        }

        // check if the referenced message has an image attachment
        String referencedImageURL = null;
        if (referencedMessage != null) {
            referencedImageURL = getImageAttachmentURL(referencedMessage);
            if (referencedImageURL != null) {
                System.out.println("Referenced Image URL: " + referencedImageURL);
            }
        }

        // check if an image should be sent
        boolean sendImage = AtmosphericRandom.nextDouble() < MuchbotConfig.IMAGE_PROBABILITY;

        // check if audio should be played
        Member member = ReceivedMessage.getMember();

        AudioChannelUnion voiceChannel = null;

        if (member != null) {
            voiceChannel = getVoiceChannelFromMember(member);
        }

        boolean playAudio = voiceChannel != null && AtmosphericRandom.nextDouble() < MuchbotConfig.AUDIO_PROBABILITY;

        ////////////////////////////////
        /// get response from OpenAI ///
        ////////////////////////////////

        List<de.checkerce.openAI.Message> openAIMessages = getDefaultPromptMessages();


        // get message history as a OpenAI Message
        StringBuilder messageHistoryString = new StringBuilder();
        messageHistoryString.append("Message History:\n");
        for (Message message : messageHistory) {
            messageHistoryString.append(message.getAuthor().getName());
            messageHistoryString.append(": ");
            messageHistoryString.append(message.getContentDisplay().replace("\n", " "));
            messageHistoryString.append("\n");
        }

        de.checkerce.openAI.Message messageHistoryMessage = new de.checkerce.openAI.Message(Role.USER, messageHistoryString.toString());
        openAIMessages.add(messageHistoryMessage);

        // get referenced message as a OpenAI Message if it exists
        if (referencedMessage != null) {
            String _msg = "Der Benutzer hat auf folgende Nachricht von " + referencedMessage.getAuthor().getName() + " geantwortet: " + referencedMessage.getContentDisplay();
            de.checkerce.openAI.Message referencedMessageMessage = new de.checkerce.openAI.Message(Role.USER, _msg);
            openAIMessages.add(referencedMessageMessage);

            // create referenced image message if an image was sent
            if (referencedImageURL != null) {
                ImageMessage referencedImageMessage = new ImageMessage(Role.USER,"die nachricht worauf geantwortet wurde enthielt dieses Bild" , referencedImageURL);
                openAIMessages.add(referencedImageMessage);
            }
        }

        // create image message if an image was sent
        if (imageAttachmentURL != null) {
            ImageMessage imageMessage = new ImageMessage(Role.USER,"this image was attached to the message" , imageAttachmentURL);
            openAIMessages.add(imageMessage);
        }

        // create message from the received message
        String _receivedMessageStr = ReceivedMessage.getAuthor().getName() + ": " + ReceivedMessage.getContentDisplay();
        de.checkerce.openAI.Message receivedMessageMessage = new de.checkerce.openAI.Message(Role.USER, _receivedMessageStr);
        openAIMessages.add(receivedMessageMessage);

        try {
            de.checkerce.openAI.Message[] _msgs = openAIMessages.toArray(new de.checkerce.openAI.Message[0]);
            ChatCompletion chatCompletion = openAI.chatCompletion(_msgs, MuchbotConfig.OPENAI_STANDARD_MODEL, MuchbotConfig.MAX_ANSWER_TOKENS);
            String response = chatCompletion.choices[0].message.content;
            System.out.println("Response: " + response);

            // play audio if it should be played

            if (playAudio) {
                PlayVoiceHandler p = new PlayVoiceHandler(voiceChannel, response, this);
                new Thread(p).start();
            }

            // check if an image should be sent
            if (sendImage) {
                String imagePrompt = getImagePrompt(response);
                ImageCompletion imageCompletion = openAI.imageCompletion(imagePrompt, MuchbotConfig.OPENAI_IMAGE_MODEL, 1, MuchbotConfig.IMAGE_DIMENSIONS);
                URL imageURL = imageCompletion.images[0];

                InputStream in = imageURL.openStream();

                // send response with image
                messageChannel.sendMessage(response).addFiles(FileUpload.fromData(in, "image.png")).queue();
            } else {
                // send response
                messageChannel.sendMessage(response).queue();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull List<de.checkerce.openAI.Message> getDefaultPromptMessages() {
        List<de.checkerce.openAI.Message> openAIMessages = new ArrayList<>();

        // main prompt message
        de.checkerce.openAI.Message mainPromptMessage = new de.checkerce.openAI.Message(Role.SYSTEM, MAIN_PROMPT);
        openAIMessages.add(mainPromptMessage);

        // personality prompt message
        de.checkerce.openAI.Message personalityPromptMessage = new de.checkerce.openAI.Message(Role.SYSTEM, PERSONALITY_PROMPT);
        openAIMessages.add(personalityPromptMessage);
        return openAIMessages;
    }

    private String getImagePrompt(String msg) throws IOException, InterruptedException {
        String prompt = MuchbotConfig.IMAGE_GENERATION_PROMPT_PROMPT + msg;
        de.checkerce.openAI.Message imagePromptPrompt = new de.checkerce.openAI.Message(Role.SYSTEM, prompt);

        ChatCompletion chatCompletion = openAI.chatCompletion(new de.checkerce.openAI.Message[]{imagePromptPrompt}, MuchbotConfig.OPENAI_STANDARD_MODEL, MuchbotConfig.MAX_ANSWER_TOKENS);
        return chatCompletion.choices[0].message.content.replace("\n", " ").replace("\"", "\\\"");
    }

    private Boolean botMentioned(Message message) {
        boolean mentioned = false;
        Mentions mentions = message.getMentions();
        boolean mentionCriteria1 = mentions.isMentioned(jda.getSelfUser());
        boolean mentionCriteria2 = message.getContentRaw().toLowerCase().contains(MuchbotConfig.botName.toLowerCase());
        mentioned = mentionCriteria1 || mentionCriteria2;
        return mentioned;
    }

    private String getImageAttachmentURL(Message message) {
        if (message.getAttachments().isEmpty()) {
            return null;
        }
        String URL = message.getAttachments().getFirst().getUrl();
        String fileName = message.getAttachments().getFirst().getFileName();

        //check if the URL is an image
        boolean isImage = false;
        for (String imageExtension : MuchbotConfig.IMAGE_EXTENSIONS) {
            if (fileName.endsWith(imageExtension)) {
                isImage = true;
                break;
            }
        }
        if (!isImage) {
            return null;
        }
        return URL;
    }

    private List<Message> getMessageHistory(MessageChannel channel) {
        // get Message History
        List<Message> messages = channel.getHistory().retrievePast(MuchbotConfig.MAX_MESSAGE_HISTORY).complete();

        // remove the last message as it is the message that triggered the bot
        messages.removeFirst();

        // reverse the list so the newest message is at the end
        messages = messages.reversed();
        return messages;
    }

    public String introduce(MessageChannel channel) {
        List<de.checkerce.openAI.Message> Messages = getDefaultPromptMessages();

        de.checkerce.openAI.Message message = new de.checkerce.openAI.Message(Role.USER, "Stelle dich ausführlich vor.");
        Messages.add(message);

        try {
            ChatCompletion chatCompletion = openAI.chatCompletion(Messages.toArray(new de.checkerce.openAI.Message[0]), MuchbotConfig.OPENAI_STANDARD_MODEL, MuchbotConfig.MAX_ANSWER_TOKENS);
            String response = chatCompletion.choices[0].message.content;
            System.out.println("Response: " + response);
            return response;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void audioTest(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        assert member != null;
        AudioChannelUnion voiceChannel = getVoiceChannelFromMember(member);
        answerInVoiceChannel(voiceChannel, "test");

        event.reply("test concluded").queue();
    }

    public void answerInVoiceChannel(AudioChannelUnion voiceChannel, String message) {
        PlayVoiceHandler p = new PlayVoiceHandler(voiceChannel, message, this);
        new Thread(p).start();
    }

    public @Nullable AudioChannelUnion getVoiceChannelFromMember(Member member) {
        GuildVoiceState voiceState = member.getVoiceState();

        if (voiceState == null) {
            System.out.println("Error: voiceState is null");
            return null;
        }

        return voiceState.getChannel();
    }

    public void playAudioFileInChannel(AudioChannelUnion voiceChannel, String audioFilePath) {
        AudioManager audioManager = Objects.requireNonNull(voiceChannel.getGuild()).getAudioManager();

        GuildAudioManager guildAudioManager = getGuildAudioManager(voiceChannel.getGuild(), audioManager);
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);


        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }

        playerManager.loadItem(audioFilePath, new AudioLoadResultHandler() {
           @Override
           public void trackLoaded(AudioTrack track) {
               System.out.println("track loaded");
               guildAudioManager.play(track);
           }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                System.out.println("playlist loaded");
            }

            @Override
           public void noMatches() {
               System.out.println("no matches");
           }

           @Override
           public void loadFailed(FriendlyException exception) {
                System.out.println("load failed");
           }
       });
    }

    private synchronized GuildAudioManager getGuildAudioManager(Guild guild, AudioManager audioManager) {
        long guildId = Long.parseLong(guild.getId());
        GuildAudioManager manager = audioManagers.get(guildId);

        if (manager == null) {
            manager = new GuildAudioManager(playerManager, audioManager);
            audioManagers.put(guildId, manager);
        }

        guild.getAudioManager().setSendingHandler(manager.getSendHandler());

        return manager;
    }
}
