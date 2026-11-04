package com.earningslens.generation;

import com.earningslens.config.LlmProperties;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link ChatLlm} backed by Spring AI's OpenAI chat model.
 *
 * Model, temperature (0) and the output limit (700 tokens) come from earningslens.llm in
 * application.yaml (spec 9.1). In Spring AI 2.0.1, options sent with a request replace the
 * model's defaults completely, so the defaults are copied (mutate) and only these three are changed.
 * That keeps the default request timeout (60 s). Retries are set on the client (spring.ai.openai.max-retries: 0).
 */
@Component
public class OpenAiChatLlm implements ChatLlm {

    private final ChatModel chatModel;
    private final LlmProperties llm;

    public OpenAiChatLlm(ChatModel chatModel, LlmProperties llm) {
        this.chatModel = chatModel;
        this.llm = llm;
    }

    @Override
    public LlmReply complete(String systemMessage, String userMessage) {
        List<Message> messages = List.of(new SystemMessage(systemMessage), new UserMessage(userMessage));
        ChatResponse response = chatModel.call(new Prompt(messages, options()));
        return toReply(response);
    }

    OpenAiChatOptions options() {
        ChatOptions defaults = chatModel.getDefaultOptions();
        OpenAiChatOptions.Builder builder = defaults instanceof OpenAiChatOptions openAi
                ? openAi.mutate()
                : OpenAiChatOptions.builder();
        return builder
                .model(llm.chatModel())
                .temperature(llm.temperature())
                .maxTokens(llm.maxOutputTokens())
                .build();
    }

    private LlmReply toReply(ChatResponse response) {
        Generation result = response == null ? null : response.getResult();
        if (result == null || result.getOutput() == null) {
            throw new IllegalStateException("The chat model returned no result");
        }
        String text = result.getOutput().getText();
        String finishReason = result.getMetadata() == null ? null : result.getMetadata().getFinishReason();
        boolean truncated = "LENGTH".equalsIgnoreCase(finishReason);

        ChatResponseMetadata metadata = response.getMetadata();
        String model = metadata == null || metadata.getModel() == null || metadata.getModel().isBlank()
                ? llm.chatModel()
                : metadata.getModel();
        Usage usage = metadata == null ? null : metadata.getUsage();
        Integer promptTokens = usage == null ? null : usage.getPromptTokens();
        Integer completionTokens = usage == null ? null : usage.getCompletionTokens();

        return new LlmReply(text == null ? "" : text, model, promptTokens, completionTokens, truncated);
    }
}
