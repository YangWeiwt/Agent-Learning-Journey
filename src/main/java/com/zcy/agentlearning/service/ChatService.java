package com.zcy.agentlearning.service;

public interface ChatService {

    String chat(String message, String chatId, AiProvider provider);
}
