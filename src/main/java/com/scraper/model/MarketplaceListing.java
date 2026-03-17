package com.scraper.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "marketplace_listings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_mpl_source_external_id",
                        columnNames = {"marketplace_source", "external_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mpl_search_query",      columnList = "search_query"),
                @Index(name = "idx_mpl_scraped_at",        columnList = "scraped_at"),
                @Index(name = "idx_mpl_price",             columnList = "price"),
                @Index(name = "idx_mpl_source",            columnList = "marketplace_source"),
                @Index(name = "idx_mpl_source_category",   columnList = "source_category"),
                @Index(name = "idx_mpl_source_city",       columnList = "source_city")
        }
)
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_source", nullable = false, length = 50)
    private MarketplaceSource marketplaceSource;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // ISO 4217 currency code
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 200)
    private String location;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "listing_url", columnDefinition = "TEXT")
    private String listingUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "search_query", nullable = false, length = 200)
    private String searchQuery;

    @Column(name = "scraped_at", nullable = false)
    private OffsetDateTime scrapedAt;

    @Column(name = "listed_at")
    private OffsetDateTime listedAt;

    @Column(length = 100)
    private String condition;

    @Column(length = 100)
    private String category;

    // raw source payload, kept for reprocessing
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "scrape_job_id")
    private Long scrapeJobId;

    @Column(name = "source_category", length = 100)
    private String sourceCategory;

    @Column(name = "source_city", length = 100)
    private String sourceCity;

    protected MarketplaceListing() {
    }

    public MarketplaceListing(
            MarketplaceSource marketplaceSource,
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
            OffsetDateTime scrapedAt,
            OffsetDateTime listedAt,
            String condition,
            String category,
            String rawPayload,
            Long scrapeJobId,
            String sourceCategory,
            String sourceCity
    ) {
        this.marketplaceSource = marketplaceSource;
        this.externalId = externalId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.currency = currency != null ? currency : "AUD";
        this.location = location;
        this.sellerName = sellerName;
        this.listingUrl = listingUrl;
        this.imageUrl = imageUrl;
        this.searchQuery = searchQuery;
        this.scrapedAt = scrapedAt;
        this.listedAt = listedAt;
        this.condition = condition;
        this.category = category;
        this.rawPayload = rawPayload;
        this.scrapeJobId = scrapeJobId;
        this.sourceCategory = sourceCategory;
        this.sourceCity = sourceCity;
    }

    public Long getId() { return id; }
    public MarketplaceSource getMarketplaceSource() { return marketplaceSource; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getLocation() { return location; }
    public String getSellerName() { return sellerName; }
    public String getListingUrl() { return listingUrl; }
    public String getImageUrl() { return imageUrl; }
    public String getSearchQuery() { return searchQuery; }
    public OffsetDateTime getScrapedAt() { return scrapedAt; }
    public OffsetDateTime getListedAt() { return listedAt; }
    public String getCondition() { return condition; }
    public String getCategory() { return category; }
    public String getRawPayload() { return rawPayload; }
    public Long getScrapeJobId() { return scrapeJobId; }
    public String getSourceCategory() { return sourceCategory; }
    public String getSourceCity() { return sourceCity; }

    public void setScrapeJobId(Long scrapeJobId) { this.scrapeJobId = scrapeJobId; }
}
