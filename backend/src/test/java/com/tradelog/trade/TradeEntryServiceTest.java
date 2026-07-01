package com.tradelog.trade;

import com.tradelog.campaign.Campaign;
import com.tradelog.campaign.CampaignRepository;
import com.tradelog.common.exception.ResourceNotFoundException;
import com.tradelog.position.Position;
import com.tradelog.position.PositionRepository;
import com.tradelog.position.PositionService;
import com.tradelog.trade.dto.RollTradeRequest;
import com.tradelog.trade.dto.SaveTradeRequest;
import com.tradelog.trade.dto.TradeLegResponse;
import com.tradelog.trade.parser.ParsedTradeInput;
import com.tradelog.trade.parser.TradeInputParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeEntryServiceTest {

    @Mock TradeInputParser parser;
    @Mock CampaignRepository campaignRepository;
    @Mock TradeEntryRepository tradeEntryRepository;
    @Mock TradeLegRepository tradeLegRepository;
    @Mock PositionService positionService;
    @Mock PositionRepository positionRepository;

    @InjectMocks
    TradeEntryService service;

    // ── helpers ──────────────────────────────────────────────────────────────

    private ParsedTradeInput validOptionParsed() {
        return new ParsedTradeInput(
                "STO", 5, "SPY", "OPTION", "PUT",
                480.0, LocalDate.of(2026, 12, 20),
                2.35, 1175.0, "CSP", true, null);
    }

    private TradeEntry makeEntry(Long id, Long campaignId, String strategyTag, String notes) {
        TradeEntry e = new TradeEntry();
        e.setCampaignId(campaignId);
        e.setStrategyTag(strategyTag);
        e.setNotes(notes);
        setId(e, id);
        return e;
    }

    private TradeLeg makeLeg(Long id, Long entryId, Long campaignId) {
        TradeLeg leg = new TradeLeg();
        leg.setTradeEntryId(entryId);
        leg.setCampaignId(campaignId);
        leg.setInstrumentType("OPTION");
        leg.setAction("STO");
        leg.setTicker("SPY");
        leg.setQuantity(5);
        leg.setPrice(2.35);
        leg.setNetCashFlow(1175.0);
        leg.setOptionType("PUT");
        leg.setStrike(480.0);
        leg.setExpiry(LocalDate.of(2026, 12, 20));
        leg.setTradedAt(LocalDate.now());
        setId(leg, id);
        return leg;
    }

    private static void setId(Object target, Long id) {
        try {
            Field f = target.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── save() tests ─────────────────────────────────────────────────────────

    @Test
    void save_throwsNotFound_whenCampaignMissing() {
        when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

        SaveTradeRequest req = new SaveTradeRequest(99L, "STO 5 SPY 480P 12/20 @2.35", null, null, null);

        assertThatThrownBy(() -> service.save(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void save_throwsIllegalArgument_whenParseInvalid() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse(any())).thenReturn(ParsedTradeInput.invalid("Unrecognized trade format"));

        SaveTradeRequest req = new SaveTradeRequest(1L, "garbage input", null, null, null);

        assertThatThrownBy(() -> service.save(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unrecognized trade format");
    }

    @Test
    void save_createsEntryAndLeg_andCallsApplyLeg() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse("STO 5 SPY 480P 12/20 @2.35")).thenReturn(validOptionParsed());

        TradeEntry fakeEntry = makeEntry(10L, 1L, "CSP", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        TradeLeg fakeLeg = makeLeg(20L, 10L, 1L);
        when(tradeLegRepository.save(any())).thenReturn(fakeLeg);

        SaveTradeRequest req = new SaveTradeRequest(1L, "STO 5 SPY 480P 12/20 @2.35", "CSP", null, null);
        TradeLegResponse resp = service.save(req);

        verify(tradeEntryRepository).save(any(TradeEntry.class));
        verify(tradeLegRepository).save(any(TradeLeg.class));
        verify(positionService).applyLeg(fakeLeg);

        assertThat(resp.id()).isEqualTo(20L);
        assertThat(resp.tradeEntryId()).isEqualTo(10L);
        assertThat(resp.campaignId()).isEqualTo(1L);
        assertThat(resp.action()).isEqualTo("STO");
        assertThat(resp.ticker()).isEqualTo("SPY");
        assertThat(resp.quantity()).isEqualTo(5);
        assertThat(resp.price()).isEqualTo(2.35);
        assertThat(resp.netCashFlow()).isEqualTo(1175.0);
        assertThat(resp.optionType()).isEqualTo("PUT");
        assertThat(resp.strike()).isEqualTo(480.0);
        assertThat(resp.strategyTag()).isEqualTo("CSP");
    }

    @Test
    void save_usesParserStrategy_whenStrategyTagIsNull() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse(any())).thenReturn(validOptionParsed());

        ArgumentCaptor<TradeEntry> entryCaptor = ArgumentCaptor.forClass(TradeEntry.class);
        TradeEntry fakeEntry = makeEntry(10L, 1L, "CSP", null);
        when(tradeEntryRepository.save(entryCaptor.capture())).thenReturn(fakeEntry);
        when(tradeLegRepository.save(any())).thenReturn(makeLeg(20L, 10L, 1L));

        // strategyTag is null in request — should fall back to parsed.strategy() = "CSP"
        SaveTradeRequest req = new SaveTradeRequest(1L, "STO 5 SPY 480P 12/20 @2.35", null, null, null);
        service.save(req);

        assertThat(entryCaptor.getValue().getStrategyTag()).isEqualTo("CSP");
    }

    @Test
    void save_usesTradedAt_whenProvided() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse(any())).thenReturn(validOptionParsed());

        TradeEntry fakeEntry = makeEntry(10L, 1L, "CSP", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        ArgumentCaptor<TradeLeg> legCaptor = ArgumentCaptor.forClass(TradeLeg.class);
        when(tradeLegRepository.save(legCaptor.capture())).thenReturn(makeLeg(20L, 10L, 1L));

        LocalDate backDate = LocalDate.of(2026, 6, 25);
        SaveTradeRequest req = new SaveTradeRequest(1L, "STO 5 SPY 480P 12/20 @2.35", null, null, backDate);
        service.save(req);

        assertThat(legCaptor.getValue().getTradedAt()).isEqualTo(backDate);
    }

    @Test
    void save_defaultsToToday_whenTradedAtNull() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse(any())).thenReturn(validOptionParsed());

        TradeEntry fakeEntry = makeEntry(10L, 1L, "CSP", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        ArgumentCaptor<TradeLeg> legCaptor = ArgumentCaptor.forClass(TradeLeg.class);
        when(tradeLegRepository.save(legCaptor.capture())).thenReturn(makeLeg(20L, 10L, 1L));

        SaveTradeRequest req = new SaveTradeRequest(1L, "STO 5 SPY 480P 12/20 @2.35", null, null, null);
        service.save(req);

        assertThat(legCaptor.getValue().getTradedAt()).isEqualTo(LocalDate.now());
    }

    // ── listByCampaign() tests ────────────────────────────────────────────────

    @Test
    void listByCampaign_returnsLegsWithEntryMetadata() {
        TradeLeg leg = makeLeg(20L, 10L, 1L);
        TradeEntry entry = makeEntry(10L, 1L, "CSP", "a note");

        when(tradeLegRepository.findByCampaignIdOrderByTradedAtAsc(1L)).thenReturn(List.of(leg));
        when(tradeEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

        List<TradeLegResponse> results = service.listByCampaign(1L);

        assertThat(results).hasSize(1);
        TradeLegResponse r = results.get(0);
        assertThat(r.strategyTag()).isEqualTo("CSP");
        assertThat(r.notes()).isEqualTo("a note");
        assertThat(r.id()).isEqualTo(20L);
        assertThat(r.tradeEntryId()).isEqualTo(10L);
    }

    @Test
    void listByCampaign_returnsEmpty_whenNoLegs() {
        when(tradeLegRepository.findByCampaignIdOrderByTradedAtAsc(99L)).thenReturn(List.of());

        List<TradeLegResponse> results = service.listByCampaign(99L);

        assertThat(results).isEmpty();
    }

    // ── saveBatch() tests ─────────────────────────────────────────────────────

    @Test
    void saveBatch_throwsNotFound_whenCampaignMissing() {
        when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

        var req = new com.tradelog.trade.dto.BatchSaveTradeRequest(
                99L, List.of("STO 5 SPY 480P 12/20 @2.35"), null, null, null);

        assertThatThrownBy(() -> service.saveBatch(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void saveBatch_createsOneEntryAndTwoLegs_andCallsApplyLegTwice() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));

        ParsedTradeInput stoLeg = new ParsedTradeInput(
                "STO", 5, "SPY", "OPTION", "CALL",
                480.0, LocalDate.of(2026, 12, 20), 2.35, 1175.0, "CC", true, null);
        ParsedTradeInput btoLeg = new ParsedTradeInput(
                "BTO", 5, "SPY", "OPTION", "CALL",
                485.0, LocalDate.of(2026, 12, 20), 0.85, -425.0, "Long", true, null);

        when(parser.parse("STO 5 SPY 480C 12/20 @2.35")).thenReturn(stoLeg);
        when(parser.parse("BTO 5 SPY 485C 12/20 @0.85")).thenReturn(btoLeg);

        TradeEntry fakeEntry = makeEntry(10L, 1L, "Bear Call Spread", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        TradeLeg fakeLeg1 = makeLeg(20L, 10L, 1L);
        TradeLeg fakeLeg2 = makeLeg(21L, 10L, 1L);
        when(tradeLegRepository.save(any()))
                .thenReturn(fakeLeg1)
                .thenReturn(fakeLeg2);

        var req = new com.tradelog.trade.dto.BatchSaveTradeRequest(
                1L,
                List.of("STO 5 SPY 480C 12/20 @2.35", "BTO 5 SPY 485C 12/20 @0.85"),
                "Bear Call Spread", null, LocalDate.now());

        List<TradeLegResponse> responses = service.saveBatch(req);

        verify(tradeEntryRepository, times(1)).save(any(TradeEntry.class));
        verify(tradeLegRepository, times(2)).save(any(TradeLeg.class));
        verify(positionService, times(2)).applyLeg(any(TradeLeg.class));
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).tradeEntryId()).isEqualTo(10L);
        assertThat(responses.get(1).tradeEntryId()).isEqualTo(10L);
    }

    @Test
    void saveBatch_throwsIllegalArgument_whenAnyLegInvalid() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse("STO 5 SPY 480C 12/20 @2.35")).thenReturn(validOptionParsed());
        when(parser.parse("garbage")).thenReturn(ParsedTradeInput.invalid("Unrecognized trade format"));

        when(tradeEntryRepository.save(any())).thenReturn(makeEntry(10L, 1L, null, null));
        when(tradeLegRepository.save(any())).thenReturn(makeLeg(20L, 10L, 1L));

        var req = new com.tradelog.trade.dto.BatchSaveTradeRequest(
                1L, List.of("STO 5 SPY 480C 12/20 @2.35", "garbage"), null, null, null);

        assertThatThrownBy(() -> service.saveBatch(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("garbage");
    }

    @Test
    void saveBatch_storedRawInputIsLegsJoinedWithComma() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(new Campaign()));
        when(parser.parse(any())).thenReturn(validOptionParsed());

        ArgumentCaptor<TradeEntry> entryCaptor = ArgumentCaptor.forClass(TradeEntry.class);
        TradeEntry fakeEntry = makeEntry(10L, 1L, null, null);
        when(tradeEntryRepository.save(entryCaptor.capture())).thenReturn(fakeEntry);
        when(tradeLegRepository.save(any())).thenReturn(makeLeg(20L, 10L, 1L));

        var req = new com.tradelog.trade.dto.BatchSaveTradeRequest(
                1L,
                List.of("STO 5 SPY 480C 12/20 @2.35", "BTO 5 SPY 485C 12/20 @0.85"),
                null, null, null);
        service.saveBatch(req);

        assertThat(entryCaptor.getValue().getRawInput())
                .isEqualTo("STO 5 SPY 480C 12/20 @2.35, BTO 5 SPY 485C 12/20 @0.85");
    }

    // ── saveRoll() helpers ────────────────────────────────────────────────────

    private Position makeOpenOptionPosition(Long id, Long campaignId, Long openingLegId) {
        Position p = new Position();
        try {
            java.lang.reflect.Field f = Position.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        p.setCampaignId(campaignId);
        p.setOpeningLegId(openingLegId);
        p.setInstrumentType("OPTION");
        p.setTicker("SPY");
        p.setOptionType("PUT");
        p.setStrike(480.0);
        p.setExpiry(LocalDate.of(2026, 12, 20));
        p.setOpenAction("STO");
        p.setOpenQuantity(5);
        p.setAvgPrice(2.35);
        p.setStatus("OPEN");
        p.setOpenedAt(LocalDate.now());
        return p;
    }

    // ── saveRoll() tests ──────────────────────────────────────────────────────

    @Test
    void saveRoll_throwsNotFound_whenPositionMissing() {
        when(positionRepository.findById(99L)).thenReturn(Optional.empty());

        RollTradeRequest req = new RollTradeRequest(1L, 99L, 5, 1.80, 470.0, "1/17", 2.10, null, null);

        assertThatThrownBy(() -> service.saveRoll(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void saveRoll_throwsBadRequest_whenPositionClosed() {
        Position closed = makeOpenOptionPosition(10L, 1L, 5L);
        closed.setStatus("CLOSED");
        when(positionRepository.findById(10L)).thenReturn(Optional.of(closed));

        RollTradeRequest req = new RollTradeRequest(1L, 10L, 5, 1.80, 470.0, "1/17", 2.10, null, null);

        assertThatThrownBy(() -> service.saveRoll(req))
                .isInstanceOf(com.tradelog.common.exception.BadRequestException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void saveRoll_createsBtcAndStoLegs_withClosesLegIdOnBtc() {
        Position position = makeOpenOptionPosition(10L, 1L, 5L);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(position));

        TradeEntry fakeEntry = makeEntry(20L, 1L, "Roll", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        TradeLeg fakeBtcLeg = makeLeg(30L, 20L, 1L);
        fakeBtcLeg.setAction("BTC");
        fakeBtcLeg.setClosesLegId(5L);
        fakeBtcLeg.setPrice(1.80);
        fakeBtcLeg.setNetCashFlow(-900.0);

        TradeLeg fakeStoLeg = makeLeg(31L, 20L, 1L);
        fakeStoLeg.setAction("STO");
        fakeStoLeg.setStrike(470.0);
        fakeStoLeg.setPrice(2.10);
        fakeStoLeg.setNetCashFlow(1050.0);

        when(tradeLegRepository.save(any()))
                .thenReturn(fakeBtcLeg)
                .thenReturn(fakeStoLeg);

        RollTradeRequest req = new RollTradeRequest(1L, 10L, 5, 1.80, 470.0, "1/17", 2.10, null, null);
        List<TradeLegResponse> responses = service.saveRoll(req);

        verify(tradeEntryRepository, times(1)).save(any(TradeEntry.class));
        verify(tradeLegRepository, times(2)).save(any(TradeLeg.class));
        verify(positionService, times(2)).applyLeg(any(TradeLeg.class));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).action()).isEqualTo("BTC");
        assertThat(responses.get(0).closesLegId()).isEqualTo(5L);
        assertThat(responses.get(1).action()).isEqualTo("STO");
        assertThat(responses.get(1).closesLegId()).isNull();
    }

    @Test
    void saveRoll_createsStcAndBtoLegs_whenRollingLongOption() {
        Position position = makeOpenOptionPosition(10L, 1L, 5L);
        position.setOpenAction("BTO");
        when(positionRepository.findById(10L)).thenReturn(Optional.of(position));

        TradeEntry fakeEntry = makeEntry(20L, 1L, "Roll", null);
        when(tradeEntryRepository.save(any())).thenReturn(fakeEntry);

        ArgumentCaptor<TradeLeg> legCaptor = ArgumentCaptor.forClass(TradeLeg.class);
        when(tradeLegRepository.save(legCaptor.capture()))
                .thenReturn(makeLeg(30L, 20L, 1L))
                .thenReturn(makeLeg(31L, 20L, 1L));

        RollTradeRequest req = new RollTradeRequest(1L, 10L, 5, 1.80, 470.0, "1/17", 2.10, null, null);
        service.saveRoll(req);

        List<TradeLeg> savedLegs = legCaptor.getAllValues();
        assertThat(savedLegs.get(0).getAction()).isEqualTo("STC");
        assertThat(savedLegs.get(0).getNetCashFlow()).isEqualTo(900.0);
        assertThat(savedLegs.get(1).getAction()).isEqualTo("BTO");
        assertThat(savedLegs.get(1).getNetCashFlow()).isEqualTo(-1050.0);
    }
}
