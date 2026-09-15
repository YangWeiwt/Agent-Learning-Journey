package com.zcy.agentlearning.controller;

import com.zcy.agentlearning.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatService chatService = (message, chatId, provider) ->
                provider.value() + ": ChatClient 是 Spring AI 的高层调用门面。";
        mockMvc = MockMvcBuilders.standaloneSetup(new LearningController(chatService)).build();
    }

    @Test
    void healthReturnsApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Agent Learning Journey"));
    }

    @Test
    void lessonsReturnsAllImplementedLessons() throws Exception {
        mockMvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].order").value(0))
                .andExpect(jsonPath("$[0].document").value("00-大模型基础概念.md"))
                .andExpect(jsonPath("$[3].title").value("Spring AI 核心对象"));
    }

    @Test
    void chatDelegatesToApplicationService() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"ChatClient 是什么？","chatId":"lesson-03","provider":"minimax"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("minimax"))
                .andExpect(jsonPath("$.chatId").value("lesson-03"))
                .andExpect(jsonPath("$.content").value("minimax: ChatClient 是 Spring AI 的高层调用门面。"));
    }

    @Test
    void chatDefaultsToDeepseek() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"你好","chatId":"default-provider"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deepseek"))
                .andExpect(jsonPath("$.content").value("deepseek: ChatClient 是 Spring AI 的高层调用门面。"));
    }

    @Test
    void chatRejectsUnknownProvider() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"你好","chatId":"unknown-provider","provider":"other"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsBlankInput() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":" ","chatId":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
