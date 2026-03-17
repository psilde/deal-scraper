package com.scraper.dto;

import com.scraper.model.MarketplaceSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RawMarketplaceListing(
        MarketplaceSource source,
        String externalId,
        String title,
        String description,
        BigDecimal price,
        String currency,
        String location,
        String sellerName,
        String listingUrl,
        String imageUrl,
        String searchQuery,
        OffsetDateTime listedAt,
        String condition,
        String category,
        String rawPayload,
        String sourceCategory,
        String sourceCity
) {
}
