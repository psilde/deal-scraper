package com.scraper.dto;

import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScrapeJobStatus;

import java.time.OffsetDateTime;

/** Public API representation of a {@link com.scraper.model.ScrapeJob}. */
public record ScrapeJobResponse(
        Long id,
        MarketplaceSource marketplaceSource,
        String searchQuery,
        ScrapeJobStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        int listingsFetched,
        int listingsInserted,
        int duplicatesSkipped,
        String errorMessage
) {
}
