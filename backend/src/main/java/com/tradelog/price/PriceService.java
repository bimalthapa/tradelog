package com.tradelog.price;

import com.tradelog.price.dto.PriceResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Service
public class PriceService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PriceService.class);

    private static final String YAHOO_URL =
        "https://query1.finance.yahoo.com/v8/finance/chart/{ticker}";

    private final RestClient restClient;

    public PriceService(RestClient priceRestClient) {
        this.restClient = priceRestClient;
    }

    @Cacheable(value = "prices", key = "#ticker", unless = "#result.price == null")
    public PriceResponse fetchPrice(String ticker) {
        try {
            YahooApiResponse body = restClient.get()
                .uri(YAHOO_URL, ticker)
                .retrieve()
                .body(YahooApiResponse.class);

            Double price = extractPrice(body);
            String source = price != null ? "yahoo" : "unavailable";
            return new PriceResponse(ticker, price, source, Instant.now());
        } catch (Exception e) {
            log.warn("Price fetch failed for ticker {}: {}", ticker, e.getMessage());
            return new PriceResponse(ticker, null, "unavailable", Instant.now());
        }
    }

    private Double extractPrice(YahooApiResponse body) {
        if (body == null || body.chart() == null) return null;
        List<YahooResult> results = body.chart().result();
        if (results == null || results.isEmpty()) return null;
        YahooMeta meta = results.getFirst().meta();
        return meta != null ? meta.regularMarketPrice() : null;
    }

    private record YahooMeta(Double regularMarketPrice) {}
    private record YahooResult(YahooMeta meta) {}
    private record YahooChart(List<YahooResult> result) {}
    private record YahooApiResponse(YahooChart chart) {}
}
