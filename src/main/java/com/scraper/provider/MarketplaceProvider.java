package com.scraper.provider;

import com.scraper.dto.MarketplaceSearchQuery;
import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceSource;

import java.util.List;

// implementations must return raw data only — no normalisation, deduplication, or persistence
public interface MarketplaceProvider {

    boolean supports(MarketplaceSource source);

    List<RawMarketplaceListing> search(MarketplaceSearchQuery query);
}
