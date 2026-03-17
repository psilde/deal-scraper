package com.scraper.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "scrape_jobs",
        indexes = {
                @Index(name = "idx_scrape_jobs_source",     columnList = "marketplace_source"),
                @Index(name = "idx_scrape_jobs_status",     columnList = "status"),
                @Index(name = "idx_scrape_jobs_started_at", columnList = "started_at")
        }
)
public class ScrapeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_source", nullable = false, length = 50)
    private MarketplaceSource marketplaceSource;

    @Column(name = "search_query", nullable = false, length = 200)
    private String searchQuery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScrapeJobStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "listings_fetched")
    private int listingsFetched;

    @Column(name = "listings_inserted")
    private int listingsInserted;

    @Column(name = "duplicates_skipped")
    private int duplicatesSkipped;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected ScrapeJob() {
    }

    public ScrapeJob(MarketplaceSource marketplaceSource, String searchQuery) {
        this.marketplaceSource = marketplaceSource;
        this.searchQuery = searchQuery;
        this.status = ScrapeJobStatus.PENDING;
        this.startedAt = OffsetDateTime.now();
    }

    public void markRunning() {
        this.status = ScrapeJobStatus.RUNNING;
    }

    public void complete(int listingsFetched, int listingsInserted, int duplicatesSkipped) {
        this.status = ScrapeJobStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
        this.listingsFetched = listingsFetched;
        this.listingsInserted = listingsInserted;
        this.duplicatesSkipped = duplicatesSkipped;
    }

    public void fail(String errorMessage) {
        this.status = ScrapeJobStatus.FAILED;
        this.completedAt = OffsetDateTime.now();
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public MarketplaceSource getMarketplaceSource() { return marketplaceSource; }
    public String getSearchQuery() { return searchQuery; }
    public ScrapeJobStatus getStatus() { return status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public int getListingsFetched() { return listingsFetched; }
    public int getListingsInserted() { return listingsInserted; }
    public int getDuplicatesSkipped() { return duplicatesSkipped; }
    public String getErrorMessage() { return errorMessage; }
}
