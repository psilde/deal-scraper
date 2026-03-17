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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MarketplaceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceIngestionService.class);

    private final List<MarketplaceProvider> providers;
    private final ListingNormalisationService normalisationService;
    private final ListingDeduplicationService deduplicationService;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final ScrapeJobRepository scrapeJobRepository;

    public MarketplaceIngestionService(
            List<MarketplaceProvider> providers,
            ListingNormalisationService normalisationService,
            ListingDeduplicationService deduplicationService,
            MarketplaceListingRepository marketplaceListingRepository,
            ScrapeJobRepository scrapeJobRepository
    ) {
        this.providers = providers;
        this.normalisationService = normalisationService;
        this.deduplicationService = deduplicationService;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.scrapeJobRepository = scrapeJobRepository;
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

        } catch (Exception e) {
            log.error("ingestion.error jobId={} label=\"{}\" error={}", job.getId(), jobLabel, e.getMessage(), e);
            job.fail(e.getMessage());
        }

        return job;
    }

    private MarketplaceProvider resolveProvider(MarketplaceSource source) {
        return providers.stream()
                .filter(p -> p.supports(source))
                .findFirst()
                .orElse(null);
    }
}
