package com.scraper.controller;

import com.scraper.config.MarketplaceProperties;
import com.scraper.dto.MarketplaceListingResponse;
import com.scraper.dto.ScrapeJobResponse;
import com.scraper.dto.TriggerScrapeRequest;
import com.scraper.model.MarketplaceListing;
import com.scraper.model.ScrapeJob;
import com.scraper.repository.MarketplaceListingRepository;
import com.scraper.service.MarketplaceIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

    private final MarketplaceProperties properties;
    private final MarketplaceIngestionService ingestionService;
    private final MarketplaceListingRepository marketplaceListingRepository;

    public MarketplaceController(
            MarketplaceProperties properties,
            MarketplaceIngestionService ingestionService,
            MarketplaceListingRepository marketplaceListingRepository
    ) {
        this.properties = properties;
        this.ingestionService = ingestionService;
        this.marketplaceListingRepository = marketplaceListingRepository;
    }

    @PostMapping("/scrape")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScrapeJobResponse triggerScrape(@Valid @RequestBody TriggerScrapeRequest req) {
        ScrapeJob job = ingestionService.ingest(
                req.marketplaceSource(), req.keyword(),
                properties.getMaxResultsPerSearch(),
                req.baseUrl(), req.scanType(), req.sourceCategory(), req.sourceCity());
        return toJobResponse(job);
    }

    @GetMapping("/listings")
    public Page<MarketplaceListingResponse> getListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sourceCategory,
            @RequestParam(required = false) String sourceCity,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by("scrapedAt").descending());

        Page<MarketplaceListing> results;
        if (keyword != null && !keyword.isBlank()) {
            results = marketplaceListingRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else if (sourceCategory != null && !sourceCategory.isBlank()) {
            results = marketplaceListingRepository.findBySourceCategoryIgnoreCase(sourceCategory, pageable);
        } else if (sourceCity != null && !sourceCity.isBlank()) {
            results = marketplaceListingRepository.findBySourceCityIgnoreCase(sourceCity, pageable);
        } else {
            results = marketplaceListingRepository.findAll(pageable);
        }

        return results.map(this::toResponse);
    }

    private MarketplaceListingResponse toResponse(MarketplaceListing ml) {
        return new MarketplaceListingResponse(
                ml.getId(),
                ml.getMarketplaceSource(),
                ml.getExternalId(),
                ml.getTitle(),
                ml.getDescription(),
                ml.getPrice(),
                ml.getCurrency(),
                ml.getLocation(),
                ml.getSellerName(),
                ml.getListingUrl(),
                ml.getImageUrl(),
                ml.getSearchQuery(),
                ml.getScrapedAt(),
                ml.getListedAt(),
                ml.getCondition(),
                ml.getCategory(),
                ml.getSourceCategory(),
                ml.getSourceCity()
        );
    }

    private ScrapeJobResponse toJobResponse(ScrapeJob job) {
        return new ScrapeJobResponse(
                job.getId(),
                job.getMarketplaceSource(),
                job.getSearchQuery(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getListingsFetched(),
                job.getListingsInserted(),
                job.getDuplicatesSkipped(),
                job.getErrorMessage()
        );
    }
}
