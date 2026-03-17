package com.scraper;

import com.scraper.model.MarketplaceListing;
import com.scraper.model.MarketplaceSource;
import com.scraper.repository.MarketplaceListingRepository;
import com.scraper.service.ListingDeduplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTests {

    @Mock
    private MarketplaceListingRepository repository;

    @InjectMocks
    private ListingDeduplicationService service;

    private MarketplaceListing listing(String externalId, String url) {
        return new MarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                externalId, "Test Listing", null,
                new BigDecimal("100.00"), "AUD",
                null, null, url, null,
                "test", OffsetDateTime.now(), null,
                null, null, "{}", null,
                null, null
        );
    }

    @Test
    void existingExternalId_isDuplicate() {
        when(repository.existsByMarketplaceSourceAndExternalId(
                MarketplaceSource.FACEBOOK_MARKETPLACE, "fb_123"))
                .thenReturn(true);

        assertTrue(service.isDuplicate(listing("fb_123", null)));
    }

    @Test
    void newExternalId_notDuplicate() {
        when(repository.existsByMarketplaceSourceAndExternalId(
                MarketplaceSource.FACEBOOK_MARKETPLACE, "fb_new"))
                .thenReturn(false);

        assertFalse(service.isDuplicate(listing("fb_new", null)));
    }

    @Test
    void noExternalId_fallsBackToUrlCheck_duplicate() {
        String url = "https://fb.com/marketplace/item/abc";

        when(repository.existsByMarketplaceSourceAndListingUrl(
                MarketplaceSource.FACEBOOK_MARKETPLACE, url))
                .thenReturn(true);

        assertTrue(service.isDuplicate(listing(null, url)));

        verify(repository, never()).existsByMarketplaceSourceAndExternalId(any(), any());
    }

    @Test
    void noExternalId_noUrl_notDuplicate() {
        assertFalse(service.isDuplicate(listing(null, null)));

        verify(repository, never()).existsByMarketplaceSourceAndExternalId(any(), any());
        verify(repository, never()).existsByMarketplaceSourceAndListingUrl(any(), any());
    }

    @Test
    void externalIdNotDuplicate_urlCheckNotCalled() {
        when(repository.existsByMarketplaceSourceAndExternalId(
                MarketplaceSource.FACEBOOK_MARKETPLACE, "fb_fresh"))
                .thenReturn(false);

        assertFalse(service.isDuplicate(listing("fb_fresh", "https://fb.com/item/1")));

        verify(repository, never()).existsByMarketplaceSourceAndListingUrl(any(), any());
    }
}
