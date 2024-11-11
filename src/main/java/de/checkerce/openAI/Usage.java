package de.checkerce.openAI;

public class Usage {
    public final int prompt_tokens;
    public final int completion_tokens;
    public final int total_tokens;
    public final completion_token_details completion_token_details;

    public Usage(int promptTokens, int completionTokens, int totalTokens, completion_token_details completionTokenDetails) {
        prompt_tokens = promptTokens;
        completion_tokens = completionTokens;
        total_tokens = totalTokens;
        completion_token_details = completionTokenDetails;
    }
}
