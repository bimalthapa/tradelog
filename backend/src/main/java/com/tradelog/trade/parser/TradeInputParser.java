package com.tradelog.trade.parser;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TradeInputParser {

    private static final Pattern OPTIONS = Pattern.compile(
            "^(STO|BTO|BTC|STC)\\s+(\\d+)\\s+([A-Z]+)\\s+(\\d+)(C|P)\\s+(\\d{1,2}/\\d{1,2}(?:/\\d{4})?)\\s+@([\\d.]+)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STOCK = Pattern.compile(
            "^(BTO|STC|ASGN)\\s+(\\d+)\\s+([A-Z]+)\\s+@([\\d.]+)$",
            Pattern.CASE_INSENSITIVE);

    public ParsedTradeInput parse(String raw) {
        if (raw == null || raw.isBlank()) return ParsedTradeInput.invalid("Input is empty");
        String input = raw.trim();
        Matcher opt = OPTIONS.matcher(input);
        if (opt.matches()) return parseOptions(opt);
        Matcher stk = STOCK.matcher(input);
        if (stk.matches()) return parseStock(stk);
        return ParsedTradeInput.invalid("Unrecognized trade format");
    }

    private ParsedTradeInput parseOptions(Matcher m) {
        String action = m.group(1).toUpperCase();
        int qty = Integer.parseInt(m.group(2));
        String ticker = m.group(3).toUpperCase();
        double strike = Double.parseDouble(m.group(4));
        String optionType = m.group(5).equalsIgnoreCase("C") ? "CALL" : "PUT";
        LocalDate expiry = parseExpiry(m.group(6));
        double price = Double.parseDouble(m.group(7));
        double cashFlow = qty * price * 100;
        if (action.equals("BTO") || action.equals("BTC")) cashFlow = -cashFlow;
        String strategy = switch (action) {
            case "STO" -> optionType.equals("PUT") ? "CSP" : "CC";
            case "BTO" -> "Long";
            default -> "Close";
        };
        return new ParsedTradeInput(action, qty, ticker, "OPTION", optionType, strike, expiry,
                price, cashFlow, strategy, true, null);
    }

    private ParsedTradeInput parseStock(Matcher m) {
        String rawAction = m.group(1).toUpperCase();
        String action = rawAction.equals("ASGN") ? "ASSIGNED" : rawAction;
        int qty = Integer.parseInt(m.group(2));
        String ticker = m.group(3).toUpperCase();
        double price = Double.parseDouble(m.group(4));
        double cashFlow = qty * price;
        if (action.equals("BTO") || action.equals("ASSIGNED")) cashFlow = -cashFlow;
        String strategy = switch (rawAction) {
            case "BTO" -> "Long";
            case "ASGN" -> "Assignment";
            default -> "Close";
        };
        return new ParsedTradeInput(action, qty, ticker, "STOCK", null, null, null,
                price, cashFlow, strategy, true, null);
    }

    public LocalDate parseExpiry(String s) {
        String[] p = s.split("/");
        int month = Integer.parseInt(p[0]);
        int day = Integer.parseInt(p[1]);
        int year = p.length == 3 ? Integer.parseInt(p[2]) : LocalDate.now().getYear();
        return LocalDate.of(year, month, day);
    }
}
