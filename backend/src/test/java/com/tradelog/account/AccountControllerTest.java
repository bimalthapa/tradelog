package com.tradelog.account;

import com.jayway.jsonpath.JsonPath;
import com.tradelog.campaign.Campaign;
import com.tradelog.campaign.CampaignRepository;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:${java.io.tmpdir}/tradelog-test.db"
})
class AccountControllerTest {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    CampaignRepository campaignRepository;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void listReturnsAccounts() throws Exception {
        createAccount("IRA");
        createAccount("Taxable");

        mvc.perform(get("/api/v1/accounts"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$[0].name").value("IRA"))
           .andExpect(jsonPath("$[1].name").value("Taxable"));
    }

    @Test
    void createReturns201() throws Exception {
        mvc.perform(post("/api/v1/accounts")
               .contentType(MediaType.APPLICATION_JSON)
               .content("""
                       {"name":"IRA"}
                       """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").isNumber())
           .andExpect(jsonPath("$.name").value("IRA"));
    }

    @Test
    void renameReturnsUpdated() throws Exception {
        int id = createAccountAndGetId("IRA");

        mvc.perform(patch("/api/v1/accounts/" + id)
               .contentType(MediaType.APPLICATION_JSON)
               .content("""
                       {"name":"Roth IRA"}
                       """))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("Roth IRA"));
    }

    @Test
    void deleteNoContentWhenEmpty() throws Exception {
        int id = createAccountAndGetId("ToDelete");

        mvc.perform(delete("/api/v1/accounts/" + id))
           .andExpect(status().isNoContent());
    }

    @Test
    void deleteConflictWhenCampaignAssigned() throws Exception {
        int id = createAccountAndGetId("Blocked");

        Account account = accountRepository.findById((long) id).orElseThrow();
        Campaign campaign = new Campaign();
        campaign.setTicker("SPY");
        campaign.setOpenedAt(LocalDate.now());
        campaign.setAccount(account);
        campaignRepository.save(campaign);

        mvc.perform(delete("/api/v1/accounts/" + id))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.error").value("Cannot delete account with assigned campaigns"));
    }

    private void createAccount(String name) throws Exception {
        mvc.perform(post("/api/v1/accounts")
               .contentType(MediaType.APPLICATION_JSON)
               .content("""
                       {"name":"%s"}
                       """.formatted(name)))
           .andExpect(status().isCreated());
    }

    private int createAccountAndGetId(String name) throws Exception {
        String body = mvc.perform(post("/api/v1/accounts")
               .contentType(MediaType.APPLICATION_JSON)
               .content("""
                       {"name":"%s"}
                       """.formatted(name)))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
