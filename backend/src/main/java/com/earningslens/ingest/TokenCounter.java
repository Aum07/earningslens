package com.earningslens.ingest;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Component;

/**
 * Counts tokens the same way OpenAI does for text-embedding-3-small (cl100k_base encoding),
 * so chunk sizes match what the embedding API actually sees.
 */
@Component
public class TokenCounter {

    private final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public int count(String text) {
        return encoding.countTokens(text);
    }
}
