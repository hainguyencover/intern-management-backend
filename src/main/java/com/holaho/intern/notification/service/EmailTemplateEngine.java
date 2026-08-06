package com.holaho.intern.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateEngine {

    private final ResourceLoader resourceLoader;

    public String render(String templateName, Map<String, String> variables) {
        String templatePath = "classpath:templates/email/" + templateName + ".html";
        try {
            Resource resource = resourceLoader.getResource(templatePath);
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                String val = entry.getValue() != null ? entry.getValue() : "";
                html = html.replace(key, val);
            }
            return html;
        } catch (IOException e) {
            log.error("Failed to render email template: {}", templateName, e);
            throw new RuntimeException("Could not read email template " + templateName, e);
        }
    }
}
