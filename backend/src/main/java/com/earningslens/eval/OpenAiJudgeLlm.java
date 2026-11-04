package com.earningslens.eval;

import com.earningslens.generation.LlmReply;
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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link JudgeLlm} backed by Spring AI's OpenAI chat model, with the judge's own model and output limit.
 *
 * Structured output: the request carries the JSON schema in strict mode, so OpenAI only returns JSON that
 * matches it (Spring AI 2.0.1: ResponseFormat type JSON_SCHEMA; the string is the schema itself). JudgeClient
 * still validates the content, because the schema cannot say "one entry per pair" or "score 1 to 5 with a reason".
 * As in OpenAiChatLlm, the model's default options are copied (mutate) because request options replace them.
 */
@Component
public class OpenAiJudgeLlm implements JudgeLlm {

    private final ChatModel chatModel;
    private final JudgeProperties judge;

    public OpenAiJudgeLlm(ChatModel chatModel, JudgeProperties judge) {
        this.chatModel = chatModel;
        this.judge = judge;
    }

    @Override
    public LlmReply complete(String systemMessage, String userMessage, String jsonSchema) {
        List<Message> messages = List.of(new SystemMessage(systemMessage), new UserMessage(userMessage));
        ChatResponse response = chatModel.call(new Prompt(messages, options(jsonSchema)));
        return toReply(response);
    }

    OpenAiChatOptions options(String jsonSchema) {
        ChatOptions defaults = chatModel.getDefaultOptions();
        OpenAiChatOptions.Builder builder = defaults instanceof OpenAiChatOptions openAi
                ? openAi.mutate()
                : OpenAiChatOptions.builder();
        OpenAiChatModel.ResponseFormat format = OpenAiChatModel.ResponseFormat.builder()
                .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(jsonSchema)
                .strict(true)
                .build();
        return builder
                .model(judge.model())
                .temperature(judge.temperature())
                .maxTokens(judge.maxOutputTokens())
                .responseFormat(format)
                .build();
    }

    private LlmReply toReply(ChatResponse response) {
        Generation result = response == null ? null : response.getResult();
        if (result == null || result.getOutput() == null) {
            throw new IllegalStateException("The judge model returned no result");
        }
        String text = result.getOutput().getText();
        String finishReason = result.getMetadata() == null ? null : result.getMetadata().getFinishReason();
        boolean truncated = "LENGTH".equalsIgnoreCase(finishReason);

        ChatResponseMetadata metadata = response.getMetadata();
        String model = metadata == null || metadata.getModel() == null || metadata.getModel().isBlank()
                ? judge.model()
                : metadata.getModel();
        Usage usage = metadata == null ? null : metadata.getUsage();
        Integer promptTokens = usage == null ? null : usage.getPromptTokens();
        Integer completionTokens = usage == null ? null : usage.getCompletionTokens();

        return new LlmReply(text == null ? "" : text, model, promptTokens, completionTokens, truncated);
    }
}
