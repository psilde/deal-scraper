package com.scraper.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ScrapeMetricsCollector {

    private static final ThreadLocal<Integer> scrollIterations =
            ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Map<String, Integer>> currencyFormats =
            ThreadLocal.withInitial(ScrapeMetricsCollector::blankCurrencyMap);

    private static Map<String, Integer> blankCurrencyMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("AUD", 0);
        m.put("USD", 0);
        m.put("GBP", 0);
        m.put("EUR", 0);
        m.put("FREE", 0);
        m.put("OTHER", 0);
        return m;
    }

    public void reset() {
        scrollIterations.set(0);
        currencyFormats.set(blankCurrencyMap());
    }

    public void recordScrollIterations(int count) {
        scrollIterations.set(count);
    }

    public int getScrollIterations() {
        return scrollIterations.get();
    }

    public void recordCurrencyFormat(String format) {
        currencyFormats.get().merge(format, 1, Integer::sum);
    }

    public Map<String, Integer> getCurrencyFormatCounts() {
        return new LinkedHashMap<>(currencyFormats.get());
    }
}
