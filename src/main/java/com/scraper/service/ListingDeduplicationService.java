package com.scraper.service;

import com.scraper.model.MarketplaceListing;
import com.scraper.repository.MarketplaceListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(ListingDeduplicationService.class);

    private final MarketplaceListingRepository marketplaceListingRepository;

    public ListingDeduplicationService(MarketplaceListingRepository marketplaceListingRepository) {
        this.marketplaceListingRepository = marketplaceListingRepository;
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(MarketplaceListing listing) {
        if (listing.getExternalId() != null && !listing.getExternalId().isBlank()) {
            boolean exists = marketplaceListingRepository
                    .existsByMarketplaceSourceAndExternalId(
                            listing.getMarketplaceSource(),
                            listing.getExternalId()
                    );
            if (exists) {
                log.debug("dedup.duplicate.externalId source={} externalId={}",
                        listing.getMarketplaceSource(), listing.getExternalId());
            }
            return exists;
        }

        // fall back to url check if no external id
        if (listing.getListingUrl() != null && !listing.getListingUrl().isBlank()) {
            boolean exists = marketplaceListingRepository
                    .existsByMarketplaceSourceAndListingUrl(
                            listing.getMarketplaceSource(),
                            listing.getListingUrl()
                    );
            if (exists) {
                log.debug("dedup.duplicate.url source={} url={}",
                        listing.getMarketplaceSource(), listing.getListingUrl());
            }
            return exists;
        }

        return false;
    }
}
