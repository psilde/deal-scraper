package com.scraper.repository;

import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScrapeJob;
import com.scraper.model.ScrapeJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, Long> {

    Page<ScrapeJob> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<ScrapeJob> findByMarketplaceSourceOrderByStartedAtDesc(
            MarketplaceSource source, Pageable pageable);

    List<ScrapeJob> findByStatus(ScrapeJobStatus status);
}
