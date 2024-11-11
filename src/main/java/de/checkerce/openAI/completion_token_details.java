package de.checkerce.openAI;

public class completion_token_details {
    public final int reasoning_tokens;
    public final int accepted_prediction_tokens;
    public final int rejected_prediction_tokens;

    public completion_token_details(int reasoningTokens, int acceptedPredictionTokens, int rejectedPredictionTokens) {
        reasoning_tokens = reasoningTokens;
        accepted_prediction_tokens = acceptedPredictionTokens;
        rejected_prediction_tokens = rejectedPredictionTokens;
    }
}
