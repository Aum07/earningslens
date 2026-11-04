package com.earningslens.eval;

import com.earningslens.generation.LlmReply;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The Spring AI ChatModel is a Mockito mock: no network, no cost. */
class OpenAiJudgeLlmTest {

    private static final JudgeProperties JUDGE =
            new JudgeProperties("gpt-4.1", 0.0, 2000, 2, "../eval/judge-rubric.md", 2.0, 8.0, "q001");

    private final ChatModel chatModel = mock(ChatModel.class);
    private final OpenAiJudgeLlm llm = new OpenAiJudgeLlm(chatModel, JUDGE);

    @Test
    void sendsJudgeModelLimitsAndStrictJsonSchema() {
        when(chatModel.getDefaultOptions()).thenReturn(
                OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.7).timeout(Duration.ofSeconds(30)).build());
        when(chatModel.call(any(Prompt.class))).thenReturn(response("{}", "STOP"));

        LlmReply reply = llm.complete("system", "user", JudgeResponseParser.SCHEMA);

        ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(sent.capture());
        assertThat(sent.getValue().getInstructions()).extracting(m -> m.getText()).containsExactly("system", "user");

        OpenAiChatOptions options = (OpenAiChatOptions) sent.getValue().getOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4.1");           // not the answer model
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getMaxTokens()).isEqualTo(2000);
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(30));   // kept from the defaults
        OpenAiChatModel.ResponseFormat format = options.getResponseFormat();
        assertThat(format.getType()).isEqualTo(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA);
        assertThat(format.getJsonSchema()).isEqualTo(JudgeResponseParser.SCHEMA);
        assertThat(format.getStrict()).isTrue();

        assertThat(reply).isEqualTo(new LlmReply("{}", "gpt-4.1-2025-04-14", 3000, 400, false));
    }

    @Test
    void lengthFinishReasonMeansTruncated() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("{\"claims\": [", "LENGTH"));

        assertThat(llm.complete("s", "u", "{}").truncated()).isTrue();
    }

    private static ChatResponse response(String text, String finishReason) {
        Generation generation = new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("gpt-4.1-2025-04-14")
                .usage(new DefaultUsage(3000, 400))
                .build();
        return new ChatResponse(List.of(generation), metadata);
    }
}
