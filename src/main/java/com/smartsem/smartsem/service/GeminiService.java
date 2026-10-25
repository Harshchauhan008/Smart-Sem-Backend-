
package com.smartsem.smartsem.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GeminiService {

    private final Client client;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    
public String askGemini(String question) {

    if (question == null || question.isBlank()) {
        throw new IllegalArgumentException(
                "Question cannot be empty");
    }

    try {
        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        question,
                        null
                );

        String answer = response.text();

        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned an empty response");
        }

        return answer;

    } catch (Exception e) {
        System.err.println("=== GEMINI API FAILURE ===");
        System.err.println("Exception: "
                + e.getClass().getName());
        System.err.println("Message: "
                + e.getMessage());
        e.printStackTrace();
        throw e;
    }
}


    public String askGeminiWithImage(
            String prompt,
            String imagePath) throws IOException {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt cannot be empty");
        }

        if (imagePath == null || imagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Image path cannot be empty");
        }

        Path path = Path.of(imagePath);

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Image file does not exist: " + imagePath);
        }

        String mimeType = Files.probeContentType(path);

        if (mimeType == null ||
                !(mimeType.equals("image/jpeg")
                || mimeType.equals("image/png")
                || mimeType.equals("image/webp"))) {
            throw new IllegalArgumentException(
                    "Unsupported image format: " + mimeType);
        }

        byte[] imageBytes = Files.readAllBytes(path);

        Content content = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imageBytes, mimeType)
        );

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        content,
                        null
                );

        String answer = response.text();

        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned an empty image analysis response");
        }

        return answer;
    }
}
