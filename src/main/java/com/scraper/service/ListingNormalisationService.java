package com.scraper.service;

import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class ListingNormalisationService {

    private static final Logger log = LoggerFactory.getLogger(ListingNormalisationService.class);

    public MarketplaceListing normalise(RawMarketplaceListing raw, Long scrapeJobId) {
        if (raw.title() == null || raw.title().isBlank()) {
            log.debug("normalisation.rejected reason=blank_title source={} externalId={}",
                    raw.source(), raw.externalId());
            return null;
        }

        if (raw.price() == null || raw.price().compareTo(BigDecimal.ZERO) < 0) {
            log.debug("normalisation.rejected reason=invalid_price source={} externalId={} price={}",
                    raw.source(), raw.externalId(), raw.price());
            return null;
        }

        String title = normaliseText(raw.title());
        if (title == null) {
            log.debug("normalisation.rejected reason=blank_title_after_normalisation source={} externalId={}",
                    raw.source(), raw.externalId());
            return null;
        }

        String currency = raw.currency() != null && !raw.currency().isBlank()
                ? raw.currency().trim().toUpperCase()
                : "AUD";

        return new MarketplaceListing(
                raw.source(),
                trimOrNull(raw.externalId()),
                title,
                trimOrNull(raw.description()),
                raw.price(),
                currency,
                trimOrNull(raw.location()),
                trimOrNull(raw.sellerName()),
                trimOrNull(raw.listingUrl()),
                trimOrNull(raw.imageUrl()),
                raw.searchQuery() != null ? raw.searchQuery().trim().toLowerCase() : "unknown",
                OffsetDateTime.now(),
                raw.listedAt(),
                trimOrNull(raw.condition()),
                trimOrNull(raw.category()),
                raw.rawPayload(),
                scrapeJobId,
                trimOrNull(raw.sourceCategory()),
                trimOrNull(raw.sourceCity())
        );
    }

    private String normaliseText(String value) {
        if (value == null) return null;
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isBlank() ? null : trimmed;
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
