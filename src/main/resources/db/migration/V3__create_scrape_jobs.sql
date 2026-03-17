-- V3: Scrape job audit log — one row per (source, search_query) execution.
--
-- Tracks ingestion counts only. Deal matching and alerting are FindADeal's concern.

CREATE TABLE IF NOT EXISTS scrape_jobs (
    id                   BIGSERIAL PRIMARY KEY,
    marketplace_source   VARCHAR(50)     NOT NULL,
    search_query         VARCHAR(200)    NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    started_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMPTZ,
    listings_fetched     INTEGER         NOT NULL DEFAULT 0,
    listings_inserted    INTEGER         NOT NULL DEFAULT 0,
    duplicates_skipped   INTEGER         NOT NULL DEFAULT 0,
    error_message        TEXT,

    CONSTRAINT chk_scrape_jobs_status
        CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_scrape_jobs_source     ON scrape_jobs(marketplace_source);
CREATE INDEX IF NOT EXISTS idx_scrape_jobs_status     ON scrape_jobs(status);
CREATE INDEX IF NOT EXISTS idx_scrape_jobs_started_at ON scrape_jobs(started_at);

ALTER TABLE marketplace_listings
    ADD CONSTRAINT fk_mpl_scrape_job
    FOREIGN KEY (scrape_job_id) REFERENCES scrape_jobs(id) ON DELETE SET NULL;
