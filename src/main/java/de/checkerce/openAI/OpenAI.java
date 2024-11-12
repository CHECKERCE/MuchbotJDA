package de.checkerce.openAI;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenAI {
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String IMAGE_COMPLETIONS_URL = "https://api.openai.com/v1/images/generations";
    private final String OPENAI_API_KEY;

    public OpenAI(String OPENAI_API_KEY) {
        this.OPENAI_API_KEY = OPENAI_API_KEY;
    }

    public ChatCompletion chatCompletion(Message[] messages, String model) throws IOException, InterruptedException {
        return chatCompletion(messages, model, 0);
    }

    public ChatCompletion chatCompletion(Message[] messages, String model, int maxTokens) throws IOException, InterruptedException {

        String body = chatCompletionBody(messages, model, maxTokens);

        String response = PostRequest.createRequest(CHAT_COMPLETIONS_URL, body, OPENAI_API_KEY);
        System.out.println(response);

        assert response != null;
        JSONObject jsonResponse = new JSONObject(response);

        String id = jsonResponse.getString("id");
        String object = jsonResponse.getString("object");
        Timestamp created = new Timestamp(jsonResponse.getLong("created"));
        String completionModel = jsonResponse.getString("model");

        JSONObject usage = jsonResponse.getJSONObject("usage");
        int promptTokens = usage.getInt("prompt_tokens");
        int completionTokens = usage.getInt("completion_tokens");
        int totalTokens = usage.getInt("total_tokens");

        JSONObject completionTokenDetails = usage.getJSONObject("completion_tokens_details");
        int reasoningTokens = completionTokenDetails.getInt("reasoning_tokens");
        int acceptedPredictionTokens = completionTokenDetails.getInt("accepted_prediction_tokens");
        int rejectedPredictionTokens = completionTokenDetails.getInt("rejected_prediction_tokens");

        completion_token_details completionTokenDetailsObject = new completion_token_details(reasoningTokens, acceptedPredictionTokens, rejectedPredictionTokens);
        Usage usageObject = new Usage(promptTokens, completionTokens, totalTokens, completionTokenDetailsObject);

        JSONArray choices = jsonResponse.getJSONArray("choices");
        List<Choice> choicesList = new ArrayList<>();

        choices.forEach(choice -> {
            JSONObject choiceObject = (JSONObject) choice;

            JSONObject messageObject = choiceObject.getJSONObject("message");
            Role role = Role.fromString(messageObject.getString("role"));
            String content = messageObject.getString("content");
            Message message = new Message(role, content);

            String finishReason = choiceObject.getString("finish_reason");
            double index = choiceObject.getDouble("index");

            choicesList.add(new Choice(message, finishReason, index));
        });

        return new ChatCompletion(id, object, created, completionModel, usageObject, choicesList.toArray(new Choice[0]));
    }

    private String chatCompletionBody(Message[] messages, String model) {
        return chatCompletionBody(messages, model, 0);
    }

    private String chatCompletionBody(Message[] messages, String model, int maxTokens) {
        StringBuilder body = new StringBuilder("{\"model\": \"");
        body.append(model);
        body.append("\", \"messages\": ");
        body.append(Arrays.toString(messages));
        if (maxTokens > 0) {
            body.append(", \"max_tokens\": ");
            body.append(maxTokens);
        }
        body.append("}");

        return body.toString();
    }

    public ImageCompletion imageCompletion(String prompt, String model, int n, int[] size) throws IOException, InterruptedException {

        if (size.length != 2) {
            throw new IllegalArgumentException("Size must be an array of length 2");
        }

        String body = imageCompletionBody(prompt, model, n, size);

        String response = PostRequest.createRequest(IMAGE_COMPLETIONS_URL, body, OPENAI_API_KEY);
        System.out.println(response);

        assert response != null;
        JSONObject jsonResponse = new JSONObject(response);

        Timestamp created = new Timestamp(jsonResponse.getLong("created"));
        JSONArray data = jsonResponse.getJSONArray("data");

        List<URL> ImageURLs = new ArrayList<>();
        data.forEach(datum -> {
            JSONObject dataObject = (JSONObject) datum;
            String url = dataObject.getString("url");
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            try {
                ImageURLs.add(uri.toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });

        return new ImageCompletion(created, ImageURLs.toArray(new URL[0]));
    }

    private String imageCompletionBody(String prompt, String model, int n, int[] size) {

        if (size.length != 2) {
            throw new IllegalArgumentException("Size must be an array of length 2");
        }

        StringBuilder body = new StringBuilder();
        body.append("{\"prompt\": \"");
        body.append(prompt);
        body.append("\", \"model\": \"");
        body.append(model);
        body.append("\", \"n\": ");
        body.append(n);
        body.append(", \"size\": \"");
        body.append(size[0]);
        body.append("x");
        body.append(size[1]);
        body.append("\"}");

        return body.toString();
    }



}
