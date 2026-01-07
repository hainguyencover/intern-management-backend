package com.example.backend.enums;

import lombok.Getter;

@Getter
public enum ReviewDecision {
    APPROVE("Duyệt"),
    REJECT("Từ chối");

    private final String displayName;

    ReviewDecision(String displayName) {
        this.displayName = displayName;
    }

}
