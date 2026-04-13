package com.github.willrees23.reportx.core.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdsTest {

    @Test
    void shortCaseId_takesFirstEightChars() {
        UUID id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456789");
        assertThat(Ids.shortCaseId(id)).isEqualTo("a1b2c3d4");
    }

    @Test
    void shortCaseId_rejectsNull() {
        assertThatThrownBy(() -> Ids.shortCaseId(null))
                .isInstanceOf(NullPointerException.class);
    }
}
