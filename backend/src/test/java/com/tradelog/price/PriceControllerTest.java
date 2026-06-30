package com.tradelog.price;

import com.tradelog.price.dto.PriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:${java.io.tmpdir}/tradelog-test.db"
})
class PriceControllerTest {

    @Autowired
    WebApplicationContext ctx;

    @MockitoBean
    PriceService priceService;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void batch_returns200WithPriceMap_forValidTickers() throws Exception {
        when(priceService.fetchPrice("AAPL"))
            .thenReturn(new PriceResponse("AAPL", 185.92, "yahoo", Instant.now()));
        when(priceService.fetchPrice("NVDA"))
            .thenReturn(new PriceResponse("NVDA", 875.40, "yahoo", Instant.now()));

        mvc.perform(post("/api/v1/prices/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tickers":["AAPL","NVDA"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.AAPL.price").value(185.92))
            .andExpect(jsonPath("$.NVDA.price").value(875.40))
            .andExpect(jsonPath("$.AAPL.source").value("yahoo"));
    }

    @Test
    void batch_returnsNullPrice_forUnavailableTicker() throws Exception {
        when(priceService.fetchPrice("UNKN"))
            .thenReturn(new PriceResponse("UNKN", null, "unavailable", Instant.now()));

        mvc.perform(post("/api/v1/prices/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tickers":["UNKN"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.UNKN.price", nullValue()))
            .andExpect(jsonPath("$.UNKN.source").value("unavailable"));
    }

    @Test
    void batch_returns400_forEmptyTickersList() throws Exception {
        mvc.perform(post("/api/v1/prices/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tickers":[]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void batch_returns400_forTickersListOver50() throws Exception {
        String tickers = "[" + "\"AAPL\",".repeat(51).replaceAll(",$", "") + "]";
        mvc.perform(post("/api/v1/prices/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tickers\":" + tickers + "}"))
            .andExpect(status().isBadRequest());
    }
}
