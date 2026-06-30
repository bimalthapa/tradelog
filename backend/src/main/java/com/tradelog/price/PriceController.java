package com.tradelog.price;

import com.tradelog.price.dto.PriceResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @PostMapping("/batch")
    public Map<String, PriceResponse> batch(@RequestBody BatchRequest body) {
        if (body.tickers() == null || body.tickers().isEmpty()) {
            throw new IllegalArgumentException("tickers must not be empty");
        }
        if (body.tickers().size() > 50) {
            throw new IllegalArgumentException("tickers list exceeds maximum of 50");
        }
        return body.tickers().stream()
            .collect(Collectors.toMap(t -> t, priceService::fetchPrice));
    }

    record BatchRequest(List<String> tickers) {}
}
