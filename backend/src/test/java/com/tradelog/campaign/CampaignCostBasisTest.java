package com.tradelog.campaign;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:${java.io.tmpdir}/tradelog-test.db"
})
class CampaignCostBasisTest {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    private int createCampaign(String ticker) throws Exception {
        String body = mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"%s","openedAt":"2026-01-01"}
                        """.formatted(ticker)))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void saveTrade(int campaignId, String rawInput) throws Exception {
        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"%s","tradedAt":"2026-01-02"}
                        """.formatted(campaignId, rawInput)))
           .andExpect(status().isOk());
    }

    @Test
    void stockOnly_costBasisIsAvgSharePrice() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(50.00));
    }

    @Test
    void stockPlusOpenCoveredCall_costBasisReducedByPremium() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");
        saveTrade(id, "STO 1 NVDA 60C 12/26 @2.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(48.00));
    }

    @Test
    void stockPlusOpenProtectivePut_costBasisIncreasedByDebit() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");
        saveTrade(id, "BTO 1 NVDA 40P 12/26 @1.50");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(51.50));
    }

    @Test
    void longCallOnly_noStock_costBasisPerShareEquivalent() throws Exception {
        int id = createCampaign("SPY");
        saveTrade(id, "BTO 1 SPY 400C 06/27 @20.00");
        saveTrade(id, "STO 1 SPY 480C 12/26 @3.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(17.00));
    }

    @Test
    void stockPlusOpenLongCall_unitsAndCashFlowCombine() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");
        saveTrade(id, "BTO 1 NVDA 55C 12/26 @4.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(27.00));
    }

    @Test
    void noStockNoOpenLongCall_costBasisIsNull() throws Exception {
        int id = createCampaign("SPY");
        saveTrade(id, "STO 1 SPY 480C 12/26 @2.00");
        saveTrade(id, "STO 1 SPY 400P 12/26 @2.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").doesNotExist());
    }

    @Test
    void closedLongCall_affectsCashFlowButNotUnits() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");
        saveTrade(id, "BTO 1 NVDA 55C 12/26 @4.00");
        saveTrade(id, "STC 1 NVDA 55C 12/26 @6.00");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.costBasis").value(48.00));
    }
}
