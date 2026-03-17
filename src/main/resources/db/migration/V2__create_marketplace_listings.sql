-- V2: Marketplace listings — full raw-payload store for scraped items.
--
-- Each row is one listing from one marketplace source.
-- The (marketplace_source, external_id) pair is the primary deduplication key.
-- listing_url provides a secondary deduplication signal when external_id is unavailable.

CREATE TABLE IF NOT EXISTS marketplace_listings (
    id                   BIGSERIAL PRIMARY KEY,
    marketplace_source   VARCHAR(50)     NOT NULL,
    external_id          VARCHAR(200),
    title                VARCHAR(500)    NOT NULL,
    description          TEXT,
    price                NUMERIC(10,2)   NOT NULL,
    currency             VARCHAR(3)      NOT NULL DEFAULT 'GBP',
    location             VARCHAR(200),
    seller_name          VARCHAR(200),
    listing_url          TEXT,
    image_url            TEXT,
    search_query         VARCHAR(200)    NOT NULL,
    scraped_at           TIMESTAMPTZ     NOT NULL,
    listed_at            TIMESTAMPTZ,
    condition            VARCHAR(100),
    category             VARCHAR(100),
    raw_payload          TEXT,
    scrape_job_id        BIGINT,

    CONSTRAINT non_negative_marketplace_prices
        CHECK (price >= 0),

    CONSTRAINT uq_mpl_source_external_id
        UNIQUE (marketplace_source, external_id)
);

CREATE INDEX IF NOT EXISTS idx_mpl_search_query  ON marketplace_listings(search_query);
CREATE INDEX IF NOT EXISTS idx_mpl_scraped_at    ON marketplace_listings(scraped_at);
CREATE INDEX IF NOT EXISTS idx_mpl_price         ON marketplace_listings(price);
CREATE INDEX IF NOT EXISTS idx_mpl_source        ON marketplace_listings(marketplace_source);
