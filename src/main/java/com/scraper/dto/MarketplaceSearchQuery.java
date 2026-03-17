package com.scraper.dto;

import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScanType;

import java.util.Set;

public record MarketplaceSearchQuery(
        MarketplaceSource source,
        String keyword,
        int maxResults,
        Set<String> knownExternalIds,
        String baseUrl,
        ScanType scanType,
        String sourceCategory,
        String sourceCity
) {
}
