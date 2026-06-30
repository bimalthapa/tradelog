package com.tradelog.trade;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
class TradeControllerTest {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    private int createCampaign() throws Exception {
        String body = mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"SPY","openedAt":"2026-01-01"}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void parseTrade_validOptions_returnsFields() throws Exception {
        mvc.perform(post("/api/v1/trades/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rawInput":"STO 5 SPY 480C 12/20 @2.35","campaignId":1}
                        """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.valid").value(true))
           .andExpect(jsonPath("$.action").value("STO"))
           .andExpect(jsonPath("$.qty").value(5))
           .andExpect(jsonPath("$.ticker").value("SPY"))
           .andExpect(jsonPath("$.instrumentType").value("OPTION"))
           .andExpect(jsonPath("$.optionType").value("CALL"))
           .andExpect(jsonPath("$.strike").value(480.0))
           .andExpect(jsonPath("$.price").value(2.35))
           .andExpect(jsonPath("$.cashFlow").value(1175.0))
           .andExpect(jsonPath("$.strategy").value("CC"));
    }

    @Test
    void parseTrade_invalidInput_returnsValidFalse() throws Exception {
        mvc.perform(post("/api/v1/trades/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rawInput":"garbage input","campaignId":1}
                        """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.valid").value(false))
           .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void saveTrade_validOption_createsLegAndPosition() throws Exception {
        int campaignId = createCampaign();

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """.formatted(campaignId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").isNumber())
           .andExpect(jsonPath("$.action").value("STO"))
           .andExpect(jsonPath("$.ticker").value("SPY"))
           .andExpect(jsonPath("$.netCashFlow").value(1175.0))
           .andExpect(jsonPath("$.strategyTag").value("CC"));

        mvc.perform(get("/api/v1/positions").param("campaignId", String.valueOf(campaignId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].status").value("OPEN"))
           .andExpect(jsonPath("$[0].openQuantity").value(5))
           .andExpect(jsonPath("$[0].avgPrice").value(2.35));
    }

    @Test
    void saveTrade_invalidRawInput_returns400() throws Exception {
        int campaignId = createCampaign();

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"garbage"}
                        """.formatted(campaignId)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void saveTrade_unknownCampaign_returns404() throws Exception {
        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":99999,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """))
           .andExpect(status().isNotFound());
    }

    @Test
    void listTrades_returnsSavedLegsInOrder() throws Exception {
        int campaignId = createCampaign();

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """.formatted(campaignId)))
           .andExpect(status().isOk());

        mvc.perform(get("/api/v1/trades").param("campaignId", String.valueOf(campaignId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$[0].action").value("STO"))
           .andExpect(jsonPath("$[0].ticker").value("SPY"));
    }

    @Test
    void closingTrade_reducesPositionQuantity() throws Exception {
        int campaignId = createCampaign();

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """.formatted(campaignId)))
           .andExpect(status().isOk());

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"BTC 5 SPY 480C 12/20 @1.00"}
                        """.formatted(campaignId)))
           .andExpect(status().isOk());

        mvc.perform(get("/api/v1/positions").param("campaignId", String.valueOf(campaignId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].status").value("CLOSED"))
           .andExpect(jsonPath("$[0].openQuantity").value(0));
    }

    @Test
    void updateTrade_validFields_updatesLegAndRebuildsPositions() throws Exception {
        int campaignId = createCampaign();

        String tradeBody = mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """.formatted(campaignId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int tradeId = JsonPath.read(tradeBody, "$.id");

        mvc.perform(patch("/api/v1/trades/{id}", tradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quantity":3,"price":1.50,"strategyTag":"ADJ","notes":"adjusted"}
                        """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.quantity").value(3))
           .andExpect(jsonPath("$.price").value(1.50))
           .andExpect(jsonPath("$.strategyTag").value("ADJ"))
           .andExpect(jsonPath("$.notes").value("adjusted"));
    }

    @Test
    void updateTrade_nonexistentTrade_returns404() throws Exception {
        mvc.perform(patch("/api/v1/trades/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quantity":3}
                        """))
           .andExpect(status().isNotFound());
    }

    @Test
    void updateTrade_closedCampaign_returns400() throws Exception {
        int campaignId = createCampaign();

        String tradeBody = mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"campaignId":%d,"rawInput":"STO 5 SPY 480C 12/20 @2.35"}
                        """.formatted(campaignId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int tradeId = JsonPath.read(tradeBody, "$.id");

        mvc.perform(patch("/api/v1/campaigns/{id}/close", campaignId))
           .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/trades/{id}", tradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quantity":3}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("Cannot edit trades in a closed campaign"));
    }
}
