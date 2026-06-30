package com.tradelog.price;

import com.tradelog.price.dto.PriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PriceServiceTest {

    private static final String YAHOO_BASE = "https://query1.finance.yahoo.com/v8/finance/chart/";

    private MockRestServiceServer mockServer;
    private PriceService service;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        service = new PriceService(client);
    }

    @Test
    void fetchPrice_returnsPrice_whenYahooRespondsWithValidJson() {
        mockServer.expect(requestTo(YAHOO_BASE + "AAPL"))
            .andRespond(withSuccess("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":185.92}}]}}
                """, MediaType.APPLICATION_JSON));

        PriceResponse result = service.fetchPrice("AAPL");

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.price()).isEqualTo(185.92);
        assertThat(result.source()).isEqualTo("yahoo");
        assertThat(result.fetchedAt()).isNotNull();
        mockServer.verify();
    }

    @Test
    void fetchPrice_returnsUnavailable_whenResultArrayIsEmpty() {
        mockServer.expect(requestTo(YAHOO_BASE + "UNKN"))
            .andRespond(withSuccess("""
                {"chart":{"result":[]}}
                """, MediaType.APPLICATION_JSON));

        PriceResponse result = service.fetchPrice("UNKN");

        assertThat(result.ticker()).isEqualTo("UNKN");
        assertThat(result.price()).isNull();
        assertThat(result.source()).isEqualTo("unavailable");
    }

    @Test
    void fetchPrice_returnsUnavailable_whenResultIsNull() {
        mockServer.expect(requestTo(YAHOO_BASE + "UNKN"))
            .andRespond(withSuccess("""
                {"chart":{"result":null}}
                """, MediaType.APPLICATION_JSON));

        PriceResponse result = service.fetchPrice("UNKN");

        assertThat(result.price()).isNull();
        assertThat(result.source()).isEqualTo("unavailable");
    }

    @Test
    void fetchPrice_returnsUnavailable_whenYahooReturns5xx() {
        mockServer.expect(requestTo(YAHOO_BASE + "FAIL"))
            .andRespond(withServerError());

        PriceResponse result = service.fetchPrice("FAIL");

        assertThat(result.ticker()).isEqualTo("FAIL");
        assertThat(result.price()).isNull();
        assertThat(result.source()).isEqualTo("unavailable");
    }
}
