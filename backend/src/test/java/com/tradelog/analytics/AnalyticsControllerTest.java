package com.tradelog.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:${java.io.tmpdir}/tradelog-test.db"
})
class AnalyticsControllerTest {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void summary_returnsZerosWithNoData() throws Exception {
        mvc.perform(get("/api/v1/analytics/summary"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalPremium").value(0.0))
           .andExpect(jsonPath("$.netOptionsPnl").value(0.0))
           .andExpect(jsonPath("$.campaignWinRate").value(0.0))
           .andExpect(jsonPath("$.tradeWinRate").value(0.0))
           .andExpect(jsonPath("$.totalTrades").value(0));
    }

    @Test
    void summary_withAccountIdParam_returnsZerosWithNoData() throws Exception {
        mvc.perform(get("/api/v1/analytics/summary?accountId=1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalPremium").value(0.0))
           .andExpect(jsonPath("$.netOptionsPnl").value(0.0));
    }

    @Test
    void summary_withUnassignedParam_returnsZerosWithNoData() throws Exception {
        mvc.perform(get("/api/v1/analytics/summary?unassigned=true"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalPremium").value(0.0))
           .andExpect(jsonPath("$.netOptionsPnl").value(0.0));
    }

    @Test
    void pnlByStrategy_returnsEmptyArrayWithNoTrades() throws Exception {
        mvc.perform(get("/api/v1/analytics/pnl-by-strategy"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void pnlByStrategy_withAccountIdParam_returnsEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/analytics/pnl-by-strategy?accountId=1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cumulative_returnsEmptySeriesWithNoTrades() throws Exception {
        mvc.perform(get("/api/v1/analytics/cumulative"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.premium").isArray())
           .andExpect(jsonPath("$.premium").isEmpty())
           .andExpect(jsonPath("$.optionsPnl").isArray())
           .andExpect(jsonPath("$.optionsPnl").isEmpty());
    }

    @Test
    void cumulative_withUnassignedParam_returnsEmptySeries() throws Exception {
        mvc.perform(get("/api/v1/analytics/cumulative?unassigned=true"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.premium").isArray())
           .andExpect(jsonPath("$.premium").isEmpty());
    }
}
