package com.earningslens.ingest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakerLabelTest {

    @Test
    void alphabetStyleNameRoleAndSpeechInOneParagraph() {
        assertThat(SpeakerLabel.parse("Sundar Pichai, CEO, Alphabet and Google: Thanks, Jim.", new HashMap<>()))
                .contains(new SpeakerLabel("Sundar Pichai, CEO, Alphabet and Google", "Thanks, Jim."));
    }

    @Test
    void microsoftStyleCapitalNames() {
        assertThat(SpeakerLabel.parse("AMY HOOD:", new HashMap<>())).contains(new SpeakerLabel("AMY HOOD", ""));
        assertThat(SpeakerLabel.parse("ADAM WOOD, Morgan Stanley:", new HashMap<>()))
                .contains(new SpeakerLabel("ADAM WOOD, Morgan Stanley", ""));
    }

    @Test
    void metaStyleLabelLineThenShortNameGetsTheFullRole() {
        Map<String, String> known = new HashMap<>();

        assertThat(SpeakerLabel.parse("Susan Li, CFO", known)).contains(new SpeakerLabel("Susan Li, CFO", ""));
        assertThat(SpeakerLabel.parse("Susan Li: I'm happy to answer that.", known))
                .contains(new SpeakerLabel("Susan Li, CFO", "I'm happy to answer that."));
        assertThat(SpeakerLabel.parse("Ross Sandler: Thanks for taking the question.", known))
                .contains(new SpeakerLabel("Ross Sandler", "Thanks for taking the question."));
    }

    @Test
    void operatorInAnyCase() {
        assertThat(SpeakerLabel.parse("Operator: Thank you.", new HashMap<>()))
                .contains(new SpeakerLabel("Operator", "Thank you."));
        assertThat(SpeakerLabel.parse("OPERATOR:", new HashMap<>())).contains(new SpeakerLabel("Operator", ""));
    }

    @Test
    void ordinaryTextIsNotASpeaker() {
        String[] notSpeakers = {
                "Meta Platforms, Inc. (META)",
                "Third Quarter 2026 Results Conference Call",
                "October 21st, 2026",
                "Our business model will have a few components:",
                "Second: we expanded capacity.",
                "Today, I will focus my remarks across the three layers of our stack: infra and apps.",
                "Amy, two related questions: one on capex.",
                "Great, thanks.",
                "Partnership Ads, which allow advertisers to run ads with creators, grew quickly."
        };
        for (String text : notSpeakers) {
            assertThat(SpeakerLabel.parse(text, new HashMap<>())).as(text).isEmpty();
        }
    }
}
