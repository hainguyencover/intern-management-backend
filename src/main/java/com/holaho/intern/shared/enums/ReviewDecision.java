package com.holaho.intern.shared.enums;

import lombok.Getter;

@Getter
public enum ReviewDecision {
    APPROVE("DuyÃ¡Â»â€¡t"),
    REJECT("TÃ¡Â»Â« chÃ¡Â»â€˜i");

    private final String displayName;

    ReviewDecision(String displayName) {
        this.displayName = displayName;
    }

}

