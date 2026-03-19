package com.scraper.service;

import com.scraper.dto.MarketplaceSearchQuery;
import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceListing;
import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScanType;
import com.scraper.model.ScrapeJob;
import com.scraper.provider.MarketplaceProvider;
import com.scraper.repository.MarketplaceListingRepository;
import com.scraper.repository.ScrapeJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MarketplaceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceIngestionService.class);

    private final List<MarketplaceProvider> providers;
    private final ListingNormalisationService normalisationService;
    private final ListingDeduplicationService deduplicationService;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapeMetricsCollector metricsCollector;

    public MarketplaceIngestionService(
            List<MarketplaceProvider> providers,
            ListingNormalisationService normalisationService,
            ListingDeduplicationService deduplicationService,
            MarketplaceListingRepository marketplaceListingRepository,
            ScrapeJobRepository scrapeJobRepository,
            ScrapeMetricsCollector metricsCollector
    ) {
        this.providers = providers;
        this.normalisationService = normalisationService;
        this.deduplicationService = deduplicationService;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.scrapeJobRepository = scrapeJobRepository;
        this.metricsCollector = metricsCollector;
    }

    @Transactional
    public ScrapeJob ingest(MarketplaceSource source, String keyword, int maxResults,
                            String baseUrl, ScanType scanType, String sourceCategory, String sourceCity) {
        ScanType type = scanType != null ? scanType : ScanType.KEYWORD;

        // use category @ city as job label for category scans
        String jobLabel = (sourceCategory != null && sourceCity != null)
                ? sourceCategory + " @ " + sourceCity
                : keyword;

        ScrapeJob job = new ScrapeJob(source, jobLabel);
        scrapeJobRepository.save(job);
        job.markRunning();

        log.info("ingestion.started jobId={} source={} scanType={} label=\"{}\"",
                job.getId(), source, type, jobLabel);

        metricsCollector.reset();

        try {
            MarketplaceProvider provider = resolveProvider(source);
            if (provider == null) {
                String msg = "No provider registered for source=" + source;
                log.error("ingestion.failed jobId={} reason={}", job.getId(), msg);
                job.fail(msg);
                return job;
            }

            Set<String> knownIds = marketplaceListingRepository.findExternalIdsByMarketplaceSource(source);
            log.info("ingestion.knownIds source={} count={}", source, knownIds.size());

            List<RawMarketplaceListing> rawListings = provider.search(
                    new MarketplaceSearchQuery(source, keyword, maxResults, knownIds, baseUrl,
                            type, sourceCategory, sourceCity));

            log.info("ingestion.fetched jobId={} label=\"{}\" count={}", job.getId(), jobLabel, rawListings.size());

            List<MarketplaceListing> inserted = new ArrayList<>();
            int duplicates = 0;
            int rejectedNormalisation = 0;

            for (RawMarketplaceListing raw : rawListings) {
                metricsCollector.recordCurrencyFormat(detectCurrencyFormat(raw));

                MarketplaceListing normalised = normalisationService.normalise(raw, job.getId());
                if (normalised == null) {
                    rejectedNormalisation++;
                    continue;
                }
                if (deduplicationService.isDuplicate(normalised)) {
                    duplicates++;
                    continue;
                }
                inserted.add(marketplaceListingRepository.save(normalised));
            }

            job.complete(rawListings.size(), inserted.size(), duplicates);

            log.info("ingestion.complete jobId={} label=\"{}\" fetched={} inserted={} duplicates={} rejected={}",
                    job.getId(), jobLabel, rawListings.size(), inserted.size(), duplicates, rejectedNormalisation);

            logMetricsSummary(job, metricsCollector);

        } catch (Exception e) {
            log.error("ingestion.error jobId={} label=\"{}\" error={}", job.getId(), jobLabel, e.getMessage(), e);
            job.fail(e.getMessage());
        }

        return job;
    }

    private void logMetricsSummary(ScrapeJob job, ScrapeMetricsCollector collector) {
        long durationMs = job.getStartedAt() != null && job.getCompletedAt() != null
                ? ChronoUnit.MILLIS.between(job.getStartedAt(), job.getCompletedAt())
                : 0;

        int attempted = job.getListingsFetched();
        int saved = job.getListingsInserted();
        int skipped = job.getDuplicatesSkipped();
        double captureRate = attempted > 0 ? (saved * 100.0 / attempted) : 0.0;
        double throughput = durationMs > 0 ? (saved * 1000.0 / durationMs) : 0.0;
        int scrollIter = collector.getScrollIterations();
        Map<String, Integer> currencies = collector.getCurrencyFormatCounts();

        log.info("\n--- metrics ---\n" +
                "duration:          {}ms\n" +
                "attempted:         {}\n" +
                "saved:             {}\n" +
                "skipped:           {}\n" +
                "capture rate:      {}\n" +
                "throughput:        {} listings/sec\n" +
                "scroll iterations: {}\n" +
                "Currency breakdown: AUD={} USD={} GBP={} EUR={} FREE={} OTHER={}\n" +
                "--- metrics ---",
                durationMs,
                attempted,
                saved,
                skipped,
                String.format("%.1f%%", captureRate),
                String.format("%.2f", throughput),
                scrollIter,
                currencies.getOrDefault("AUD", 0),
                currencies.getOrDefault("USD", 0),
                currencies.getOrDefault("GBP", 0),
                currencies.getOrDefault("EUR", 0),
                currencies.getOrDefault("FREE", 0),
                currencies.getOrDefault("OTHER", 0)
        );
    }

    // maps raw currency/price to one of the 6 tracked format labels
    private String detectCurrencyFormat(RawMarketplaceListing raw) {
        if (raw.price() != null && raw.price().compareTo(BigDecimal.ZERO) == 0) return "FREE";
        if (raw.currency() == null || raw.currency().isBlank()) return "AUD";
        return switch (raw.currency().trim().toUpperCase()) {
            case "AUD", "AU$", "A$" -> "AUD";
            case "USD", "US$" -> "USD";
            case "GBP", "£" -> "GBP";
            case "EUR", "€" -> "EUR";
            case "FREE" -> "FREE";
            default -> "OTHER";
        };
    }

    private MarketplaceProvider resolveProvider(MarketplaceSource source) {
        return providers.stream()
                .filter(p -> p.supports(source))
                .findFirst()
                .orElse(null);
    }
}
