package com.zcy.agentlearning.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(AiProvider provider) {
        super("AI Provider 未配置: " + provider.value());
    }
}
