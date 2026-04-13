package com.github.willrees23.reportx.paper.reputation;

import com.github.willrees23.reportx.core.config.ReputationYaml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TierResolverTest {

    private static final List<ReputationYaml.Tier> DEFAULT_TIERS = List.of(
            new ReputationYaml.Tier("outstanding", 0, 0, "Outstanding", "0"),
            new ReputationYaml.Tier("good", 1, 2, "Good", "1-2"),
            new ReputationYaml.Tier("neutral", 3, 5, "Neutral", "3-5"),
            new ReputationYaml.Tier("questionable", 6, 10, "Questionable", "6-10"),
            new ReputationYaml.Tier("bad", 11, -1, "Bad", "11+"));

    @Test
    void zeroReportsResolvesToOutstanding() {
        assertThat(TierResolver.resolve(0, DEFAULT_TIERS).id()).isEqualTo("outstanding");
    }

    @Test
    void midRangeResolvesToCorrectTier() {
        assertThat(TierResolver.resolve(2, DEFAULT_TIERS).id()).isEqualTo("good");
        assertThat(TierResolver.resolve(4, DEFAULT_TIERS).id()).isEqualTo("neutral");
        assertThat(TierResolver.resolve(7, DEFAULT_TIERS).id()).isEqualTo("questionable");
    }

    @Test
    void countAboveAllBoundsResolvesToOpenEndedTier() {
        assertThat(TierResolver.resolve(11, DEFAULT_TIERS).id()).isEqualTo("bad");
        assertThat(TierResolver.resolve(9999, DEFAULT_TIERS).id()).isEqualTo("bad");
    }

    @Test
    void negativeCountTreatedAsZero() {
        assertThat(TierResolver.resolve(-3, DEFAULT_TIERS).id()).isEqualTo("outstanding");
    }

    @Test
    void emptyTiersListIsRejected() {
        assertThatThrownBy(() -> TierResolver.resolve(1, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
