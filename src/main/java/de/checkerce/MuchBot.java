package de.checkerce;

import de.checkerce.utils.AtmosphericRandom;
import de.checkerce.utils.FileReader;
import de.checkerce.utils.MuchbotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.Objects;

public class MuchBot extends ListenerAdapter {
    JDA jda;
    final String OPENAI_API_KEY = Objects.requireNonNull(FileReader.readFile("src/main/java/de/checkerce/data/openai-api-key"))[0];

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

          ///////////////////////////////////////
         /// check if the bot should respond ///
        ///////////////////////////////////////
        {
            // check if the message was sent by the bot itself
            if (messageAuthor.equals(jda.getSelfUser()) && !MuchbotConfig.ALLOW_RESPOND_TO_OWN_MESSAGES) {
                return;
            }

            // check if the message exceeds the maximum message length
            if (ReceivedMessage.getContentDisplay().length() > MuchbotConfig.MAX_MESSAGE_LENGTH) {
                return;
            }

            // check if the bot was mentioned
            Boolean botMentioned = botMentioned(ReceivedMessage);
            if (botMentioned) {
                System.out.println("Bot Mentioned");
            }

            // check if the bot was mentioned or should respond anyway due to random chance
            if (!botMentioned && AtmosphericRandom.nextDouble() > MuchbotConfig.RESPONSE_PROBABILITY) {
                return;
            }
        }


          //////////////////////////
         /// bot should respond ///
        //////////////////////////
        System.out.println("Bot Responding...");
        messageChannel.sendTyping().queue();

        // get message History
        List<Message> messages = getMessageHistory(messageChannel);
        System.out.print("Message History: ");
        System.out.println(messages);

        // check for image attachment
        String imageURL = getImageAttachmentURL(ReceivedMessage);
        if (imageURL != null) {
            System.out.println("Image URL: " + imageURL);
        }

        // get referenced message
        Message referencedMessage = ReceivedMessage.getReferencedMessage();
        if (referencedMessage != null) {
            System.out.println("Referenced Message: " + referencedMessage.getContentDisplay());
        }

        // check if an image should be sent
        boolean sendImage = AtmosphericRandom.nextDouble() < MuchbotConfig.IMAGE_PROBABILITY;


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
        // reverse the list so the newest message is at the end
        messages = messages.reversed();
        return messages;
    }

    @Override
    public void onReady(ReadyEvent e) {
        jda = e.getJDA();
        System.out.println("bot running");
        MuchbotConfig.botName = jda.getSelfUser().getName();
        System.out.printf("logged in as %s\n\n", MuchbotConfig.botName);
    }
}
