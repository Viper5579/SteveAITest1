package com.steve.ai.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.steve.ai.SteveMod;
import com.steve.ai.config.SteveConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AnthropicClient {
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;

    private final HttpClient client;
    private final String apiKey;

    public AnthropicClient() {
        this.apiKey = SteveConfig.ANTHROPIC_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            SteveMod.LOGGER.error("Anthropic API key not configured!");
            return null;
        }

        JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ANTHROPIC_API_URL))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    if (responseBody == null || responseBody.isEmpty()) {
                        SteveMod.LOGGER.error("Anthropic API returned empty response");
                        return null;
                    }
                    return parseResponse(responseBody);
                }

                if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    if (attempt < MAX_RETRIES - 1) {
                        int delayMs = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
                        SteveMod.LOGGER.warn("Anthropic API request failed with status {}, retrying in {}ms (attempt {}/{})",
                            response.statusCode(), delayMs, attempt + 1, MAX_RETRIES);
                        Thread.sleep(delayMs);
                        continue;
                    }
                }

                SteveMod.LOGGER.error("Anthropic API request failed: {}", response.statusCode());
                SteveMod.LOGGER.error("Response body: {}", response.body());
                return null;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SteveMod.LOGGER.error("Request interrupted", e);
                return null;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES - 1) {
                    int delayMs = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
                    SteveMod.LOGGER.warn("Error communicating with Anthropic API, retrying in {}ms (attempt {}/{})",
                        delayMs, attempt + 1, MAX_RETRIES, e);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    SteveMod.LOGGER.error("Error communicating with Anthropic API after {} attempts", MAX_RETRIES, e);
                    return null;
                }
            }
        }

        return null;
    }

    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", SteveConfig.ANTHROPIC_MODEL.get());
        body.addProperty("max_tokens", SteveConfig.ANTHROPIC_MAX_TOKENS.get());
        body.addProperty("temperature", SteveConfig.TEMPERATURE.get());
        body.addProperty("system", systemPrompt);

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);
        body.add("messages", messages);

        return body;
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("content") && json.get("content").isJsonArray()) {
                JsonArray content = json.getAsJsonArray("content");
                if (!content.isEmpty()) {
                    JsonObject first = content.get(0).getAsJsonObject();
                    if (first.has("text")) {
                        return first.get("text").getAsString();
                    }
                }
            }
            SteveMod.LOGGER.error("Unexpected Anthropic response format: {}", responseBody);
            return null;
        } catch (Exception e) {
            SteveMod.LOGGER.error("Error parsing Anthropic response", e);
            return null;
        }
    }
}
