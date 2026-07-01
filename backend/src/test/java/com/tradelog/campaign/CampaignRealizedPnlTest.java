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
class CampaignRealizedPnlTest {

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
    void newCampaign_hasZeroRealizedPnl() throws Exception {
        int id = createCampaign("SPY");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.realizedPnl").value(0.0));
    }

    @Test
    void openShortOptionOnly_realizedPnlIsZero() throws Exception {
        int id = createCampaign("SPY");
        saveTrade(id, "STO 1 SPY 480P 12/20 @2.35");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.realizedPnl").value(0.0));
    }

    @Test
    void shortOptionFullyClosed_realizedPnlIsPremiumMinusCostToClose() throws Exception {
        int id = createCampaign("SPY");
        saveTrade(id, "STO 1 SPY 480P 12/20 @2.35");   // +235
        saveTrade(id, "BTC 1 SPY 480P 12/20 @1.00");   // -100

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.realizedPnl").value(135.0));
    }

    @Test
    void shortOptionPartiallyClosed_realizedPnlReflectsOnlyClosedPortion() throws Exception {
        int id = createCampaign("SPY");
        saveTrade(id, "STO 2 SPY 480P 12/20 @2.35");   // +470
        saveTrade(id, "BTC 1 SPY 480P 12/20 @1.00");   // -100, 1 contract remains open @2.35

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.realizedPnl").value(135.0));
    }

    @Test
    void stockBoughtThenPartiallySold_realizedPnlReflectsOnlySoldShares() throws Exception {
        int id = createCampaign("NVDA");
        saveTrade(id, "BTO 100 NVDA @50.00");   // -5000
        saveTrade(id, "STC 50 NVDA @60.00");    // +3000, 50 shares remain open @ avg 50

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.realizedPnl").value(500.0));
    }
}
