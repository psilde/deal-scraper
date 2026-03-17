package com.scraper;

import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceListing;
import com.scraper.model.MarketplaceSource;
import com.scraper.service.ListingNormalisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NormalisationServiceTests {

    private ListingNormalisationService service;

    @BeforeEach
    void setUp() {
        service = new ListingNormalisationService();
    }

    @Test
    void validListing_normalisedCorrectly() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_123",
                "  RTX 4070  Super  ",
                "Great card",
                new BigDecimal("450.00"),
                "gbp",
                "London",
                "seller1",
                "https://fb.com/item/123",
                "https://fb.com/img/123.jpg",
                "rtx 4070",
                OffsetDateTime.now().minusDays(1),
                "Used - Good",
                "Electronics",
                "{}",
                null, null
        );

        MarketplaceListing result = service.normalise(raw, 42L);

        assertNotNull(result);
        assertEquals("RTX 4070 Super", result.getTitle());
        assertEquals("GBP", result.getCurrency());
        assertEquals("rtx 4070", result.getSearchQuery());
        assertEquals(42L, result.getScrapeJobId());
        assertNotNull(result.getScrapedAt());
    }

    @Test
    void blankTitle_returnsNull() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_456", "   ", null,
                new BigDecimal("100.00"), null, null, null, null, null,
                "test", null, null, null, null, null, null
        );

        assertNull(service.normalise(raw, null));
    }

    @Test
    void nullTitle_returnsNull() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_789", null, null,
                new BigDecimal("100.00"), null, null, null, null, null,
                "test", null, null, null, null, null, null
        );

        assertNull(service.normalise(raw, null));
    }

    @Test
    void zeroPrice_normalisedAsFreeListing() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_000", "Free Sofa", null,
                BigDecimal.ZERO, "AUD", null, null, null, null,
                "sofa", null, null, null, null, null, null
        );

        MarketplaceListing result = service.normalise(raw, null);

        assertNotNull(result);
        assertEquals(0, result.getPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void negativePrice_returnsNull() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_001", "iPhone 15", null,
                new BigDecimal("-50.00"), "GBP", null, null, null, null,
                "iphone", null, null, null, null, null, null
        );

        assertNull(service.normalise(raw, null));
    }

    @Test
    void nullCurrency_defaultsToAUD() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_002", "Gaming Chair", null,
                new BigDecimal("120.00"), null, null, null, null, null,
                "gaming chair", null, null, null, null, null, null
        );

        MarketplaceListing result = service.normalise(raw, null);

        assertNotNull(result);
        assertEquals("AUD", result.getCurrency());
    }

    @Test
    void nullExternalId_normalisedToNull() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                null, "Sofa", null,
                new BigDecimal("200.00"), "GBP", null, null, null, null,
                "sofa", null, null, null, null, null, null
        );

        MarketplaceListing result = service.normalise(raw, null);

        assertNotNull(result);
        assertNull(result.getExternalId());
    }

    @Test
    void sourceMetadata_passedThrough() {
        RawMarketplaceListing raw = new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                "fb_cat_1", "Toyota Camry", null,
                new BigDecimal("15000.00"), "AUD", "Perth WA", null, null, null,
                "vehicles", null, null, null, null, "Vehicles", "Perth"
        );

        MarketplaceListing result = service.normalise(raw, null);

        assertNotNull(result);
        assertEquals("Vehicles", result.getSourceCategory());
        assertEquals("Perth", result.getSourceCity());
    }
}
