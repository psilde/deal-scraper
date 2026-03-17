package com.scraper.dto;

import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record TriggerScrapeRequest(
        @NotNull MarketplaceSource marketplaceSource,
        @NotBlank @Length(min = 1, max = 200) String keyword,
        String baseUrl,
        ScanType scanType,
        String sourceCategory,
        String sourceCity
) {
}
