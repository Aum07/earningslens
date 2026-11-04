package com.earningslens.generation;

import com.earningslens.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The Spring AI ChatModel is a Mockito mock: no network, no cost. */
class OpenAiChatLlmTest {

    private static final LlmProperties LLM =
            new LlmProperties("gpt-4o-mini", "text-embedding-3-small", 1536, 0.0, 700);

    private final ChatModel chatModel = mock(ChatModel.class);
    private final OpenAiChatLlm llm = new OpenAiChatLlm(chatModel, LLM);

    @Test
    void sendsSystemAndUserMessagesWithConfiguredOptions() {
        when(chatModel.getDefaultOptions()).thenReturn(
                OpenAiChatOptions.builder().model("some-default").temperature(0.7).timeout(Duration.ofSeconds(30)).build());
        when(chatModel.call(any(Prompt.class))).thenReturn(response("Answer [1].", "STOP", "gpt-4o-mini-2024-07-18", 1200, 35));

        LlmReply reply = llm.complete("system text", "user text");

        ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(sent.capture());
        Prompt prompt = sent.getValue();
        assertThat(prompt.getInstructions()).extracting(m -> m.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(prompt.getInstructions()).extracting(m -> m.getText()).containsExactly("system text", "user text");

        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getMaxTokens()).isEqualTo(700);
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(30));   // kept from the defaults

        assertThat(reply).isEqualTo(new LlmReply("Answer [1].", "gpt-4o-mini-2024-07-18", 1200, 35, false));
    }

    @Test
    void lengthFinishReasonMeansTruncated() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("cut off", "LENGTH", "gpt-4o-mini", 5000, 700));

        assertThat(llm.complete("s", "u").truncated()).isTrue();
    }

    @Test
    void missingModelNameAndUsageFallBackSafely() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("hi")))));

        LlmReply reply = llm.complete("s", "u");

        assertThat(reply.text()).isEqualTo("hi");
        assertThat(reply.model()).isEqualTo("gpt-4o-mini");   // configured name when the API sends none
        assertThat(reply.truncated()).isFalse();
    }

    @Test
    void noResultIsAnError() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of()));

        assertThatThrownBy(() -> llm.complete("s", "u")).isInstanceOf(IllegalStateException.class);
    }

    private static ChatResponse response(String text, String finishReason, String model, int promptTokens,
                                         int completionTokens) {
        Generation generation = new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model(model)
                .usage(new DefaultUsage(promptTokens, completionTokens))
                .build();
        return new ChatResponse(List.of(generation), metadata);
    }
}
