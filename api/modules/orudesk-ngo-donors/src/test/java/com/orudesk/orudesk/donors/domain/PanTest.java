package com.orudesk.orudesk.donors.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PanTest {

    @Test
    void isStoredInUpperCase() {
        assertThat(new Pan(" abcde1234f ").value()).isEqualTo("ABCDE1234F");
    }

    @Test
    void showsOnlyTheLastFourCharacters() {
        assertThat(new Pan("ABCDE1234F").masked()).isEqualTo("XXXXXX234F");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ABCDE1234", "ABCD12345F", "ABCDE12345", "ABCDE1234FF"})
    void refusesAnythingElse(String value) {
        assertThatThrownBy(() -> new Pan(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
