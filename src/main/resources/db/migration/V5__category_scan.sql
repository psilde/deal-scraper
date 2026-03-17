-- V5: Category-first scanning metadata on marketplace_listings.
--
-- source_category / source_city: which category and city produced the listing.

ALTER TABLE marketplace_listings
    ADD COLUMN IF NOT EXISTS source_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_city     VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_mpl_source_category ON marketplace_listings(source_category);
CREATE INDEX IF NOT EXISTS idx_mpl_source_city     ON marketplace_listings(source_city);
