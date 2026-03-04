package com.example.backend.service.impl;

import com.example.backend.dto.response.CvScreeningResponse;
import com.example.backend.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final ChatModel chatModel;

    @Override
    public CvScreeningResponse screenCv(byte[] fileContent, String fileName) {
        String text = extractTextFromPdf(fileContent);

        String promptString = """
                Analyze the following CV text and extract key information in JSON format:
                - skills: list of technical skills
                - score: overall score from 0 to 100 based on quality
                - summary: 2-3 sentence summary of the candidate
                - recommendation: Hire, Interview, or Reject

                CV Text:
                {text}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptString);
        Prompt prompt = promptTemplate.create(Map.of("text", text));

        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getContent();

        log.info("AI CV Screening Result for {}: {}", fileName, content);

        // In a real app, we would use a Structured Output Parser.
        // For simplicity, we return a mock-mapped response.
        return CvScreeningResponse.builder()
                .skills(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"))
                .score(85)
                .summary("Strong candidate with relevant full-stack experience.")
                .recommendation("Interview")
                .build();
    }

    @Override
    public Map<String, Object> analyzeSentiment(String text) {
        String promptString = "Analyze the sentiment of the following text and return: Label (POSITIVE, NEUTRAL, NEGATIVE) and Score (0.0 to 1.0).\n\nText: "
                + text;
        ChatResponse response = chatModel.call(new Prompt(promptString));
        String result = response.getResult().getOutput().getContent();

        Map<String, Object> sentiment = new HashMap<>();
        sentiment.put("label",
                result.contains("POSITIVE") ? "POSITIVE" : (result.contains("NEGATIVE") ? "NEGATIVE" : "NEUTRAL"));
        sentiment.put("score", 0.85); // Mock score
        return sentiment;
    }

    @Override
    public Double calculateMatchingScore(Long internId, Long mentorId) {
        // Mock matching logic
        return 0.92;
    }

    private String extractTextFromPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            return "";
        }
    }
}
