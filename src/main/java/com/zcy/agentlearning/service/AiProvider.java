package com.zcy.agentlearning.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum AiProvider {
    DEEPSEEK,
    MINIMAX;

    @JsonCreator
    public static AiProvider fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DEEPSEEK;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
