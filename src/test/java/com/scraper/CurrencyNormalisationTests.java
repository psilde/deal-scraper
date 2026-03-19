package com.scraper;

import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceListing;
import com.scraper.model.MarketplaceSource;
import com.scraper.service.ListingNormalisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyNormalisationTests {

    private ListingNormalisationService service;

    @BeforeEach
    void setUp() {
        service = new ListingNormalisationService();
    }

    @ParameterizedTest(name = "currency \"{0}\" -> \"{1}\"")
    @CsvSource({
            "AUD, AUD",
            "USD, USD",
            "GBP, GBP",
            "EUR, EUR",
            "aud, AUD",
            "gbp, GBP",
            "eur, EUR",
            "usd, USD",
            "  AUD  , AUD",
            "  gbp  , GBP"
    })
    void currencyCodeUpperCase(String rawCurrency, String expectedCurrency) {
        MarketplaceListing result = service.normalise(listing("100.00", rawCurrency), null);
        assertNotNull(result);
        assertEquals(expectedCurrency, result.getCurrency());
    }

    @Test
    void audStored() {
        assertEquals("AUD", service.normalise(listing("450.00", "AUD"), null).getCurrency());
    }

    @Test
    void usdStored() {
        assertEquals("USD", service.normalise(listing("299.99", "USD"), null).getCurrency());
    }

    @Test
    void gbpStored() {
        assertEquals("GBP", service.normalise(listing("200.00", "GBP"), null).getCurrency());
    }

    @Test
    void eurStored() {
        assertEquals("EUR", service.normalise(listing("350.00", "EUR"), null).getCurrency());
    }

    @Test
    void poundSymbolPreserved() {
        // scraper may pass "£" directly; no map symbol to "GBP"
        assertEquals("£", service.normalise(listing("150.00", "£"), null).getCurrency());
    }

    @Test
    void zeroPriceAccepted() {
        MarketplaceListing result = service.normalise(listing("0.00", "AUD"), null);
        assertNotNull(result);
        assertEquals(0, result.getPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void zeroPriceNullCurrencyDefaultsToAUD() {
        MarketplaceListing result = service.normalise(listing("0.00", null), null);
        assertNotNull(result);
        assertEquals("AUD", result.getCurrency());
    }

    @Test
    void nullCurrencyDefaultsToAUD() {
        assertEquals("AUD", service.normalise(listing("100.00", null), null).getCurrency());
    }

    @Test
    void blankCurrencyDefaultsToAUD() {
        assertEquals("AUD", service.normalise(listing("100.00", ""), null).getCurrency());
    }

    @Test
    void whitespaceCurrencyDefaultsToAUD() {
        assertEquals("AUD", service.normalise(listing("100.00", "   "), null).getCurrency());
    }

    @Test
    void unknownCurrencyPreserved() {
        assertEquals("JPY", service.normalise(listing("5000.00", "JPY"), null).getCurrency());
    }

    @Test
    void euroSymbolPreserved() {
        assertEquals("€", service.normalise(listing("80.00", "€"), null).getCurrency());
    }

    @Test
    void negativePriceRejected() {
        assertNull(service.normalise(listing("-10.00", "AUD"), null));
    }

    @Test
    void nullPriceRejected() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_null_price", "Some Item", null,
                null, "AUD", null, null, null, null,
                "test", null, null, null, null, null, null
        );
        assertNull(service.normalise(raw, null));
    }

    private RawMarketplaceListing listing(String price, String currency) {
        return new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_test", "Test Item", null,
                new BigDecimal(price), currency,
                null, null, null, null,
                "test", null, null, null, null, null, null
        );
    }
}
