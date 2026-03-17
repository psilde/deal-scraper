package com.scraper.repository;

import com.scraper.model.MarketplaceListing;
import com.scraper.model.MarketplaceSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {

    // used to skip already-seen listings during scraping
    @Query("select ml.externalId from MarketplaceListing ml where ml.marketplaceSource = :source")
    Set<String> findExternalIdsByMarketplaceSource(MarketplaceSource source);

    boolean existsByMarketplaceSourceAndExternalId(MarketplaceSource source, String externalId);

    boolean existsByMarketplaceSourceAndListingUrl(MarketplaceSource source, String listingUrl);

    Optional<MarketplaceListing> findByMarketplaceSourceAndExternalId(
            MarketplaceSource source, String externalId);

    Page<MarketplaceListing> findBySearchQueryIgnoreCase(String searchQuery, Pageable pageable);

    Page<MarketplaceListing> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<MarketplaceListing> findBySourceCategoryIgnoreCase(String sourceCategory, Pageable pageable);

    Page<MarketplaceListing> findBySourceCityIgnoreCase(String sourceCity, Pageable pageable);

}
