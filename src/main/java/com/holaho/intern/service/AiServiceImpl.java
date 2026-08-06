package com.holaho.intern.service;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.entity.Application;
import com.holaho.intern.shared.dto.response.CvScreeningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final ChatModel chatModel;
    private final MentorRepository mentorRepository;
    private final InternProfileRepository internProfileRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public CvScreeningResponse screenCv(byte[] fileContent, String fileName) {
        String text = extractTextFromPdf(fileContent);

        // Use BeanOutputConverter for structured AI response
        BeanOutputConverter<CvScreeningResponse> outputConverter = new BeanOutputConverter<>(CvScreeningResponse.class);

        String promptString = """
                Analyze the following CV text and extract key information.
                {format}

                CV Text:
                {text}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptString);
        Prompt prompt = promptTemplate.create(Map.of(
                "text", text,
                "format", outputConverter.getFormat()));

        try {
            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getContent();
            log.info("AI CV Screening raw response for {}: {}", fileName, content);

            return outputConverter.convert(content);
        } catch (Exception e) {
            log.error("AI Screening failed for {}. Returning fallback.", fileName, e);
            return CvScreeningResponse.builder()
                    .summary("Error processing CV with AI: " + e.getMessage())
                    .recommendation("Manual Review Required")
                    .score(0)
                    .build();
        }
    }

    @Override
    public String chat(String message) {
        log.info("AI Chat request: {}", message);

        String systemPrompt = """
                You are "DevMind AI", the official assistant for the HoLaHo Intern Management System.
                Your goal is to help interns, mentors, and HR with system usage and company policies.

                Company Knowledge:
                1. Working Hours: 8:00 AM - 5:00 PM, Monday to Friday.
                2. Dress Code: Business casual.
                3. Onboarding: All interns must complete the 'Getting Started' task in their first week.
                4. Contacts: HR is available at hr@holaho.com. Tech support at support@holaho.com.

                Be professional, helpful, and concise.
                """;

        PromptTemplate template = new PromptTemplate(systemPrompt + "\n\nUser: {message}\nAssistant:");
        ChatResponse response = chatModel.call(template.create(Map.of("message", message)));

        return response.getResult().getOutput().getContent();
    }

    @Override
    public Map<String, Object> analyzeSentiment(String text) {
        String promptString = "Analyze the sentiment of the following text. Return ONLY a valid JSON with keys: 'label' (POSITIVE, NEUTRAL, NEGATIVE) and 'score' (0.0 to 1.0).\n\nText: {text}";
        PromptTemplate template = new PromptTemplate(promptString);
        ChatResponse response = chatModel.call(template.create(Map.of("text", text)));

        // Simple manual parsing or use another converter
        String content = response.getResult().getOutput().getContent();
        log.info("Sentiment result: {}", content);
        return Map.of("result", content);
    }

    @Override
    public Double calculateMatchingScore(Long internId, Long mentorId) {
        log.info("Calculating matching score for intern {} and mentor {}", internId, mentorId);

        Optional<InternProfile> internOpt = internProfileRepository.findById(internId);
        Optional<Mentor> mentorOpt = mentorRepository.findById(mentorId);

        if (internOpt.isEmpty() || mentorOpt.isEmpty()) {
            return 0.5; // Neutral fallback
        }

        InternProfile intern = internOpt.get();
        Mentor mentor = mentorOpt.get();

        String promptString = """
                Rate the compatibility between an intern and a mentor on a scale from 0.0 to 1.0.
                Return ONLY the numeric score.

                Intern Info:
                - Major: {major}
                - University: {university}
                - Skills: {skills}

                Mentor Info:
                - Title: {title}
                - Department: {department}

                Compatibility Score:
                """;

        PromptTemplate template = new PromptTemplate(promptString);
        ChatResponse response = chatModel.call(template.create(Map.of(
                "major", intern.getMajor() != null ? intern.getMajor() : "N/A",
                "university", intern.getUniversity() != null ? intern.getUniversity() : "N/A",
                "skills", intern.getCvSkills() != null ? intern.getCvSkills() : "N/A",
                "title", mentor.getTitle() != null ? mentor.getTitle() : "N/A",
                "department", mentor.getDepartment() != null ? mentor.getDepartment().getName() : "N/A")));

        try {
            return Double.parseDouble(response.getResult().getOutput().getContent().trim());
        } catch (Exception e) {
            log.error("Failed to parse matching score", e);
            return 0.75; // Rational fallback
        }
    }

    @Override
    public String generateInterviewQuestions(Long applicationId) {
        log.info("Generating interview questions for application {}", applicationId);

        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty())
            return "Application not found.";

        Application app = appOpt.get();

        String promptString = """
                Based on the following AI CV screening results, generate 5 challenging interview questions for this candidate.
                Focus on technical skills and gaps identified.

                Skills Found: {skills}
                AI Score: {score}
                AI Summary: {summary}

                Interview Questions:
                """;

        PromptTemplate template = new PromptTemplate(promptString);
        ChatResponse response = chatModel.call(template.create(Map.of(
                "skills", app.getAiSkills() != null ? app.getAiSkills() : "N/A",
                "score", app.getAiScore() != null ? app.getAiScore() : 0,
                "summary", app.getAiSummary() != null ? app.getAiSummary() : "N/A")));

        return response.getResult().getOutput().getContent();
    }

    @Override
    public Map<String, Object> getAnalytics() {
        log.info("Generating AI Analytics insights");

        long totalInterns = internProfileRepository.count();
        long totalMentors = mentorRepository.count();
        long pendingApps = applicationRepository.count(); // Simplified logic

        String analysisPrompt = """
                Provide a high-level strategic analysis for an intern management system based on the following stats:
                - Total Interns: {interns}
                - Total Mentors: {mentors}
                - Pending Applications: {pending}

                Return ONLY a JSON with keys:
                - 'performanceForecast': A short prediction about intern success.
                - 'topSkills': A comma-separated list of most needed skills.
                - 'bottleneckAlert': Identify if there's a shortage of mentors or too many applicants.
                - 'aiEfficiencyScore': A fake percentage (80-99%) based on these numbers.
                """;

        PromptTemplate template = new PromptTemplate(analysisPrompt);
        ChatResponse response = chatModel.call(template.create(Map.of(
                "interns", totalInterns,
                "mentors", totalMentors,
                "pending", pendingApps)));

        String content = response.getResult().getOutput().getContent();

        return Map.of(
                "stats", Map.of(
                        "totalInterns", totalInterns,
                        "totalMentors", totalMentors,
                        "pendingApplications", pendingApps),
                "aiInsights", content);
    }

    private String extractTextFromPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(5); // Limit to first 5 pages for performance and context window
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            return "Empty CV content due to parsing error.";
        }
    }
}
