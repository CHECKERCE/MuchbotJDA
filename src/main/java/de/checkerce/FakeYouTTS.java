package de.checkerce;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class FakeYouTTS {
    private static final String FAKE_YOU_GENERATION_URL = "https://api.fakeyou.com/tts/inference";
    private static final String FAKE_YOU_VOICE_LIST_URL = "https://api.fakeyou.com/tts/list";
    private static final String STORAGE_URL = "https://storage.googleapis.com/vocodes-public";
    private static List<JSONObject> voices = null;

    private static List<JSONObject> getVoiceList(List<String> languages) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(FAKE_YOU_VOICE_LIST_URL))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject responseJson = new JSONObject(response.body());
        JSONArray models = responseJson.getJSONArray("models");

        List<JSONObject> _voices = new ArrayList<>();
        for (int i = 0; i < models.length(); i++) {
            JSONObject voice = models.getJSONObject(i);
            if (languages == null || languages.contains(voice.getString("ietf_language_tag"))) {
                _voices.add(voice);
            }
        }

        return _voices;
    }

    private static String generateAudio(String modelToken, String text) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        JSONObject payload = new JSONObject()
                .put("tts_model_token", modelToken)
                .put("uuid_idempotency_token", UUID.randomUUID().toString())
                .put("inference_text", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(FAKE_YOU_GENERATION_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject responseJson = new JSONObject(response.body());
        String inferenceJobToken = responseJson.getString("inference_job_token");

        String jobUrl = "https://api.fakeyou.com/tts/job/" + inferenceJobToken;
        boolean success = false;

        JSONObject jobResponseJson = null;

        while (true) {
            Thread.sleep(1000);
            HttpRequest jobRequest = HttpRequest.newBuilder()
                    .uri(new URI(jobUrl))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> jobResponse = client.send(jobRequest, HttpResponse.BodyHandlers.ofString());
            jobResponseJson = new JSONObject(jobResponse.body());
            String status = jobResponseJson.getJSONObject("state").getString("status");

            System.out.println(status);
            if ("pending".equals(status) || "started".equals(status)) {
                continue;
            } else if ("complete_success".equals(status)) {
                success = true;
                break;
            } else {
                break;
            }
        }

        if (success) {
            System.out.println("Success!");
            return STORAGE_URL + jobResponseJson.getJSONObject("state").getString("maybe_public_bucket_wav_audio_path");
        } else {
            System.out.println("Failed!");
            System.out.println(jobResponseJson.getJSONObject("state"));
            return null;
        }
    }

    private static List<JSONObject> getVoices() throws Exception {
        if (voices == null) {
            voices = getVoiceList(List.of("de"));
        }
        return voices;
    }

    public static String generateAudioRandomVoice(String text) {
        try {
            JSONObject voice = getVoices().get(new Random().nextInt(voices.size()));
            System.out.println("Generating using voice: " + voice.getString("title"));
            String audio = generateAudio(voice.getString("model_token"), text);
            return audio;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
