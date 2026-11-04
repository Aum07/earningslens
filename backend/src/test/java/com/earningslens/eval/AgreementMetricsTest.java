package com.earningslens.eval;

import com.earningslens.eval.AgreementMetrics.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Roadmap E5.2: agreement maths checked against numbers worked out by hand. */
class AgreementMetricsTest {

    @Test
    void cohensKappaMatchesAHandCalculation() {
        // 10 answers: 6 both yes, 2 both no, 1 person-yes/judge-no, 1 person-no/judge-yes.
        // observed = 8/10 = 0.8; P(yes) = 0.7 for both; expected = 0.7*0.7 + 0.3*0.3 = 0.58;
        // kappa = (0.8 - 0.58) / (1 - 0.58) = 0.5238
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            pairs.add(new Pair(true, true, 5, 5));
        }
        pairs.add(new Pair(false, false, 2, 2));
        pairs.add(new Pair(false, false, 2, 2));
        pairs.add(new Pair(true, false, 4, 4));
        pairs.add(new Pair(false, true, 3, 3));

        AgreementMetrics.Faithfulness f = AgreementMetrics.faithfulness(pairs);

        assertThat(f.bothYes()).isEqualTo(6);
        assertThat(f.bothNo()).isEqualTo(2);
        assertThat(f.humanYesJudgeNo()).isEqualTo(1);
        assertThat(f.humanNoJudgeYes()).isEqualTo(1);
        assertThat(f.agreement()).isCloseTo(0.8, within(1e-9));
        assertThat(f.kappa()).isCloseTo(0.5238, within(1e-4));
    }

    @Test
    void perfectAgreementIsKappaOne() {
        List<Pair> pairs = List.of(new Pair(true, true, 5, 5), new Pair(false, false, 1, 1));

        assertThat(AgreementMetrics.faithfulness(pairs).kappa()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void kappaIsUndefinedWhenEveryoneGivesTheSameLabel() {
        // Both raters said "faithful" every time: 100% agreement, but nothing to tell apart from chance.
        List<Pair> pairs = List.of(new Pair(true, true, 5, 5), new Pair(true, true, 4, 4));

        AgreementMetrics.Faithfulness f = AgreementMetrics.faithfulness(pairs);
        assertThat(f.agreement()).isEqualTo(1.0);
        assertThat(f.kappa()).isNull();
    }

    @Test
    void correctnessExactWithinOneBiasAndTable() {
        List<Pair> pairs = List.of(
                new Pair(true, true, 5, 5),    // exact
                new Pair(true, true, 4, 5),    // within one, judge +1
                new Pair(true, true, 2, 5),    // off by 3, judge +3
                new Pair(true, true, 3, 2));   // within one, judge -1

        AgreementMetrics.Correctness c = AgreementMetrics.correctness(pairs);

        assertThat(c.exact()).isEqualTo(0.25);
        assertThat(c.withinOne()).isEqualTo(0.75);
        assertThat(c.meanJudgeMinusHuman()).isEqualTo(0.75);   // (0 + 1 + 3 - 1) / 4
        assertThat(c.matrix().get(1).get(4)).isEqualTo(1);     // person 2, judge 5
        assertThat(c.matrix().get(2).get(1)).isEqualTo(1);     // person 3, judge 2
        assertThat(c.matrix().stream().flatMap(List::stream).mapToInt(Integer::intValue).sum()).isEqualTo(4);
    }

    @Test
    void emptyOrOutOfRangeIsRefused() {
        assertThatThrownBy(() -> AgreementMetrics.summarize(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgreementMetrics.correctness(List.of(new Pair(true, true, 0, 3))))
                .hasMessageContaining("1 to 5");
    }
}
