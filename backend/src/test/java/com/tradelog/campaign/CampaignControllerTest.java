package com.tradelog.campaign;

import com.jayway.jsonpath.JsonPath;
import com.tradelog.account.AccountRepository;
import com.tradelog.account.Account;
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
class CampaignControllerTest {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void createCampaign_returnsCreatedWithComputedFields() throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"NVDA","label":"Wheel","notes":"test","openedAt":"2026-01-10"}
                        """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").isNumber())
           .andExpect(jsonPath("$.ticker").value("NVDA"))
           .andExpect(jsonPath("$.label").value("Wheel"))
           .andExpect(jsonPath("$.status").value("OPEN"))
           .andExpect(jsonPath("$.openedAt").value("2026-01-10"))
           .andExpect(jsonPath("$.netCashFlow").value(0.0))
           .andExpect(jsonPath("$.openPositionCount").value(0))
           .andExpect(jsonPath("$.accountId").value((Object) null));
    }

    @Test
    void createCampaign_withAccountId_setsAccount() throws Exception {
        Account account = accountRepository.save(new Account("Roth IRA"));

        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"NVDA","openedAt":"2026-01-10","accountId":%d}
                        """.formatted(account.getId())))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.accountId").value(account.getId().intValue()))
           .andExpect(jsonPath("$.accountName").value("Roth IRA"));
    }

    @Test
    void createCampaign_withUnknownAccountId_returns404() throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"NVDA","openedAt":"2026-01-10","accountId":99999}
                        """))
           .andExpect(status().isNotFound());
    }

    @Test
    void createCampaign_rejectsLowercaseTicker() throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"nvda","openedAt":"2026-01-01"}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors.ticker").exists());
    }

    @Test
    void createCampaign_rejectsMissingOpenedAt() throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"NVDA"}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors.openedAt").exists());
    }

    @Test
    void listCampaigns_returnsEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/campaigns"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listCampaigns_orderedByOpenedAtDesc() throws Exception {
        create("SPY", "2026-01-01");
        create("NVDA", "2026-03-01");
        create("AAPL", "2026-02-01");

        mvc.perform(get("/api/v1/campaigns"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].ticker").value("NVDA"))
           .andExpect(jsonPath("$[1].ticker").value("AAPL"))
           .andExpect(jsonPath("$[2].ticker").value("SPY"));
    }

    @Test
    void listCampaigns_filterByAccountId_returnsOnlyMatching() throws Exception {
        Account account = accountRepository.save(new Account("IRA"));
        int assignedId = createAndGetId("NVDA", "2026-01-01");
        create("SPY", "2026-02-01");  // no account

        mvc.perform(patch("/api/v1/campaigns/" + assignedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":%d}
                        """.formatted(account.getId())))
           .andExpect(status().isOk());

        mvc.perform(get("/api/v1/campaigns?accountId=" + account.getId()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].ticker").value("NVDA"));
    }

    @Test
    void listCampaigns_filterUnassigned_returnsOnlyNullAccount() throws Exception {
        Account account = accountRepository.save(new Account("IRA"));
        int assignedId = createAndGetId("NVDA", "2026-01-01");
        create("SPY", "2026-02-01");  // unassigned

        mvc.perform(patch("/api/v1/campaigns/" + assignedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":%d}
                        """.formatted(account.getId())))
           .andExpect(status().isOk());

        mvc.perform(get("/api/v1/campaigns?unassigned=true"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].ticker").value("SPY"));
    }

    @Test
    void getCampaign_returnsById() throws Exception {
        int id = createAndGetId("TSLA", "2026-01-15");

        mvc.perform(get("/api/v1/campaigns/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.ticker").value("TSLA"))
           .andExpect(jsonPath("$.openedAt").value("2026-01-15"));
    }

    @Test
    void getCampaign_returns404ForUnknownId() throws Exception {
        mvc.perform(get("/api/v1/campaigns/99999"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void patchCampaign_assignAccount() throws Exception {
        Account account = accountRepository.save(new Account("Taxable"));
        int id = createAndGetId("AMD", "2026-01-01");

        mvc.perform(patch("/api/v1/campaigns/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":%d}
                        """.formatted(account.getId())))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.accountId").value(account.getId().intValue()))
           .andExpect(jsonPath("$.accountName").value("Taxable"));
    }

    @Test
    void patchCampaign_clearAccount() throws Exception {
        Account account = accountRepository.save(new Account("Taxable"));
        int id = createAndGetId("AMD", "2026-01-01");

        // first assign
        mvc.perform(patch("/api/v1/campaigns/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":%d}
                        """.formatted(account.getId())))
           .andExpect(status().isOk());

        // then clear
        mvc.perform(patch("/api/v1/campaigns/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clearAccount":true}
                        """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.accountId").value((Object) null));
    }

    @Test
    void patchCampaign_returns404ForUnknownId() throws Exception {
        mvc.perform(patch("/api/v1/campaigns/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":1}
                        """))
           .andExpect(status().isNotFound());
    }

    @Test
    void patchCampaign_returns404ForUnknownAccountId() throws Exception {
        int id = createAndGetId("NVDA", "2026-01-01");

        mvc.perform(patch("/api/v1/campaigns/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":99999}
                        """))
           .andExpect(status().isNotFound());
    }

    @Test
    void closeCampaign_setsStatusAndClosedAt() throws Exception {
        int id = createAndGetId("AMD", "2026-01-01");

        mvc.perform(patch("/api/v1/campaigns/" + id + "/close"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CLOSED"))
           .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void closeCampaign_returns404ForUnknownId() throws Exception {
        mvc.perform(patch("/api/v1/campaigns/99999/close"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").exists());
    }

    private void create(String ticker, String openedAt) throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"%s","openedAt":"%s"}
                        """.formatted(ticker, openedAt)))
           .andExpect(status().isCreated());
    }

    private int createAndGetId(String ticker, String openedAt) throws Exception {
        String body = mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ticker":"%s","openedAt":"%s"}
                        """.formatted(ticker, openedAt)))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
