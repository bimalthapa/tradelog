package com.tradelog.trade;

import com.tradelog.trade.dto.ParseTradeRequest;
import com.tradelog.trade.dto.ParseTradeResponse;
import com.tradelog.trade.dto.BatchSaveTradeRequest;
import com.tradelog.trade.dto.SaveTradeRequest;
import com.tradelog.trade.dto.TradeLegResponse;
import com.tradelog.trade.dto.UpdateTradeRequest;
import com.tradelog.trade.parser.ParsedTradeInput;
import com.tradelog.trade.parser.TradeInputParser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeInputParser parser;
    private final TradeEntryService tradeEntryService;

    public TradeController(TradeInputParser parser, TradeEntryService tradeEntryService) {
        this.parser = parser;
        this.tradeEntryService = tradeEntryService;
    }

    @PostMapping("/parse")
    public ParseTradeResponse parse(@RequestBody ParseTradeRequest req) {
        ParsedTradeInput p = parser.parse(req.rawInput());
        return new ParseTradeResponse(p.action(), p.qty(), p.ticker(), p.instrumentType(),
                p.optionType(), p.strike(), p.expiry(), p.price(), p.cashFlow(),
                p.strategy(), p.valid(), p.error());
    }

    @PostMapping
    public TradeLegResponse save(@RequestBody SaveTradeRequest req) {
        return tradeEntryService.save(req);
    }

    @PostMapping("/batch")
    public List<TradeLegResponse> saveBatch(@RequestBody BatchSaveTradeRequest req) {
        return tradeEntryService.saveBatch(req);
    }

    @PatchMapping("/{id}")
    public TradeLegResponse update(@PathVariable Long id, @RequestBody UpdateTradeRequest req) {
        return tradeEntryService.update(id, req);
    }

    @GetMapping
    public List<TradeLegResponse> list(@RequestParam Long campaignId) {
        return tradeEntryService.listByCampaign(campaignId);
    }
}
