package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SupportTicketCreateRequest {
    @NotNull(message = "Category is required")
    private TicketCategory category;

    private TicketPriority priority = TicketPriority.MEDIUM;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 5000, message = "Content must be between 10 and 5000 characters")
    private String content;
}
