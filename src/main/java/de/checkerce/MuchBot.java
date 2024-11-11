package de.checkerce;

import de.checkerce.openAI.Completion;
import de.checkerce.openAI.OpenAI;
import de.checkerce.openAI.Role;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MuchBot extends ListenerAdapter {
    JDA jda;
    final String OPENAI_API_KEY = Objects.requireNonNull(FileReader.readFile("src/main/java/de/checkerce/data/openai-api-key"))[0];
    final OpenAI openAI = new OpenAI(OPENAI_API_KEY);

    final String MAIN_PROMPT = Objects.requireNonNull(FileReader.readFile(MuchbotConfig.MAIN_PROMPT_FILE))[0];
    final String PERSONALITY_PROMPT = Objects.requireNonNull(FileReader.readFile(MuchbotConfig.PERSONALITY_PROMPT_FILE))[0];

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

        // get response from OpenAI

        List<de.checkerce.openAI.Message> openAIMessages = new ArrayList<>();

        // main prompt message
        de.checkerce.openAI.Message mainPromptMessage = new de.checkerce.openAI.Message(Role.SYSTEM, MAIN_PROMPT);
        openAIMessages.add(mainPromptMessage);

        // personality prompt message
        de.checkerce.openAI.Message personalityPromptMessage = new de.checkerce.openAI.Message(Role.SYSTEM, PERSONALITY_PROMPT);
        openAIMessages.add(personalityPromptMessage);


        // get message history as a OpenAI Message
        StringBuilder messageHistoryString = new StringBuilder();
        messageHistoryString.append("Message History: [");
        for (Message message : messageHistory) {
            messageHistoryString.append(message.getAuthor().getName());
            messageHistoryString.append(": ");
            messageHistoryString.append(message.getContentDisplay().replace("\n", " "));
            messageHistoryString.append("; ");
        }
        messageHistoryString.append("]");

        de.checkerce.openAI.Message messageHistoryMessage = new de.checkerce.openAI.Message(Role.USER, messageHistoryString.toString());
        openAIMessages.add(messageHistoryMessage);

        // get referenced message as a OpenAI Message if it exists
        if (referencedMessage != null) {
            String _msg = "Der Benutzer hat auf folgende Nachricht von " + referencedMessage.getAuthor().getName() + " geantwortet: " + referencedMessage.getContentDisplay();
            de.checkerce.openAI.Message referencedMessageMessage = new de.checkerce.openAI.Message(Role.USER, _msg);
            openAIMessages.add(referencedMessageMessage);
        }

        // create image message if an image was sent
        if (imageURL != null) {
            de.checkerce.openAI.ImageMessage imageMessage = new de.checkerce.openAI.ImageMessage(Role.USER,"this image was attached to the message" , imageURL);
            openAIMessages.add(imageMessage);
        }

        // create message from the received message
        String _receivedMessageStr = ReceivedMessage.getAuthor().getName() + ": " + ReceivedMessage.getContentDisplay();
        de.checkerce.openAI.Message receivedMessageMessage = new de.checkerce.openAI.Message(Role.USER, _receivedMessageStr);
        openAIMessages.add(receivedMessageMessage);

        try {
            de.checkerce.openAI.Message[] _msgs = openAIMessages.toArray(new de.checkerce.openAI.Message[0]);
            Completion completion = openAI.chatCompletion(_msgs, MuchbotConfig.OPENAI_STANDARD_MODEL, MuchbotConfig.MAX_ANSWER_TOKENS);
            String response = completion.choices[0].message.content;
            System.out.println("Response: " + response);

            // send response
            messageChannel.sendMessage(response).queue();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


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

    @Override
    public void onReady(ReadyEvent e) {
        jda = e.getJDA();
        System.out.println("bot running");
        MuchbotConfig.botName = jda.getSelfUser().getName();
        System.out.printf("logged in as %s\n\n", MuchbotConfig.botName);
    }
}
