package com.earningslens.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logs a short summary of the active configuration when the app starts.
 * It only reports WHETHER the OpenAI key is set, never the key itself.
 *
 * SPRING CONCEPT, @Component: tells Spring to create one instance of this class
 * (a "bean") and manage it. Because it implements ApplicationRunner, Spring calls
 * run() once, right after the app has started.
 *
 * SPRING CONCEPT, dependency injection: we never write "new LlmProperties(...)".
 * Spring sees the constructor parameters below, finds the matching beans it has
 * already created, and passes them in. This is called constructor injection.
 */
@Component
public class StartupConfigLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigLogger.class);
    private static final String PLACEHOLDER_KEY = "not-set";

    private final Environment environment;
    private final LlmProperties llm;
    private final RetrievalProperties retrieval;
    private final String openAiApiKey;

    public StartupConfigLogger(Environment environment,
                               LlmProperties llm,
                               RetrievalProperties retrieval,
                               // @Value reads a single property; here the key Spring AI uses.
                               @Value("${spring.ai.openai.api-key:" + PLACEHOLDER_KEY + "}") String openAiApiKey) {
        this.environment = environment;
        this.llm = llm;
        this.retrieval = retrieval;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean keySet = openAiApiKey != null && !openAiApiKey.isBlank() && !PLACEHOLDER_KEY.equals(openAiApiKey);
        log.info("Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("OpenAI key configured: {}", keySet ? "yes" : "NO (set OPENAI_API_KEY in .env)");
        log.info("Models: chat={}, embedding={} ({} dims)", llm.chatModel(), llm.embeddingModel(), llm.embeddingDimensions());
        log.info("Retrieval: k={}, kPerPeriod={}, rrfK={}", retrieval.k(), retrieval.kPerPeriod(), retrieval.rrfK());
    }
}
