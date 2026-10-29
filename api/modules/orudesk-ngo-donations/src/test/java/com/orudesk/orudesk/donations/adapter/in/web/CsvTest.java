package com.orudesk.orudesk.donations.adapter.in.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {

    @Test
    void plainTextStaysAsItIs() {
        assertThat(Csv.field("Meera Iyer")).isEqualTo("Meera Iyer");
    }

    @Test
    void commasQuotesAndLineBreaksAreQuoted() {
        assertThat(Csv.field("Iyer, Meera")).isEqualTo("\"Iyer, Meera\"");
        assertThat(Csv.field("the \"Trust\"")).isEqualTo("\"the \"\"Trust\"\"\"");
    }

    @Test
    void aFormulaIsKeptAsText() {
        assertThat(Csv.field("=HYPERLINK(\"http://evil\")")).isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\"");
        assertThat(Csv.field("+91 98450")).isEqualTo("'+91 98450");
        assertThat(Csv.field("@SUM(A1)")).isEqualTo("'@SUM(A1)");
    }
}
