package com.holaho.intern.shared.dto.request;

import com.holaho.intern.user.entity.User;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMentorRequest {

    @NotNull(message = "User ID khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    private Long userId;

    private Long departmentId;

    @Size(max = 255, message = "Chá»©c danh khÃ´ng quÃ¡ 255 kÃ½ tá»±")
    private String title;
}








