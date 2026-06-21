package com.tradelog.trade.parser;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TradeInputParserTest {

    private final TradeInputParser parser = new TradeInputParser();

    @Test
    void parsesStoCallOption() {
        var r = parser.parse("STO 5 SPY 480C 12/20 @2.35");
        assertThat(r.valid()).isTrue();
        assertThat(r.action()).isEqualTo("STO");
        assertThat(r.qty()).isEqualTo(5);
        assertThat(r.ticker()).isEqualTo("SPY");
        assertThat(r.instrumentType()).isEqualTo("OPTION");
        assertThat(r.optionType()).isEqualTo("CALL");
        assertThat(r.strike()).isEqualTo(480.0);
        assertThat(r.expiry()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 12, 20));
        assertThat(r.price()).isEqualTo(2.35);
        assertThat(r.cashFlow()).isEqualTo(5 * 2.35 * 100);
        assertThat(r.strategy()).isEqualTo("CC");
    }

    @Test
    void parsesSto_put_taggedCsp() {
        var r = parser.parse("STO 1 NVDA 500P 01/16 @3.00");
        assertThat(r.valid()).isTrue();
        assertThat(r.optionType()).isEqualTo("PUT");
        assertThat(r.strategy()).isEqualTo("CSP");
        assertThat(r.cashFlow()).isEqualTo(300.0);
    }

    @Test
    void parsesExpiryWithFullYear() {
        var r = parser.parse("STO 1 SPY 450P 03/21/2027 @1.50");
        assertThat(r.valid()).isTrue();
        assertThat(r.expiry()).isEqualTo(LocalDate.of(2027, 3, 21));
    }

    @Test
    void parsesBtcOption_negativeCashFlow() {
        var r = parser.parse("BTC 5 SPY 480C 12/20 @1.00");
        assertThat(r.valid()).isTrue();
        assertThat(r.cashFlow()).isEqualTo(-500.0);
        assertThat(r.strategy()).isEqualTo("Close");
    }

    @Test
    void parsesStockBto_negativeCashFlow() {
        var r = parser.parse("BTO 100 NVDA @820.00");
        assertThat(r.valid()).isTrue();
        assertThat(r.instrumentType()).isEqualTo("STOCK");
        assertThat(r.cashFlow()).isEqualTo(-82000.0);
        assertThat(r.strategy()).isEqualTo("Long");
        assertThat(r.optionType()).isNull();
        assertThat(r.expiry()).isNull();
    }

    @Test
    void parsesAsgn_mapsToAssigned() {
        var r = parser.parse("ASGN 100 NVDA @500.00");
        assertThat(r.valid()).isTrue();
        assertThat(r.action()).isEqualTo("ASSIGNED");
        assertThat(r.cashFlow()).isEqualTo(-50000.0);
        assertThat(r.strategy()).isEqualTo("Assignment");
    }

    @Test
    void parsesStockStc_positiveCashFlow() {
        var r = parser.parse("STC 100 NVDA @850.00");
        assertThat(r.valid()).isTrue();
        assertThat(r.cashFlow()).isEqualTo(85000.0);
        assertThat(r.strategy()).isEqualTo("Close");
    }

    @Test
    void invalidFormat_returnsValidFalse() {
        var r = parser.parse("STO SPY 480C @2.35");
        assertThat(r.valid()).isFalse();
        assertThat(r.error()).isNotBlank();
    }

    @Test
    void emptyInput_returnsValidFalse() {
        var r = parser.parse("");
        assertThat(r.valid()).isFalse();
    }

    @Test
    void nullInput_returnsValidFalse() {
        var r = parser.parse(null);
        assertThat(r.valid()).isFalse();
    }
}
