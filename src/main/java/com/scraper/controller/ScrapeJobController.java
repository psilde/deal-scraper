package com.scraper.controller;

import com.scraper.dto.ScrapeJobResponse;
import com.scraper.model.ScrapeJob;
import com.scraper.model.MarketplaceSource;
import com.scraper.repository.ScrapeJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scrape-jobs")
public class ScrapeJobController {

    private final ScrapeJobRepository scrapeJobRepository;

    public ScrapeJobController(ScrapeJobRepository scrapeJobRepository) {
        this.scrapeJobRepository = scrapeJobRepository;
    }

    @GetMapping
    public Page<ScrapeJobResponse> getJobs(
            @RequestParam(required = false) MarketplaceSource source,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());

        Page<ScrapeJob> jobs = (source != null)
                ? scrapeJobRepository.findByMarketplaceSourceOrderByStartedAtDesc(source, pageable)
                : scrapeJobRepository.findAllByOrderByStartedAtDesc(pageable);

        return jobs.map(this::toResponse);
    }

    @GetMapping("/{id}")
    public ScrapeJobResponse getById(@PathVariable Long id) {
        ScrapeJob job = scrapeJobRepository.findById(id)
                .orElseThrow(() -> new com.scraper.exception.NotFoundException("Scrape job not found: " + id));
        return toResponse(job);
    }

    private ScrapeJobResponse toResponse(ScrapeJob j) {
        return new ScrapeJobResponse(
                j.getId(),
                j.getMarketplaceSource(),
                j.getSearchQuery(),
                j.getStatus(),
                j.getStartedAt(),
                j.getCompletedAt(),
                j.getListingsFetched(),
                j.getListingsInserted(),
                j.getDuplicatesSkipped(),
                j.getErrorMessage()
        );
    }
}
