package com.holaho.intern.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class NotificationPreferenceDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String eventType;
        private boolean emailEnabled = true;
        private boolean websocketEnabled = true;
        private boolean inAppEnabled = true;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String eventType;
        private boolean emailEnabled;
        private boolean websocketEnabled;
        private boolean inAppEnabled;
    }
}
